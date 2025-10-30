/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager

import aws.sdk.kotlin.hll.s3transfermanager.model.MultipartDownloadType
import aws.sdk.kotlin.hll.s3transfermanager.model.Part
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileResponse
import aws.sdk.kotlin.hll.s3transfermanager.operations.uploadfile.uploadFileImplementation
import aws.sdk.kotlin.hll.s3transfermanager.utils.S3TransferManagerBusinessMetricInterceptor
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.withConfig

/**
 * High level utility for managing transfers to Amazon S3.
 */
public class S3TransferManager private constructor(s3Client: S3Client, builder: Builder) {
    public val client: S3Client = s3Client.withConfig { interceptors += S3TransferManagerBusinessMetricInterceptor }
    public val partSizeBytes: Long = builder.partSizeBytes
    public val multipartUploadThresholdBytes: Long = builder.multipartUploadThresholdBytes
    public val multipartDownloadType: MultipartDownloadType = builder.multipartDownloadType
    public val interceptors: MutableList<TransferInterceptor> = builder.interceptors
    public val maxInMemoryParts: Int = builder.maxInMemoryParts
    public val maxConcurrentPartUploads: Int = builder.maxConcurrentPartUploads

    public companion object {
        public operator fun invoke(client: S3Client, block: Builder.() -> Unit): S3TransferManager =
            Builder().apply(block).build(client)
    }

    public class Builder {
        // TODO: K-docs for each one
        public var partSizeBytes: Long = 8_000_000
        public var multipartUploadThresholdBytes: Long = 16_000_000L
        public var multipartDownloadType: MultipartDownloadType = Part
        public var interceptors: MutableList<TransferInterceptor> = mutableListOf()
        public var maxInMemoryParts: Int = 5
        public var maxConcurrentPartUploads: Int = 5

        internal fun build(client: S3Client): S3TransferManager =
            S3TransferManager(client, this)
    }

    /**
     * Uploads a byte stream to Amazon S3, automatically using multipart uploads
     * for large objects as needed.
     *
     * This function handles the complexity of splitting the data into parts,
     * uploading each part, and completing the multipart upload. For object smaller than [multipartUploadThresholdBytes],
     * a standard single-part upload is performed automatically.
     *
     * If the specified [partSizeBytes] for multipart uploads is too small to allow
     * all parts to fit within S3's limit of 10,000 parts, the part size will be
     * automatically increased so that exactly 10,000 parts are uploaded.
     */
    public suspend fun uploadFile(
        uploadFileRequest: UploadFileRequest,
    ): UploadFileResponse =
        uploadFileImplementation(
            uploadFileRequest,
            client,
            multipartUploadThresholdBytes,
            partSizeBytes,
            interceptors,
            maxInMemoryParts,
            maxConcurrentPartUploads,
        )

    /**
     * Uploads a byte stream to Amazon S3, automatically using multipart uploads
     * for large objects as needed.
     *
     * This function handles the complexity of splitting the data into parts,
     * uploading each part, and completing the multipart upload. For object smaller than [multipartUploadThresholdBytes],
     * a standard single-part upload is performed automatically.
     *
     * If the specified [partSizeBytes] for multipart uploads is too small to allow
     * all parts to fit within S3's limit of 10,000 parts, the part size will be
     * automatically increased so that exactly 10,000 parts are uploaded.
     */
    public suspend inline fun uploadFile(
        crossinline block: UploadFileRequest.Builder.() -> Unit,
    ): UploadFileResponse =
        uploadFile(UploadFileRequest.Builder().apply(block).build())
}
