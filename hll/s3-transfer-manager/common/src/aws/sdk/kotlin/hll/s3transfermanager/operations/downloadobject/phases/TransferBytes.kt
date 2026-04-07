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
import aws.sdk.kotlin.hll.s3transfermanager.utils.ceilDiv
import aws.sdk.kotlin.hll.s3transfermanager.utils.S3TransferManagerException
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

internal suspend fun <T> transferBytes(
    multipartDownloadType: MultipartDownloadType,
    globalContext: MutableTransferContext,
    s3Client: S3Client,
    downloadPath: String?,
    interceptors: List<TransferInterceptor>,
    objectHandler: (suspend (GetObjectResponse) -> T)?,
    logger: Logger,
    networkOperation: Semaphore,
    diskOperation: Semaphore,
    maxInMemoryParts: Int,
    targetPartSizeBytes: Long,
    bufferCount: Semaphore,
) {
    val system = PlatformProvider.System
    val tempDownloadPath = "$downloadPath.s3tmp.${Random.nextInt(0, 10_000_000)}"

    val result = downloadFirstPart(
        globalContext,
        s3Client,
        interceptors,
        networkOperation,
        objectHandler,
        downloadPath,
        tempDownloadPath,
        system,
        logger,
    )
    if (!result.singleRequest) {
        downloadRemainingParts(
            // Use S3 provided parts count if download type is part otherwise calculate part numbers if transfer is range
            result.partsCount ?: ceilDiv(globalContext.transferableBytes!!, targetPartSizeBytes).toInt(),
            multipartDownloadType,
            globalContext,
            s3Client,
            interceptors,
            networkOperation,
            diskOperation,
            bufferCount,
            maxInMemoryParts,
            targetPartSizeBytes,
            result.etag,
            objectHandler,
            downloadPath,
            tempDownloadPath,
            system,
            logger,
        )
    }

    buildTransferManagerResponse(globalContext, result.contentLength)
}

/**
 * Result of the initial (first-part) download, used to decide whether multipart is needed.
 */
private data class InitialDownloadResult(
    val singleRequest: Boolean,
    val etag: String,
    val partsCount: Int?,
    val contentLength: Long,
    val request: GetObjectResponse,
)

/**
 * A downloaded part and its index, buffered between downloader and writer coroutines.
 */
private data class Part(
    val response: GetObjectResponse,
    val part: Int,
)

/**
 * Download the first part to determine total size, etag, and whether a single request suffices.
 */
