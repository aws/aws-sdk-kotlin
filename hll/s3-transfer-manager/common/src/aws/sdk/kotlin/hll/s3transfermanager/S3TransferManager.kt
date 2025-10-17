/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager

import aws.sdk.kotlin.hll.s3transfermanager.model.MultiPartDownloadType
import aws.sdk.kotlin.hll.s3transfermanager.model.Part
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileResponse
import aws.sdk.kotlin.hll.s3transfermanager.utils.buildCompleteMultipartUploadRequest
import aws.sdk.kotlin.hll.s3transfermanager.utils.buildUploadPartRequest
import aws.sdk.kotlin.hll.s3transfermanager.utils.ceilDiv
import aws.sdk.kotlin.hll.s3transfermanager.utils.getNextPart
import aws.sdk.kotlin.hll.s3transfermanager.utils.resolvePartSize
import aws.sdk.kotlin.hll.s3transfermanager.utils.toCreateMultiPartUploadRequest
import aws.sdk.kotlin.hll.s3transfermanager.utils.toPutObjectRequest
import aws.sdk.kotlin.hll.s3transfermanager.utils.toUploadFileResponse
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.abortMultipartUpload
import aws.sdk.kotlin.services.s3.model.CompleteMultipartUploadRequest
import aws.sdk.kotlin.services.s3.model.CompleteMultipartUploadResponse
import aws.sdk.kotlin.services.s3.model.CompletedPart
import aws.sdk.kotlin.services.s3.model.CreateMultipartUploadRequest
import aws.sdk.kotlin.services.s3.model.CreateMultipartUploadResponse
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import aws.sdk.kotlin.services.s3.model.UploadPartRequest
import aws.sdk.kotlin.services.s3.model.UploadPartResponse
import aws.sdk.kotlin.services.s3.withConfig
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.io.SdkBuffer
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * High level utility for managing transfers to Amazon S3.
 */
