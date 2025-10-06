/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.withConfig

/**
 * High level utility for managing transfers to Amazon S3.
 */
public class S3TransferManager private constructor(
    public val client: S3Client,
    public val partSize: Long?,
    public val multipartUploadThreshold: Long?,
    public val multipartDownloadType: MultiPartDownloadType?,
    public val interceptors: MutableList<TransferInterceptor>?,
) {
    public companion object {
        public operator fun invoke(block: Builder.() -> Unit): S3TransferManager =
            Builder().apply(block).build()
    }

    public class Builder {
        public var client: S3Client? = null
        public var partSize: Long? = 8_000_000L
        public var multipartUploadThreshold: Long? = 16_000_000L
        public var multipartDownloadType: MultiPartDownloadType? = Part
        public var interceptors: MutableList<TransferInterceptor>? = mutableListOf()

        internal fun build(): S3TransferManager =
            S3TransferManager(
                client = client?.withConfig { interceptors += BusinessMetricInterceptor } ?: error("client must be set"),
                partSize = partSize,
                multipartUploadThreshold = multipartUploadThreshold,
                multipartDownloadType = multipartDownloadType,
                interceptors = interceptors
            )
    }

    public fun x(): String = "" // TODO
}
