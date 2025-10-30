/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.uploadfile.hooks

import aws.sdk.kotlin.hll.s3transfermanager.BytesTransferred
import aws.sdk.kotlin.hll.s3transfermanager.TransferContext
import aws.sdk.kotlin.hll.s3transfermanager.TransferInterceptor
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.utils.toUploadPartRequest
import aws.sdk.kotlin.hll.s3transfermanager.operationHook
import aws.sdk.kotlin.hll.s3transfermanager.operations.uploadfile.ceilDiv
import aws.sdk.kotlin.hll.s3transfermanager.operations.uploadfile.nextPartBytes
import aws.sdk.kotlin.hll.s3transfermanager.operations.uploadfile.resolvePartSize
import aws.sdk.kotlin.hll.s3transfermanager.operations.uploadfile.resolvePartSource
import aws.sdk.kotlin.hll.s3transfermanager.utils.S3TransferManagerException
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.abortMultipartUpload
import aws.sdk.kotlin.services.s3.model.CompletedPart
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.UploadPartRequest
import aws.sdk.kotlin.services.s3.model.UploadPartResponse
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.io.SdkBuffer
import aws.smithy.kotlin.runtime.io.SdkSource
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Represents a part in a multipart upload.
 *
 * @param number The part number.
 * @param bytes The bytes of the part.
 */
internal data class Part(
    val number: Int,
    val bytes: SdkBuffer,
)

internal suspend fun transferBytes(
    multiPartUpload: Boolean,
    contentLength: Long,
    partSizeBytes: Long,
    logger: Logger,
    uploadFileRequest: UploadFileRequest,
    context: TransferContext,
    mpuUploadId: String?,
    interceptors: List<TransferInterceptor>,
    client: S3Client,
    maxInMemoryParts: Int,
    maxConcurrentPartUploads: Int,
): List<CompletedPart> = coroutineScope {
    val uploadedParts = mutableListOf<CompletedPart>()

    if (multiPartUpload) {
        try {
            val partSize = resolvePartSize(contentLength, partSizeBytes, logger)
            val numberOfParts = ceilDiv(contentLength, partSize).toInt()
            val partSource = resolvePartSource(uploadFileRequest.body!!)

            val producer = produceParts(
                context.transferableBytes!!,
                partSource,
                partSize,
                numberOfParts,
                maxInMemoryParts,
            )

            val mutex = Mutex()
            repeat(maxConcurrentPartUploads) {
                consumer(
                    producer,
                    uploadFileRequest,
                    mpuUploadId!!,
                    context,
                    interceptors,
                    client,
                    uploadedParts,
                    mutex,
                )
            }

            if (uploadedParts.size != numberOfParts) {
                throw S3TransferManagerException("The number of uploaded parts does not match the expected count. Expected $numberOfParts, actual: ${uploadedParts.size}")
            }
        } catch (uploadPartException: Exception) {
            try {
                client.abortMultipartUpload {
                    bucket = uploadFileRequest.bucket
                    expectedBucketOwner = uploadFileRequest.expectedBucketOwner
                    key = uploadFileRequest.key
                    requestPayer = uploadFileRequest.requestPayer
                    uploadId = mpuUploadId
                }
                throw S3TransferManagerException("Multipart upload failed (ID: $mpuUploadId). One or more parts could not be uploaded", uploadPartException)
            } catch (abortException: Exception) {
                throw S3TransferManagerException("Multipart upload failed (ID: $mpuUploadId). Unable to abort multipart upload.", abortException)
                    .also { it.addSuppressed(uploadPartException) }
            }
        }
    } else {
        operationHook(
            BytesTransferred,
            context,
            interceptors,
        ) {
            context.currentBytes = uploadFileRequest.body // TODO: This will consume the bytes
            context.response = client.putObject(context.request as PutObjectRequest)
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
internal fun CoroutineScope.produceParts(
    readableBytes: Long,
    partSource: Any,
    partSize: Long,
    numberOfParts: Int,
    maxInMemoryParts: Int,
) = produce(
    capacity = maxInMemoryParts,
) {
    var readBytes = 0L
    var currentPartNumber = 1

    while (readBytes < readableBytes) {
        send(
            Part(
                currentPartNumber,
                nextPartBytes(
                    partSource,
                    partSize,
                    currentPartNumber == numberOfParts,
                    readBytes.toInt(),
                    readableBytes.toInt(),
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
 * where multiple consumers concurrently upload different parts of the same file.
 */
internal suspend fun consumer(
    channel: ReceiveChannel<Part>,
    uploadFileRequest: UploadFileRequest,
    mpuUploadId: String,
    context: TransferContext,
    interceptors: List<TransferInterceptor>,
    client: S3Client,
    uploadedParts: MutableList<CompletedPart>,
    mutex: Mutex,
) = coroutineScope {
    launch {
        for (part in channel) {
            val partSize = part.bytes.size // Store the original size, as it will shrink when bytes are read
            val localContext = context.copy() // Create a separate copy to avoid concurrent modifications

            localContext.request = uploadFileRequest.toUploadPartRequest(
                part.bytes,
                part.number,
                mpuUploadId,
            )
            localContext.currentBytes = object : ByteStream.SourceStream() {
                override fun readFrom(): SdkSource = part.bytes.peek() // Peek so bytes aren’t consumed before sending
                override val contentLength: Long = partSize
            }

            operationHook(
                BytesTransferred,
                localContext,
                interceptors,
            ) {
                localContext.response = client.uploadPart(localContext.request as UploadPartRequest)
                localContext.transferredBytes = localContext.transferredBytes!! + partSize
            }

            // Update shared state between coroutines
            mutex.withLock {
                context.request = localContext.request
                context.response = localContext.response
                context.transferableBytes = localContext.transferableBytes
                context.currentBytes = localContext.currentBytes
                context.transferredBytes = context.transferredBytes!! + partSize // Don't use transferredBytes from local context as it might be out of date
                context.transferableFiles = localContext.transferableFiles
                context.currentFile = localContext.currentFile
                context.transferredFiles = localContext.transferredFiles

                uploadedParts.add(
                    CompletedPart {
                        partNumber = part.number
                        eTag = (localContext.response as UploadPartResponse).eTag
                    },
                )
            }
        }
    }
}
