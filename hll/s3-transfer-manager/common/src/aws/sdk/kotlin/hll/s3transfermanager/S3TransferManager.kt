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

    /**
     * Preferred part size for multipart uploads.
     * If using this size would require more than 10,000 parts (the S3 limit),
     * the smallest possible part size that results in 10,000 parts is used instead.
     *
     * Default to 8,000,000 bytes.
     */
    public val partSizeBytes: Long = builder.partSizeBytes

    /**
     * Threshold size above which a file upload uses multipart upload
     * instead of a single put object request.
     *
     * Defaults to 16,000,000 bytes.
     */
    public val multipartUploadThresholdBytes: Long = builder.multipartUploadThresholdBytes

    /**
     * Strategy for multipart downloads, defined by [MultipartDownloadType].
     * Downloads can be performed either by specifying byte ranges or by requesting individual parts.
     *
     * Defaults to [Part].
     */
    public val multipartDownloadType: MultipartDownloadType = builder.multipartDownloadType

    /**
     * Mutable list of [TransferInterceptor]s, typically used to track transfers
     * or inspect/modify low-level S3 requests.
     */
    public val interceptors: MutableList<TransferInterceptor> = builder.interceptors

    /**
     * The maximum amount of parts to buffer in memory while waiting for uploads to complete.
     * The actual number of parts buffered at any given time may be less than or equal but never greater.
     *
     * Defaults to 5.
     */
    public val maxInMemoryParts: Int = builder.maxInMemoryParts

    /**
     * Maximum number of concurrent part uploads for a file.
     * The actual number of uploads at any given time may be less than or equal but never greater.
     *
     * Defaults to 5.
     */
    public val maxConcurrentPartUploads: Int = builder.maxConcurrentPartUploads

    public companion object {
        public operator fun invoke(client: S3Client, block: Builder.() -> Unit): S3TransferManager =
            Builder().apply(block).build(client)
    }

    public class Builder {
        /**
         * Preferred part size for multipart uploads.
         * If using this size would require more than 10,000 parts (the S3 limit),
         * the smallest possible part size that results in 10,000 parts is used instead.
         *
         * Default to 8,000,000 bytes.
         */
        public var partSizeBytes: Long = 8_000_000

        /**
         * Threshold size above which a file upload uses multipart upload
         * instead of a single put object request.
         *
         * Defaults to 16,000,000 bytes.
         */
        public var multipartUploadThresholdBytes: Long = 16_000_000L

        /**
         * Strategy for multipart downloads, defined by [MultipartDownloadType].
         * Downloads can be performed either by specifying byte ranges or by requesting individual parts.
         *
         * Defaults to [Part].
         */
        public var multipartDownloadType: MultipartDownloadType = Part

        /**
         * Mutable list of [TransferInterceptor]s, typically used to track transfers
         * or inspect/modify low-level S3 requests.
         */
        public var interceptors: MutableList<TransferInterceptor> = mutableListOf()

        /**
         * The maximum amount of parts to buffer in memory while waiting for uploads to complete.
         * The actual number of parts buffered at any given time may be less than or equal but never greater.
         *
         * Defaults to 5.
         */
        public var maxInMemoryParts: Int = 5

        /**
         * Maximum number of concurrent part uploads for a file.
         * The actual number of uploads at any given time may be less than or equal but never greater.
         *
         * Defaults to 5.
         */
        public var maxConcurrentPartUploads: Int = 5

        internal fun build(client: S3Client): S3TransferManager =
            S3TransferManager(client, this)
    }

    /**
     * Uploads a file to S3 via [aws.smithy.kotlin.runtime.content.ByteStream].
     * Uses multipart uploads with concurrent uploads if the object size is more than the configured [multipartUploadThresholdBytes].
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
     * Uploads a file to S3 via [aws.smithy.kotlin.runtime.content.ByteStream].
     * Uses multipart uploads with concurrent uploads if the object size is more than the configured [multipartUploadThresholdBytes].
     */
    public suspend inline fun uploadFile(
        crossinline block: UploadFileRequest.Builder.() -> Unit,
    ): UploadFileResponse =
        uploadFile(UploadFileRequest.Builder().apply(block).build())
}
