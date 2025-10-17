/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.model

import aws.sdk.kotlin.services.s3.model.ChecksumType
import aws.sdk.kotlin.services.s3.model.RequestCharged
import aws.sdk.kotlin.services.s3.model.ServerSideEncryption

public class UploadFileResponse(
    public val bucketKeyEnabled: Boolean?,
    public val checksumCrc32: String?,
    public val checksumCrc32C: String?,
    public val checksumCrc64Nvme: String?,
    public val checksumSha1: String?,
    public val checksumSha256: String?,
    public val checksumType: ChecksumType?,
    public val eTag: String?,
    public val expiration: String?,
    public val requestCharged: RequestCharged?,
    public val serverSideEncryption: ServerSideEncryption?,
    public val ssekmsKeyId: String?,
    public val versionId: String?,
) {
    public companion object {
        public operator fun invoke(block: Builder.() -> Unit): UploadFileResponse =
            Builder().apply(block).build()
    }

    public class Builder {
        public var bucketKeyEnabled: Boolean? = null
        public var checksumCrc32: String? = null
        public var checksumCrc32C: String? = null
        public var checksumCrc64Nvme: String? = null
        public var checksumSha1: String? = null
        public var checksumSha256: String? = null
        public var checksumType: ChecksumType? = null
        public var eTag: String? = null
        public var expiration: String? = null
        public var requestCharged: RequestCharged? = null
        public var serverSideEncryption: ServerSideEncryption? = null
        public var ssekmsKeyId: String? = null
        public var versionId: String? = null

        internal fun build(): UploadFileResponse =
            UploadFileResponse(
                bucketKeyEnabled,
                checksumCrc32,
                checksumCrc32C,
                checksumCrc64Nvme,
                checksumSha1,
                checksumSha256,
                checksumType,
                eTag,
                expiration,
                requestCharged,
                serverSideEncryption,
                ssekmsKeyId,
                versionId,
            )
    }
}
