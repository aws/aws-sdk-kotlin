/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.uploadobject.phases

import aws.sdk.kotlin.hll.s3transfermanager.interceptors.MutableTransferContext
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferInterceptor
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferPhase
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.executePhase
import aws.sdk.kotlin.hll.s3transfermanager.model.Part
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadObjectRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.utils.toUploadPartRequest
import aws.sdk.kotlin.hll.s3transfermanager.operations.uploadobject.ceilDiv
import aws.sdk.kotlin.hll.s3transfermanager.operations.uploadobject.nextPartBytes
import aws.sdk.kotlin.hll.s3transfermanager.operations.uploadobject.resolvePartSize
import aws.sdk.kotlin.hll.s3transfermanager.operations.uploadobject.resolveSource
import aws.sdk.kotlin.hll.s3transfermanager.utils.S3TransferManagerException
import aws.sdk.kotlin.hll.s3transfermanager.utils.withTmBusinessMetric
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.abortMultipartUpload
import aws.sdk.kotlin.services.s3.model.CompletedPart
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.UploadPartRequest
import aws.sdk.kotlin.services.s3.model.UploadPartResponse
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock

internal suspend fun transferBytes(
    multipartUpload: Boolean,
    contentLength: Long,
    partSizeBytes: Long,
    logger: Logger,
    uploadObjectRequest: UploadObjectRequest,
    context: MutableTransferContext,
    mpuUploadId: String?,
    interceptors: List<TransferInterceptor>,
    client: S3Client,
    maxInMemoryParts: Int,
    maxConcurrentPartUploads: Int,
    bufferSemaphore: Semaphore,
): List<CompletedPart> = coroutineScope {
    val uploadedParts = mutableListOf<CompletedPart>()

    if (multipartUpload) {
        try {
            val partSize = resolvePartSize(
                contentLength,
                partSizeBytes,
                uploadObjectRequest.key,
                logger,
            )
            val numberOfParts = ceilDiv(contentLength, partSize).toInt()
            val partSource = resolveSource(uploadObjectRequest.body!!)

            val producer = produceParts(
                context.transferableBytes!!,
                partSource,
                partSize,
                numberOfParts,
                maxInMemoryParts,
                bufferSemaphore,
            )

            try {
                val mutex = Mutex()
                repeat(maxConcurrentPartUploads) {
                    consumer(
                        producer,
                        uploadObjectRequest,
                        mpuUploadId!!,
                        context,
                        interceptors,
                        client,
                        uploadedParts,
                        mutex,
                        bufferSemaphore,
                    )
                }
            } catch (e: Exception) {
                // Consume remaining in memory parts and reduce part count
                producer.consumeEach {
                    bufferSemaphore.release()
                }
                throw e
            }

            if (uploadedParts.size != numberOfParts) {
                throw S3TransferManagerException("The number of uploaded parts does not match the expected count. Expected $numberOfParts, actual: ${uploadedParts.size}")
            }
        } catch (uploadPartException: Exception) {
            logger.warn {
                buildString {
                    append("Exception occurred while uploading parts for object: ${uploadObjectRequest.key}. ")
                    append("Aborting multi part upload!")
                }
            }

            try {
                client.withTmBusinessMetric {
                    it.abortMultipartUpload {
                        bucket = uploadObjectRequest.bucket
                        expectedBucketOwner = uploadObjectRequest.expectedBucketOwner
                        key = uploadObjectRequest.key
                        requestPayer = uploadObjectRequest.requestPayer
                        uploadId = mpuUploadId
                    }
                }
                throw S3TransferManagerException("Multipart upload failed (ID: $mpuUploadId). One or more parts could not be uploaded", uploadPartException)
            } catch (abortException: Exception) {
                throw S3TransferManagerException("Multipart upload failed (ID: $mpuUploadId). Unable to abort multipart upload.", abortException)
                    .also { it.addSuppressed(uploadPartException) }
            }
        }
    } else {
        executePhase(
            TransferPhase.BytesTransferred,
            context,
            interceptors,
        ) {
            context.s3Response = client.withTmBusinessMetric {
                it.putObject(context.s3Request as PutObjectRequest)
            }
            context.transferredBytes = context.transferableBytes
        }
    }

    return@coroutineScope uploadedParts
}

/**
 * Produces multipart upload parts to be consumed by [consumer].
 *
 * Uses a [kotlinx.coroutines.channels.Channel].
 * Produces until all readable bytes are read.
 */
private fun CoroutineScope.produceParts(
    readableBytes: Long,
    partSource: Any,
    partSize: Long,
    numberOfParts: Int,
    maxInMemoryParts: Int,
    bufferSemaphore: Semaphore,
) = produce(
    capacity = maxInMemoryParts,
) {
    var readBytes = 0L
    var currentPartNumber = 1

    while (readBytes < readableBytes) {
        // +1 part in memory
        bufferSemaphore.acquire()

        send(
            Part(
                currentPartNumber,
                nextPartBytes(
                    partSource,
                    partSize,
                    currentPartNumber == numberOfParts,
                    readBytes,
                    readableBytes,
                ),
            ).also {
                if (currentPartNumber != numberOfParts && it.bytes.size != partSize) {
                    throw S3TransferManagerException("Part #$currentPartNumber size mismatch detected. Expected $partSize, actual: ${it.bytes.size}")
                }
            },
        )

        currentPartNumber++
        readBytes += partSize
    }
}

/**
 * Launches a coroutine that consumes and uploads multipart upload parts.
 *
 * It receives mutable shared state that may also be used by other coroutines and is
 * intended for use in a [fan-out](https://kotlinlang.org/docs/channels.html#fan-out) pattern,
 * where multiple consumers concurrently upload different parts of the same object.
 */
private fun CoroutineScope.consumer(
    channel: ReceiveChannel<Part>,
    uploadObjectRequest: UploadObjectRequest,
    mpuUploadId: String,
    context: MutableTransferContext,
    interceptors: List<TransferInterceptor>,
    client: S3Client,
    uploadedParts: MutableList<CompletedPart>,
    mutex: Mutex,
    semaphore: Semaphore,
) = launch {
    for (part in channel) {
        val partSize = part.bytes.size // Store the original size, as it will shrink when bytes are read
        val localContext = context.copy() // Create a separate copy to avoid concurrent modifications

        localContext.s3Request = uploadObjectRequest.toUploadPartRequest(
            part.bytes,
            part.number,
            mpuUploadId,
        )

        executePhase(
            TransferPhase.BytesTransferred,
            localContext,
            interceptors,
        ) {
            localContext.s3Response = client.withTmBusinessMetric {
                it.uploadPart(localContext.s3Request as UploadPartRequest)
            }

            // -1 part in memory
            semaphore.release()

            localContext.transferredBytes = localContext.transferredBytes!! + partSize
        }

        // Update shared state between coroutines
        mutex.withLock {
            context.s3Request = localContext.s3Request
            context.s3Response = localContext.s3Response
            context.transferableBytes = localContext.transferableBytes
            context.transferredBytes = context.transferredBytes!! + partSize // Don't use transferredBytes from local context as it might be out of date
            context.transferableObjects = localContext.transferableObjects
            context.transferredObjects = localContext.transferredObjects

            uploadedParts.add(
                CompletedPart {
                    partNumber = part.number
                    eTag = (localContext.s3Response as UploadPartResponse).eTag
                },
            )
        }
    }
}
