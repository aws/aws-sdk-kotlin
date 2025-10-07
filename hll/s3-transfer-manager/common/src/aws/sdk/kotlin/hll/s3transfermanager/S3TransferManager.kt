/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager

import aws.sdk.kotlin.hll.s3transfermanager.model.MultiPartDownloadType
import aws.sdk.kotlin.hll.s3transfermanager.model.Part
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileRequest.Companion.toCreateMultiPartUploadRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileRequest.Companion.toPutObjectRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileResponse
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CreateMultipartUploadRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.withConfig
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * High level utility for managing transfers to Amazon S3.
 */
public class S3TransferManager private constructor(
    public val client: S3Client,
    public val partSize: Long,
    public val multipartUploadThreshold: Long,
    public val multipartDownloadType: MultiPartDownloadType,
    public val interceptors: MutableList<TransferInterceptor>,
) {
    public companion object {
        public operator fun invoke(block: Builder.() -> Unit): S3TransferManager =
            Builder().apply(block).build()
    }

    public class Builder {
        public var client: S3Client? = null
        public var partSize: Long? = null
        public var multipartUploadThreshold: Long? = null
        public var multipartDownloadType: MultiPartDownloadType? = null
        public var interceptors: MutableList<TransferInterceptor>? = null

        internal fun build(): S3TransferManager =
            S3TransferManager(
                client = client?.withConfig { interceptors += BusinessMetricInterceptor } ?: error("client must be set"),
                partSize = partSize ?: 8_000_000L,
                multipartUploadThreshold = multipartUploadThreshold ?: 16_000_000L,
                multipartDownloadType = multipartDownloadType ?: Part,
                interceptors = interceptors ?: mutableListOf(),
            )
    }

    private var context: TransferContext = TransferContext()

    // TODO: Try to find parts of the code you can commonize
    /**
     * TODO
     */
    public suspend fun uploadFile(uploadFileRequest: UploadFileRequest): Deferred<UploadFileResponse> = coroutineScope {
        async {
            val multiPartUpload = uploadFileRequest.contentLength >= multipartUploadThreshold

            context.request = if (multiPartUpload) {
                uploadFileRequest.toCreateMultiPartUploadRequest()
            } else {
                uploadFileRequest.toPutObjectRequest()
            }

            operationHook(TransferInitiated) {
                context.transferredBytes = 0L
                context.transferableBytes = uploadFileRequest.contentLength

                if (multiPartUpload) {
                    context.response = client.createMultipartUpload(context.request as CreateMultipartUploadRequest)
                }
            }

            operationHook(BytesTransferred) {
                if (multiPartUpload) {
                    // TODO: MPU logic
                    // TODO: Update bytes transferred
                } else {
                    context.response = client.putObject(context.request as PutObjectRequest)
                    context.transferredBytes = context.transferableBytes
                }
            }

            operationHook(TransferCompleted) {
                if (multiPartUpload) {
                    // TODO: MPU logic
                    // TODO: Update bytes transferred?
                }
            }

            UploadFileResponse.fromS3Response(context.response)
        }
    }

    private suspend fun operationHook(hook: TransferHook, block: suspend () -> Any) {
        interceptors.forEach { interceptor ->
            when (hook) {
                is TransferInitiated -> {
                    interceptor.readBeforeTransferInitiated(context)
                    context = interceptor.modifyBeforeTransferInitiated(context)
                    block.invoke()
                    interceptor.readAfterTransferInitiated(context)
                    context = interceptor.modifyAfterTransferInitiated(context)
                }
                is BytesTransferred -> {
                    interceptor.readBeforeBytesTransferred(context)
                    context = interceptor.modifyBeforeBytesTransferred(context)
                    block.invoke()
                    interceptor.readAfterBytesTransferred(context)
                    context = interceptor.modifyAfterBytesTransferred(context)
                }
                is FileTransferred -> {
                    interceptor.readBeforeFileTransferred(context)
                    context = interceptor.modifyBeforeFileTransferred(context)
                    block.invoke()
                    interceptor.readAfterFileTransferred(context)
                    context = interceptor.modifyAfterFileTransferred(context)
                }
                is TransferCompleted -> {
                    interceptor.readBeforeTransferCompleted(context)
                    context = interceptor.modifyBeforeTransferCompleted(context)
                    block.invoke()
                    interceptor.readAfterTransferCompleted(context)
                    context = interceptor.modifyAfterTransferCompleted(context)
                }
                else -> error("TransferHook not implemented: ${hook::class.simpleName}")
            }
        }
    }
}
