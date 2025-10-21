/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.model

import aws.sdk.kotlin.services.s3.model.ChecksumAlgorithm
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.sdk.kotlin.services.s3.model.ObjectLockLegalHoldStatus
import aws.sdk.kotlin.services.s3.model.ObjectLockMode
import aws.sdk.kotlin.services.s3.model.RequestPayer
import aws.sdk.kotlin.services.s3.model.ServerSideEncryption
import aws.sdk.kotlin.services.s3.model.StorageClass
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.time.Instant

public class UploadFileRequest private constructor(
    public val acl: ObjectCannedAcl?,
    public val body: ByteStream?,
    public val bucket: String?,
    public val bucketKeyEnabled: Boolean?,
    public val cacheControl: String?,
    public val checksumAlgorithm: ChecksumAlgorithm?,
    public val checksumCrc32: String?,
    public val checksumCrc32C: String?,
    public val checksumCrc64Nvme: String?,
    public val checksumSha1: String?,
    public val checksumSha256: String?,
    public val contentDisposition: String?,
    public val contentEncoding: String?,
    public val contentLanguage: String?,
    public val contentType: String?,
    public val expectedBucketOwner: String?,
    public val expires: Instant?,
    public val grantFullControl: String?,
    public val grantRead: String?,
    public val grantReadAcp: String?,
    public val grantWriteAcp: String?,
    public val ifMatch: String?,
    public val ifNoneMatch: String?,
    public val key: String?,
    public val metadata: Map<String, String>?,
    public val objectLockLegalHoldStatus: ObjectLockLegalHoldStatus?,
    public val objectLockMode: ObjectLockMode?,
    public val objectLockRetainUntilDate: Instant?,
    public val requestPayer: RequestPayer?,
    public val sseCustomerAlgorithm: String?,
    public val sseCustomerKey: String?,
    public val sseCustomerKeyMd5: String?,
    public val ssekmsEncryptionContext: String?,
    public val ssekmsKeyId: String?,
    public val serverSideEncryption: ServerSideEncryption?,
    public val storageClass: StorageClass?,
    public val tagging: String?,
    public val websiteRedirectLocation: String?,
) {
    public companion object {
        public operator fun invoke(block: Builder.() -> Unit): UploadFileRequest =
            Builder().apply(block).build()
    }

    public class Builder {
        public var acl: ObjectCannedAcl? = null
        public var body: ByteStream? = null
        public var bucket: String? = null
        public var bucketKeyEnabled: Boolean? = null
        public var cacheControl: String? = null
        public var checksumAlgorithm: ChecksumAlgorithm? = null
        public var checksumCrc32: String? = null
        public var checksumCrc32C: String? = null
        public var checksumCrc64Nvme: String? = null
        public var checksumSha1: String? = null
        public var checksumSha256: String? = null
        public var contentDisposition: String? = null
        public var contentEncoding: String? = null
        public var contentLanguage: String? = null
        public var contentType: String? = null
        public var expectedBucketOwner: String? = null
        public var expires: Instant? = null
        public var grantFullControl: String? = null
        public var grantRead: String? = null
        public var grantReadAcp: String? = null
        public var grantWriteAcp: String? = null
        public var ifMatch: String? = null
        public var ifNoneMatch: String? = null
        public var key: String? = null
        public var metadata: Map<String, String>? = null
        public var objectLockLegalHoldStatus: ObjectLockLegalHoldStatus? = null
        public var objectLockMode: ObjectLockMode? = null
        public var objectLockRetainUntilDate: Instant? = null
        public var requestPayer: RequestPayer? = null
        public var sseCustomerAlgorithm: String? = null
        public var sseCustomerKey: String? = null
        public var sseCustomerKeyMd5: String? = null
        public var ssekmsEncryptionContext: String? = null
        public var ssekmsKeyId: String? = null
        public var serverSideEncryption: ServerSideEncryption? = null
        public var storageClass: StorageClass? = null
        public var tagging: String? = null
        public var websiteRedirectLocation: String? = null

        public fun build(): UploadFileRequest =
            UploadFileRequest(
                acl,
                body,
                bucket,
                bucketKeyEnabled,
                cacheControl,
                checksumAlgorithm,
                checksumCrc32,
                checksumCrc32C,
                checksumCrc64Nvme,
                checksumSha1,
                checksumSha256,
                contentDisposition,
                contentEncoding,
                contentLanguage,
                contentType,
                expectedBucketOwner,
                expires,
                grantFullControl,
                grantRead,
                grantReadAcp,
                grantWriteAcp,
                ifMatch,
                ifNoneMatch,
                key,
                metadata,
                objectLockLegalHoldStatus,
                objectLockMode,
                objectLockRetainUntilDate,
                requestPayer,
                sseCustomerAlgorithm,
                sseCustomerKey,
                sseCustomerKeyMd5,
                ssekmsEncryptionContext,
                ssekmsKeyId,
                serverSideEncryption,
                storageClass,
                tagging,
                websiteRedirectLocation,
            )
    }
}