private suspend fun <T> downloadFirstPart(
    context: MutableTransferContext,
    s3Client: S3Client,
    interceptors: List<TransferInterceptor>,
    networkOperation: Semaphore,
    objectHandler: (suspend (GetObjectResponse) -> T)?,
    downloadPath: String?,
    tempDownloadPath: String,
    system: Filesystem,
    logger: Logger,
): InitialDownloadResult {
    var singleRequest = false
    var partsCount: Int? = null
    lateinit var etag: String

    executePhase(TransferPhase.BytesTransferred, context, interceptors) {
        try {
            networkOperation.acquire()
            s3Client.withTmBusinessMetric { client ->
                client.getObject(context.s3Request as GetObjectRequest) { getObjectResponse ->
                    // TODO: Commonize these sets ?
                    context.s3Response = getObjectResponse
                    context.transferredBytes = getObjectResponse.contentLength
                    context.transferableBytes = parseTransferableBytes(getObjectResponse)

                    etag = getObjectResponse.eTag ?: throw S3TransferManagerException("ETag not found in GetObjectResponse")
                    partsCount = getObjectResponse.partsCount

                    // TODO: Disk operation semaphore
                    objectHandler?.invoke(getObjectResponse)
                    downloadPath?.let {
                        system.write(
                            tempDownloadPath,
                            getObjectResponse.body?.toByteArray() ?: byteArrayOf(), // TODO: Stream body
                            WriteType.OVERWRITE,
                        )
                    }

                    if (partsCount == 1 || context.transferredBytes == context.transferableBytes) {
                        singleRequest = true
                        downloadPath?.let {
                            if (system.fileExists(downloadPath)) {
                                logger.warn { "File $downloadPath already exists and will be overwritten." }
                            }
                            system.atomicMove(tempDownloadPath, downloadPath, overwrite = true)
                        }
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

    return InitialDownloadResult(singleRequest, etag, partsCount, context.transferableBytes!!, context.s3Response as GetObjectResponse)
}

/**
 * Parse the total transferable bytes from the Content-Range header.
 *
 * e.g. ContentRange=bytes 0-1/5  where 5 is the total content length
 */
private fun parseTransferableBytes(response: GetObjectResponse): Long = response.contentRange
    ?.split("/")
    ?.last()
    ?.toLong()
    ?: throw S3TransferManagerException("Content range not found in GetObjectResponse")

/**
 * Set the transfer manager response on the global context from the final [GetObjectResponse].
 */
private fun buildTransferManagerResponse(context: MutableTransferContext, contentLength: Long) {
    context.tmResponse = (context.s3Response as GetObjectResponse)
        .toDownloadObjectResponse(
            contentLength,
            "bytes=0-${contentLength - 1}/$contentLength",
        )
}

/**
 * Build the per-part [GetObjectRequest] for a multipart download.
 */
private fun buildPartRequest(
    baseRequest: GetObjectRequest,
    multipartDownloadType: MultipartDownloadType,
    partIndex: Int,
    targetPartSizeBytes: Long,
    etag: String?,
): GetObjectRequest = when (multipartDownloadType) {
    MultipartDownloadType.Part -> baseRequest.copy {
        partNumber = partNumber!! + 1
        ifMatch = etag
    }
    MultipartDownloadType.Range -> baseRequest.copy {
        range = "bytes=${targetPartSizeBytes * partIndex}-${(targetPartSizeBytes * (partIndex + 1)) - 1}"
        ifMatch = etag
    }
}

/**
 * Download remaining parts concurrently, buffer them, and write to disk.
 */
private suspend fun <T> downloadRemainingParts(
    partsCount: Int,
    multipartDownloadType: MultipartDownloadType,
    context: MutableTransferContext,
    s3Client: S3Client,
    interceptors: List<TransferInterceptor>,
    networkOperation: Semaphore,
    diskOperation: Semaphore,
    bufferCount: Semaphore,
    maxInMemoryParts: Int,
    targetPartSizeBytes: Long,
    etag: String?,
    objectHandler: (suspend (GetObjectResponse) -> T)?,
    downloadPath: String?,
    tempDownloadPath: String,
    system: Filesystem,
    logger: Logger,
) {
    val buffer = Channel<Part>(capacity = maxInMemoryParts)
    val contextMutex = Mutex()

    try {
        coroutineScope {
            launch {
                downloadParts(
                    partsCount,
                    multipartDownloadType,
                    context,
                    s3Client,
                    interceptors,
                    networkOperation,
                    bufferCount,
                    targetPartSizeBytes,
                    etag,
                    contextMutex,
                    buffer,
                )
            }
            launch {
                processParts(
                    buffer,
                    objectHandler,
                    diskOperation,
                    bufferCount,
                    downloadPath,
                    tempDownloadPath,
                    targetPartSizeBytes,
                    system,
                )
            }
        }

        downloadPath?.let {
            if (system.fileExists(downloadPath)) {
                logger.warn { "File $downloadPath already exists and will be overwritten." }
            }
            system.atomicMove(tempDownloadPath, downloadPath, overwrite = true)
        }
    } finally {
        system.delete(tempDownloadPath, mustExist = false)
        buffer.consumeEach { bufferCount.release() }
    }
}

/**
 * Launch concurrent downloaders for each part, sending results to [buffer].
 */
private suspend fun downloadParts(
    partsCount: Int,
    multipartDownloadType: MultipartDownloadType,
    context: MutableTransferContext,
    s3Client: S3Client,
    interceptors: List<TransferInterceptor>,
    networkOperation: Semaphore,
    bufferCount: Semaphore,
    targetPartSizeBytes: Long,
    etag: String?,
    contextMutex: Mutex,
    buffer: Channel<Part>,
) {
    val downloaders = mutableSetOf<Job>()
    coroutineScope {
        repeat(partsCount) { part ->
            downloaders += launch {
                val localContext = context.copy()
                localContext.s3Request = buildPartRequest(
                    localContext.s3Request as GetObjectRequest,
                    multipartDownloadType,
                    part,
                    targetPartSizeBytes,
                    etag,
                )

                try {
                    networkOperation.acquire()
                    executePhase(TransferPhase.BytesTransferred, localContext, interceptors) {
                        s3Client.withTmBusinessMetric {
                            it.getObject(localContext.s3Request as GetObjectRequest) { getObjectResponse ->
                                localContext.s3Response = getObjectResponse
                                val contentLength = getObjectResponse.contentLength
                                    ?: throw S3TransferManagerException("Content length not found in GetObjectResponse")
                                localContext.transferredBytes = localContext.transferredBytes!! + contentLength

                                bufferCount.acquire()
                                buffer.send(Part(getObjectResponse, part))

                                mergeContext(context, localContext, contentLength, contextMutex)
                            }
                        }
                    }
                } finally {
                    networkOperation.release()
                }
            }
        }
        downloaders.joinAll()
        buffer.close()
    }
}

/**
 * Merge a completed part's local context back into the shared [context].
 */
private suspend fun mergeContext(
    context: MutableTransferContext,
    localContext: MutableTransferContext,
    contentLength: Long,
    mutex: Mutex,
) {
    mutex.withLock {
        context.s3Request = localContext.s3Request
        context.s3Response = localContext.s3Response
        context.transferableBytes = localContext.transferableBytes
        context.transferredBytes = context.transferredBytes!! + contentLength
    }
}

/**
 * Consume buffered parts, invoke the handler, and write bytes to disk at the correct offset.
 */
private suspend fun <T> processParts(
    buffer: Channel<Part>,
    objectHandler: (suspend (GetObjectResponse) -> T)?,
    diskOperation: Semaphore,
    bufferCount: Semaphore,
    downloadPath: String?,
    tempDownloadPath: String,
    targetPartSizeBytes: Long,
    system: Filesystem,
) {
    val processors = mutableSetOf<Job>()
    coroutineScope {
        for (part in buffer) {
            processors += launch {
                objectHandler?.invoke(part.response)
                try {
                    diskOperation.acquire()
                    downloadPath?.let {
                        system.write(
                            tempDownloadPath,
                            part.response.body?.toByteArray() ?: byteArrayOf(),
                            WriteType.OFFSET(targetPartSizeBytes * part.part),
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
}
