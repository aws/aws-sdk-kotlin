/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject.phases

import aws.sdk.kotlin.hll.s3transfermanager.interceptors.MutableTransferContext
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferInterceptor
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferPhase
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.executePhase
import aws.sdk.kotlin.hll.s3transfermanager.model.MultipartDownloadType
import aws.sdk.kotlin.hll.s3transfermanager.model.utils.toDownloadObjectResponse
import aws.sdk.kotlin.hll.s3transfermanager.utils.S3TransferManagerException
import aws.sdk.kotlin.hll.s3transfermanager.utils.ceilDiv
import aws.sdk.kotlin.hll.s3transfermanager.utils.withTmBusinessMetric
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectResponse
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import aws.smithy.kotlin.runtime.util.Filesystem
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.WriteType
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

/**
 * Orchestrates downloading an S3 object, potentially in multiple parts, to a local file **and/or to a handler function**.
 */
internal class TransferBytes<T>(
    private val multipartDownloadType: MultipartDownloadType,
    private val context: MutableTransferContext,
    private val s3Client: S3Client,
    private val downloadPath: String?,
    private val interceptors: List<TransferInterceptor>,
    private val objectHandler: (suspend (ByteArray) -> T)?,
    private val networkOperation: Semaphore,
    private val diskOperation: Semaphore,
    private val maxInMemoryParts: Int,
    private val targetPartSizeBytes: Long,
    private val bufferCount: Semaphore,
) {
    /**
     * Used to interact with the underlying systems files.
     */
    private val system: Filesystem = PlatformProvider.System

    /**
     * Concurrent coroutines will be updating a shared context using this mutex.
     */
    private val contextMutex = Mutex()

    /**
     * If a download path is provided, the object will be downloaded to a temporary file and then renamed to the
     * provided path at the end.
     */
    private val tempDownloadPath = "$downloadPath.s3tmp.${Random.nextInt(0, 10_000_000)}"

    /**
     * Will be used to verify we're receiving the same version of the object for each part.
     */
    private lateinit var etag: String

    /**
     * The part count is provided by S3 if the download type is PART, but it needs to be calculated if the download type is RANGE.
     *
     * Defaults to null since we don't know how many parts are needed until after we receive the first part and a number
     * is provided, or we can calculate one using the object's length.
     */
    private var partCount: Int? = null

    /**
     * The total length of the object to download
     *
     * Defaults to null since we don't know it until after we receive the first part, and we can parse the object's length.
     */
    private var contentLength: Long? = null

    /**
     * Whether the download will require more than 1 part
     */
    private var needMoreParts: Boolean = true

    suspend fun transfer() {
        transferFirstPart()
        if (needMoreParts) {
            transferRemainingParts()
        }
        buildResponse()
    }

    private suspend fun transferFirstPart() {
        executePhase(TransferPhase.BytesTransferred, context, interceptors) {
            try {
                networkOperation.acquire()
                s3Client.withTmBusinessMetric { client ->
                    client.getObject(context.s3Request as GetObjectRequest) { getObjectResponse ->
                        context.s3Response = getObjectResponse
                        context.transferredBytes = getObjectResponse.contentLength
                        context.transferableBytes = parseTransferableBytes(getObjectResponse)

                        contentLength = context.transferableBytes
                        etag = getObjectResponse.eTag ?: throw S3TransferManagerException("etag is null in initial get object response")
                        partCount = getObjectResponse.partsCount

                        val bytes = getObjectResponse.body?.toByteArray() ?: byteArrayOf()
                        objectHandler?.invoke(bytes)
                        try {
                            diskOperation.acquire()
                            downloadPath?.let {
                                system.write(
                                    tempDownloadPath,
                                    bytes,
                                    WriteType.OVERWRITE,
                                )
                            }
                        } finally {
                            diskOperation.release()
                        }

                        if (partCount == 1 || context.transferredBytes == context.transferableBytes) {
                            needMoreParts = false
                            renameTempFile()
                        }
                    }
                }
            } catch (t: Throwable) {
                system.delete(tempDownloadPath, mustExist = false)
                throw t
            } finally {
                networkOperation.release()
            }
        }
    }

    private suspend fun transferRemainingParts() {
        val buffer = Channel<Part>(capacity = maxInMemoryParts)
        partCount = partCount ?: ceilDiv(context.transferableBytes!!, targetPartSizeBytes).toInt()

        try {
            coroutineScope {
                launch { downloadParts(buffer) }
                launch { processParts(buffer) }
            }
            renameTempFile()
        } finally {
            system.delete(tempDownloadPath, mustExist = false) // Deletes downloaded bytes if something went wrong, or no op if success
            buffer.consumeEach { bufferCount.release() } // Clears buffer and resets count if something went wrong, or no op if success
        }
    }

    private suspend fun downloadParts(
        buffer: Channel<Part>,
    ) = coroutineScope {
        val jobs = mutableSetOf<Job>()

        repeat(partCount!! - 1) { i ->
            // -1 since first part is already downloaded
            jobs += launch {
                // Other jobs will be updating the global context so make a copy that won't change and we can modify safely
                val localContext = context.copy()

                val previousRequest = localContext.s3Request as GetObjectRequest
                localContext.s3Request = buildNextRequest(previousRequest, i)

                try {
                    networkOperation.acquire()
                    var downloadedBytes = 0L

                    executePhase(TransferPhase.BytesTransferred, localContext, interceptors) {
                        s3Client.withTmBusinessMetric {
                            it.getObject(localContext.s3Request as GetObjectRequest) { getObjectResponse ->
                                downloadedBytes = getObjectResponse.contentLength!!

                                localContext.s3Response = getObjectResponse
                                localContext.transferredBytes = localContext.transferredBytes!! + downloadedBytes

                                val bytes = getObjectResponse.body?.toByteArray() ?: byteArrayOf()
                                bufferCount.acquire()
                                buffer.send(
                                    Part(bytes, i),
                                )
                            }
                        }
                    }

                    // Merge the local context back into the global context
                    updateContext(localContext, downloadedBytes)
                } finally {
                    networkOperation.release()
                }
            }
        }

        jobs.joinAll()
        buffer.close()
    }

    private suspend fun processParts(buffer: Channel<Part>) = coroutineScope {
        val processors = mutableSetOf<Job>()

        for (part in buffer) {
            processors += launch {
                objectHandler?.invoke(part.bytes)
                try {
                    diskOperation.acquire()
                    downloadPath?.let {
                        system.write(
                            tempDownloadPath,
                            part.bytes,
                            WriteType.OFFSET(targetPartSizeBytes * (part.part + 1)), // +1 since first part is already written
                        )
                    }
                    bufferCount.release()
                } finally {
                    diskOperation.release()
                }
            }
        }
        processors.joinAll()
    }

    private suspend fun updateContext(localContext: MutableTransferContext, downloadedBytes: Long) {
        // Note: coroutines will update the context as they complete, not sequentially based on part number
        contextMutex.withLock {
            // It doesn't matter what part the latest req/resp corresponds to
            context.s3Request = localContext.s3Request
            context.s3Response = localContext.s3Response

            // This value should remain the same throughout the transfer unless a user changes it using an interceptor
            context.transferableBytes = localContext.transferableBytes

            // Add rather than overwrite since updates are unordered
            // Note: Sending bytes to the buffer is considered "transferred"
            context.transferredBytes = context.transferredBytes!! + downloadedBytes
        }
    }

    private fun renameTempFile() {
        if (system.fileExists(tempDownloadPath) && downloadPath != null) {
            system.atomicMove(tempDownloadPath, downloadPath, overwrite = true)
        }
    }

    private fun buildNextRequest(
        previousRequest: GetObjectRequest,
        i: Int,
    ): GetObjectRequest = when (multipartDownloadType) {
        MultipartDownloadType.Part ->
            previousRequest.copy {
                partNumber = i + 2 // i is zero based while parts are 1 based, and first part was already downloaded
                ifMatch = etag
            }
        MultipartDownloadType.Range ->
            previousRequest.copy {
                range = "bytes=${targetPartSizeBytes * (i + 1)}-${(targetPartSizeBytes * (i + 2)) - 1}"
                ifMatch = etag
            }
    }

    private fun buildResponse() {
        checkNotNull(contentLength)
        context.tmResponse = (context.s3Response as GetObjectResponse).toDownloadObjectResponse(
            _contentLength = contentLength,
            _contentRange = "bytes=0-${contentLength!! - 1}/$contentLength",
        )
    }
}

private data class Part(
    val bytes: ByteArray,
    val part: Int,
)

private fun parseTransferableBytes(response: GetObjectResponse): Long = response.contentRange
    ?.split("/") // e.g. ContentRange=bytes0-50/100 where 100 is the total length of an object
    ?.last()
    ?.toLong()
    ?: throw S3TransferManagerException("Content range not found in GetObjectResponse")