public class S3TransferManager private constructor(
    public val client: S3Client,
    public val targePartSize: Long,
    public val multipartUploadThreshold: Long,
    public val multipartDownloadType: MultiPartDownloadType,
    public val interceptors: MutableList<TransferInterceptor>,
) {
    internal var context: TransferContext = TransferContext()

    public companion object {
        public operator fun invoke(block: Builder.() -> Unit): S3TransferManager =
            Builder().apply(block).build()
    }

    public class Builder {
        public var client: S3Client? = null
        public var targePartSize: Long = 8_000_000
        public var multipartUploadThreshold: Long = 16_000_000L
        public var multipartDownloadType: MultiPartDownloadType = Part
        public var interceptors: MutableList<TransferInterceptor> = mutableListOf()

        internal fun build(): S3TransferManager =
            S3TransferManager(
                client = client?.withConfig { interceptors += BusinessMetricInterceptor } ?: error("client must be set"),
                targePartSize = targePartSize,
                multipartUploadThreshold = multipartUploadThreshold,
                multipartDownloadType = multipartDownloadType,
                interceptors = interceptors,
            )
    }

    /**
     * Executes a sequence of operations around a hook.
     *
     * The execution flow is as follows:
     * 1. Runs all interceptors scheduled to execute **before** the hook.
     * 2. Executes the main hook logic.
     * 3. Runs all interceptors scheduled to execute **after** the hook.
     */
    private suspend fun operationHook(hook: TransferHook, block: suspend () -> Any) {
        when (hook) {
            is TransferInitiated -> {
                interceptors.forEach { it.readBeforeTransferInitiated(context) }
                interceptors.forEach { context = it.modifyBeforeTransferInitiated(context) }
                block.invoke()
                interceptors.forEach { it.readAfterTransferInitiated(context) }
                interceptors.forEach { context = it.modifyAfterTransferInitiated(context) }
            }
            is BytesTransferred -> {
                interceptors.forEach { it.readBeforeBytesTransferred(context) }
                interceptors.forEach { context = it.modifyBeforeBytesTransferred(context) }
                block.invoke()
                interceptors.forEach { it.readAfterBytesTransferred(context) }
                interceptors.forEach { context = it.modifyAfterBytesTransferred(context) }
            }
            is FileTransferred -> {
                interceptors.forEach { it.readBeforeFileTransferred(context) }
                interceptors.forEach { context = it.modifyBeforeFileTransferred(context) }
                block.invoke()
                interceptors.forEach { it.readAfterFileTransferred(context) }
                interceptors.forEach { context = it.modifyAfterFileTransferred(context) }
            }
            is TransferCompleted -> {
                interceptors.forEach { it.readBeforeTransferCompleted(context) }
                interceptors.forEach { context = it.modifyBeforeTransferCompleted(context) }
                block.invoke()
                interceptors.forEach { it.readAfterTransferCompleted(context) }
                interceptors.forEach { context = it.modifyAfterTransferCompleted(context) }
            }
            else -> error("TransferHook not implemented: ${hook::class.simpleName}")
        }
    }

    /**
     * Uploads a byte stream to Amazon S3, automatically using multipart uploads
     * for large objects as needed.
     *
     * This function handles the complexity of splitting the data into parts,
     * uploading each part, and completing the multipart upload. For object smaller than [multipartUploadThreshold],
     * a standard single-part upload is performed automatically.
     *
     * If the specified [targePartSize] for multipart uploads is too small to allow
     * all parts to fit within S3's limit of 10,000 parts, the part size will be
     * automatically increased so that exactly 10,000 parts are uploaded.
     */
    public suspend fun uploadFile(uploadFileRequest: UploadFileRequest): Deferred<UploadFileResponse> = coroutineScope {
        val multiPartUpload = uploadFileRequest.contentLength >= multipartUploadThreshold
        val uploadedParts = mutableListOf<CompletedPart>()
        var mpuUploadId = "null"

        val logger = coroutineContext.logger<S3TransferManager>()

        /*
         Handles transfer initiated hook
         */
        suspend fun transferInitiated(multiPartUpload: Boolean) {
            context.transferredBytes = 0L
            context.transferableBytes = uploadFileRequest.contentLength
            context.request = if (multiPartUpload) {
                uploadFileRequest.toCreateMultiPartUploadRequest()
            } else {
                uploadFileRequest.toPutObjectRequest()
            }
            operationHook(TransferInitiated) {
                if (multiPartUpload) {
                    context.response = client.createMultipartUpload(context.request as CreateMultipartUploadRequest)
                    mpuUploadId = (context.response as CreateMultipartUploadResponse).uploadId ?: throw Exception("Missing upload id in create multipart upload response")
                }
            }
        }

        /*
         Handles bytes transferred hook
         */
        suspend fun transferBytes(multiPartUpload: Boolean) {
            if (multiPartUpload) {
                try {
                    val partSize = resolvePartSize(uploadFileRequest, this@S3TransferManager, logger)
                    val numberOfParts = ceilDiv(uploadFileRequest.contentLength, partSize)
                    val partSource = when (uploadFileRequest.body) {
                        is ByteStream.Buffer -> uploadFileRequest.body.bytes()
                        is ByteStream.ChannelStream -> uploadFileRequest.body.readFrom()
                        is ByteStream.SourceStream -> uploadFileRequest.body.readFrom()
                        else -> error("Unhandled body type: ${uploadFileRequest.body?.let { it::class.simpleName } ?: "null"}")
                    }
                    val partBuffer = SdkBuffer()
                    var currentPartNumber = 1L

                    while (context.transferredBytes!! < context.transferableBytes!!) {
                        partBuffer.getNextPart(partSource, partSize, this@S3TransferManager)
                        if (currentPartNumber != numberOfParts) {
                            check(partBuffer.size == partSize) {
                                "Part #$currentPartNumber size mismatch detected. Expected $partSize, actual: ${partBuffer.size}"
                            }
                        }

                        context.request =
                            buildUploadPartRequest(
                                uploadFileRequest,
                                partBuffer,
                                currentPartNumber,
                                mpuUploadId,
                            )

                        operationHook(BytesTransferred) {
                            context.response = client.uploadPart(context.request as UploadPartRequest)
                            context.transferredBytes = context.transferredBytes!! + partSize
                        }

                        uploadedParts += CompletedPart {
                            partNumber = currentPartNumber.toInt()
                            eTag = (context.response as UploadPartResponse).eTag
                        }
                        currentPartNumber += 1
                    }

                    check(uploadedParts.size == numberOfParts.toInt()) {
                        "The number of uploaded parts does not match the expected count. Expected $numberOfParts, actual: ${uploadedParts.size}"
                    }
                } catch (uploadPartThrowable: Throwable) {
                    try {
                        client.abortMultipartUpload {
                            bucket = uploadFileRequest.bucket
                            expectedBucketOwner = uploadFileRequest.expectedBucketOwner
                            key = uploadFileRequest.key
                            requestPayer = uploadFileRequest.requestPayer
                            uploadId = mpuUploadId
                        }
                        throw Exception("Multipart upload failed (ID: $mpuUploadId). One or more parts could not be uploaded", uploadPartThrowable)
                    } catch (abortThrowable: Throwable) {
                        throw Exception("Multipart upload failed (ID: $mpuUploadId). Unable to abort multipart upload.", abortThrowable)
                    }
                }
            } else {
                operationHook(BytesTransferred) {
                    context.response = client.putObject(context.request as PutObjectRequest)
                    context.transferredBytes = context.transferableBytes
                }
            }
        }

        /*
         Handles transfer completed hook
         */
        suspend fun transferComplete(multiPartUpload: Boolean) {
            if (multiPartUpload) {
                context.request = buildCompleteMultipartUploadRequest(uploadFileRequest, mpuUploadId, uploadedParts)
            }
            operationHook(TransferCompleted) {
                if (multiPartUpload) {
                    try {
                        context.response = client.completeMultipartUpload(context.request as CompleteMultipartUploadRequest)
                    } catch (t: Throwable) {
                        throw Exception("Unable to complete multipart upload with ID: $mpuUploadId", t)
                    }
                }
            }
        }

        async {
            checkNotNull(uploadFileRequest.body?.contentLength) {
                "UploadFileRequest.body.contentLength must be set"
            }
            check(uploadFileRequest.body.contentLength == uploadFileRequest.contentLength) {
                "contentLength mismatch. uploadFileRequest: ${uploadFileRequest.contentLength}, uploadFileRequest.body.contentLength: ${uploadFileRequest.body.contentLength}"
            }

            transferInitiated(multiPartUpload)
            transferBytes(multiPartUpload)
            transferComplete(multiPartUpload)

            when (context.response) {
                is PutObjectResponse -> (context.response as PutObjectResponse).toUploadFileResponse()
                is CompleteMultipartUploadResponse -> (context.response as CompleteMultipartUploadResponse).toUploadFileResponse()
                else -> error("Unexpected response: ${context.response?.let { it::class.simpleName } ?: "null"}")
            }
        }
    }

    /**
     * Uploads a byte stream to Amazon S3, automatically using multipart uploads
     * for large objects as needed.
     *
     * This function handles the complexity of splitting the data into parts,
     * uploading each part, and completing the multipart upload. For object smaller than [multipartUploadThreshold],
     * a standard single-part upload is performed automatically.
     *
     * If the specified [targePartSize] for multipart uploads is too small to allow
     * all parts to fit within S3's limit of 10,000 parts, the part size will be
     * automatically increased so that exactly 10,000 parts are uploaded.
     */
    public suspend inline fun uploadFile(crossinline block: UploadFileRequest.Builder.() -> Unit): Deferred<UploadFileResponse> = uploadFile(UploadFileRequest.Builder().apply(block).build())
}
