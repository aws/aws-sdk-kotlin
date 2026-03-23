/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager

import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferInterceptor
import aws.sdk.kotlin.hll.s3transfermanager.model.DownloadObjectRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.DownloadObjectResponse
import aws.sdk.kotlin.hll.s3transfermanager.model.MultipartDownloadType
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadObjectRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadObjectResponse
import aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject.downloadObjectImplementation
import aws.sdk.kotlin.hll.s3transfermanager.operations.uploadobject.uploadObjectImplementation
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectResponse
import kotlinx.coroutines.sync.Semaphore

/**
 * High level utility for managing transfers to Amazon S3.
 */
public class S3TransferManager private constructor(public val s3Client: S3Client, builder: Builder) {

    /**
     * Preferred part size for multipart uploads.
     * If using this size would require more than 10,000 parts (the S3 limit),
     * the smallest possible part size that results in 10,000 parts is used instead.
     *
     * Defaults to 8,000,000 bytes.
     */
    public val targetPartSizeBytes: Long = builder.targetPartSizeBytes

    /**
     * Threshold size at which an object upload uses multipart upload
     * instead of a single [S3Client.putObject] request.
     *
     * Defaults to 16,000,000 bytes.
     */
    public val multipartUploadThresholdBytes: Long = builder.multipartUploadThresholdBytes

    /**
     * Strategy for multipart downloads, defined by [MultipartDownloadType].
     * Downloads can be performed either by specifying byte ranges or by requesting individual parts.
     *
     * Defaults to [MultipartDownloadType.Part].
     */
    public val multipartDownloadType: MultipartDownloadType = builder.multipartDownloadType

    /**
     * Mutable list of [TransferInterceptor] instances, typically used to track transfers
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
     * Maximum number of concurrent part uploads for an object.
     * The actual number of uploads at any given time may be less than or equal but never greater.
     *
     * Defaults to 5.
     */
    public val maxConcurrentPartUploads: Int = builder.maxConcurrentPartUploads

    public companion object {
        public operator fun invoke(client: S3Client, block: Builder.() -> Unit = {}): S3TransferManager = Builder().apply(block).build(client)
    }

    public class Builder {
        /**
         * Preferred part size for multipart uploads.
         * If using this size would require more than 10,000 parts (the S3 limit),
         * the smallest possible part size that results in 10,000 parts is used instead.
         *
         * Defaults to 8,000,000 bytes.
         */
        public var targetPartSizeBytes: Long = 8_000_000

        /**
         * Threshold size at which an object upload uses multipart upload
         * instead of a single [S3Client.putObject] request.
         *
         * Defaults to 16,000,000 bytes.
         */
        public var multipartUploadThresholdBytes: Long = 16_000_000L

        /**
         * Strategy for multipart downloads, defined by [MultipartDownloadType].
         * Downloads can be performed either by specifying byte ranges or by requesting individual parts.
         *
         * Defaults to [MultipartDownloadType.Part].
         */
        public var multipartDownloadType: MultipartDownloadType = MultipartDownloadType.Part

        /**
         * Mutable list of [TransferInterceptor] instances, typically used to track transfers
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
         * Maximum number of concurrent part uploads for an object.
         * The actual number of uploads at any given time may be less than or equal but never greater.
         *
         * Defaults to 5.
         */
        public var maxConcurrentPartUploads: Int = 5

        internal fun build(client: S3Client): S3TransferManager = S3TransferManager(client, this)
    }

    // Keeps track of how many parts are in memory for this S3 TM via permits
    internal val bufferSemaphore = Semaphore(maxInMemoryParts)

    /**
     * Uploads an object to S3 via [aws.smithy.kotlin.runtime.content.ByteStream].
     * Uses multipart uploads with concurrent uploads if the object size is more than the configured [multipartUploadThresholdBytes].
     */
    public suspend fun uploadObject(
        uploadObjectRequest: UploadObjectRequest,
    ): UploadObjectResponse = uploadObjectImplementation(
        uploadObjectRequest,
        s3Client,
        multipartUploadThresholdBytes,
        targetPartSizeBytes,
        interceptors,
        maxInMemoryParts,
        maxConcurrentPartUploads,
        bufferSemaphore,
    )

    /**
     * Uploads an object to S3 via [aws.smithy.kotlin.runtime.content.ByteStream].
     * Uses multipart uploads with concurrent uploads if the object size is more than the configured [multipartUploadThresholdBytes].
     */
    public suspend inline fun uploadObject(
        crossinline block: UploadObjectRequest.Builder.() -> Unit,
    ): UploadObjectResponse = uploadObject(UploadObjectRequest.Builder().apply(block).build())

    // TODO: KDocs
    public suspend fun <T> downloadObject(
        downloadObjectRequest: DownloadObjectRequest,
        downloadPath: String? = null,
        objectHandler: (suspend (GetObjectResponse) -> T)? = null,
    ): DownloadObjectResponse = downloadObjectImplementation(
        downloadObjectRequest,
        objectHandler,
        s3Client,
        multipartDownloadType,
        targetPartSizeBytes,
        interceptors,
        downloadPath,
    )

    // TODO: KDocs
    public suspend inline fun <T> downloadObject(
        crossinline downloadObjectRequest: DownloadObjectRequest.Builder.() -> Unit,
        downloadPath: String? = null,
        noinline objectHandler: (suspend (GetObjectResponse) -> T)? = null,
    ): DownloadObjectResponse = downloadObject(DownloadObjectRequest.Builder().apply(downloadObjectRequest).build(), downloadPath, objectHandler)
}
