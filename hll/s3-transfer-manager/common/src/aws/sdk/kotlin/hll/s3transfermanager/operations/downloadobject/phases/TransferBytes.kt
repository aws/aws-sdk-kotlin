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
import aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject.ceilDiv
import aws.sdk.kotlin.hll.s3transfermanager.utils.S3TransferManagerException
import aws.sdk.kotlin.hll.s3transfermanager.utils.withTmBusinessMetric
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectResponse
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
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

// TODO: Break down into functions
internal suspend fun <T> transferBytes(
    multipartDownloadType: MultipartDownloadType,
    context: MutableTransferContext,
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
    var singleRequest = false
    var etag: String? = null
    var partsCount: Int? = null

    val system = PlatformProvider.System
    val tempFilePath = "$downloadPath.s3tmp.${Random.nextInt(0, 10_000_000)}" // e.g. Users/bob/downloads/object.s3tmp.314

    // TODO: Full download function
    executePhase(
        TransferPhase.BytesTransferred,
        context,
        interceptors,
    ) {
        try {
            networkOperation.acquire()
            s3Client.withTmBusinessMetric {
                it.getObject(context.s3Request as GetObjectRequest) { getObjectResponse ->
                    context.s3Response = getObjectResponse
                    context.transferredBytes = getObjectResponse.contentLength
                    context.transferableBytes = getObjectResponse
                        .contentRange
                        ?.split("/") // e.g. "ContentRange=bytes 0-1/5" where 5 is the content length
                        ?.last()
                        ?.toLong()
                        ?: throw S3TransferManagerException("Content range not found in GetObjectResponse")
                    etag = getObjectResponse.eTag ?: throw S3TransferManagerException("ETag not found in GetObjectResponse")
                    partsCount = getObjectResponse.partsCount

                    if (partsCount == 1 || context.transferredBytes == context.transferableBytes) {
                        singleRequest = true
                        objectHandler?.invoke(getObjectResponse)
                        downloadPath?.let {
                            system.write(
                                tempFilePath,
                                getObjectResponse.body?.toByteArray() ?: byteArrayOf(),
                                WriteType.OVERWRITE,
                                mustExist = true
                            )
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            system.delete(tempFilePath)
        } finally {
            networkOperation.release()
            // Rename temp file
            downloadPath?.let {
                system.atomicMove(
                    tempFilePath,
                    downloadPath,
                )
            }
        }
    }
    if (singleRequest) return

    // TODO: Multiple downloads function
    partsCount = partsCount ?: ceilDiv(context.transferableBytes!!, targetPartSizeBytes).toInt()
    val buffer = Channel<Part>(capacity = maxInMemoryParts)
    val contextMutex = Mutex()
    try {
        coroutineScope {
            // Download parts
            launch {
                val downloaders = mutableSetOf<Job>()
                repeat(partsCount!!) { part ->
                    downloaders += launch {
                        // Take a snapshot of global context
                        val localContext = context.copy()

                        localContext.s3Request = when (multipartDownloadType) {
                            MultipartDownloadType.Part -> (localContext.s3Request as GetObjectRequest).copy {
                                partNumber = partNumber!! + 1
                                ifMatch = etag
                            }
                            MultipartDownloadType.Range -> (localContext.s3Request as GetObjectRequest).copy {
                                // TODO: Turn this into a helper function
                                range = "bytes=${targetPartSizeBytes * part}-${(targetPartSizeBytes * (part + 1)) - 1}"
                                ifMatch = etag
                            }
                        }
                        try { // TODO: Fix this nesting
                            executePhase(
                                TransferPhase.BytesTransferred,
                                context,
                                interceptors,
                            ) {
                                networkOperation.acquire()
                                s3Client.withTmBusinessMetric {
                                    it.getObject(localContext.s3Request as GetObjectRequest) { getObjectResponse ->
                                        localContext.s3Response = getObjectResponse

                                        val partSize = (getObjectResponse.contentLength ?: throw S3TransferManagerException("Content length not found in GetObjectResponse"))
                                        localContext.transferredBytes = localContext.transferredBytes!! + partSize

                                        bufferCount.acquire()
                                        buffer.send(
                                            Part(
                                                getObjectResponse,
                                                part
                                            )
                                        )

                                        // Update shared state between coroutines
                                        // TODO: Update global context helper function
                                        contextMutex.withLock {
                                            context.s3Request = localContext.s3Request
                                            context.s3Response = localContext.s3Response
                                            context.transferableBytes = localContext.transferableBytes
                                            context.transferredBytes = context.transferredBytes!! + partSize // Don't use transferredBytes from local context as it might be out of date
                                            context.transferableObjects = localContext.transferableObjects
                                            context.transferredObjects = localContext.transferredObjects
                                        }
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

            // Process parts
            launch {
                val processors = mutableSetOf<Job>()
                for (part in buffer) {
                    processors += launch {
                        objectHandler?.invoke(part.response)
                        try {
                            diskOperation.acquire()
                            downloadPath?.let {
                                system.write(
                                    tempFilePath,
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
    } catch (_:Throwable) {
        system.delete(tempFilePath, mustExist = false)
    } finally {
        // Clear buffer (and reduce buffer count) in case of exception
        buffer.consumeEach {
            bufferCount.release()
        }
        // Rename temp file
        downloadPath?.let {
            system.atomicMove(tempFilePath, downloadPath, overwrite = true)
        }
    }
}

private data class Part(
    val response: GetObjectResponse,
    val part: Int
)