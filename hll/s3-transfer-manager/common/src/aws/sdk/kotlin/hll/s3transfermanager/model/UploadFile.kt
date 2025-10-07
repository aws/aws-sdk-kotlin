/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.model

import aws.sdk.kotlin.services.s3.model.ChecksumAlgorithm
import aws.sdk.kotlin.services.s3.model.ChecksumType
import aws.sdk.kotlin.services.s3.model.CompleteMultipartUploadResponse
import aws.sdk.kotlin.services.s3.model.CreateMultipartUploadRequest
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.sdk.kotlin.services.s3.model.ObjectLockLegalHoldStatus
import aws.sdk.kotlin.services.s3.model.ObjectLockMode
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import aws.sdk.kotlin.services.s3.model.RequestCharged
import aws.sdk.kotlin.services.s3.model.RequestPayer
import aws.sdk.kotlin.services.s3.model.ServerSideEncryption
import aws.sdk.kotlin.services.s3.model.StorageClass
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.time.Instant
import kotlin.Boolean

// TODO: Add documentation to each thing ?
/**
 * TODO
 */
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
    public val contentLength: Long,
    public val contentType: String?,
    public val expectedBucketOwner: String?,
    public val expires: Instant?, // TODO: Is this the right instant?
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
    public val serverSideEncryption: ServerSideEncryption?,
    public val source: String?,
    public val sseCustomerAlgorithm: String?,
    public val sseCustomerKey: String?,
    public val sseCustomerKeyMd5: String?,
    public val ssekmsEncryptionContext: String?,
    public val ssekmsKeyId: String?,
    public val storageClass: StorageClass?,
    public val tagging: String?,
    public val websiteRedirectLocation: String?,
) {
    public companion object {
        public operator fun invoke(block: Builder.() -> Unit): UploadFileRequest =
            Builder().apply(block).build()

        // TODO: Checkout the null values CAREFULLY
        internal fun UploadFileRequest.toPutObjectRequest(): PutObjectRequest =
            PutObjectRequest {
                acl = this@toPutObjectRequest.acl
                body = this@toPutObjectRequest.body
                bucket = this@toPutObjectRequest.bucket
                bucketKeyEnabled = this@toPutObjectRequest.bucketKeyEnabled
                cacheControl = this@toPutObjectRequest.cacheControl
                checksumAlgorithm = this@toPutObjectRequest.checksumAlgorithm
                checksumCrc32 = this@toPutObjectRequest.checksumCrc32
                checksumCrc32C = this@toPutObjectRequest.checksumCrc32C
                checksumCrc64Nvme = this@toPutObjectRequest.checksumCrc64Nvme
                checksumSha1 = this@toPutObjectRequest.checksumSha1
                checksumSha256 = this@toPutObjectRequest.checksumSha256
                contentDisposition = this@toPutObjectRequest.contentDisposition
                contentEncoding = this@toPutObjectRequest.contentEncoding
                contentLanguage = this@toPutObjectRequest.contentLanguage
                contentLength = this@toPutObjectRequest.contentLength
                contentMd5 = null
                contentType = this@toPutObjectRequest.contentType
                expectedBucketOwner = this@toPutObjectRequest.expectedBucketOwner
                expires = this@toPutObjectRequest.expires
                grantFullControl = this@toPutObjectRequest.grantFullControl
                grantRead = this@toPutObjectRequest.grantRead
                grantReadAcp = this@toPutObjectRequest.grantReadAcp
                grantWriteAcp = this@toPutObjectRequest.grantWriteAcp
                ifMatch = this@toPutObjectRequest.ifMatch
                ifNoneMatch = this@toPutObjectRequest.ifNoneMatch
                key = this@toPutObjectRequest.key
                metadata = this@toPutObjectRequest.metadata
                objectLockLegalHoldStatus = this@toPutObjectRequest.objectLockLegalHoldStatus
                objectLockMode = this@toPutObjectRequest.objectLockMode
                objectLockRetainUntilDate = this@toPutObjectRequest.objectLockRetainUntilDate
                requestPayer = this@toPutObjectRequest.requestPayer
                serverSideEncryption = this@toPutObjectRequest.serverSideEncryption
                sseCustomerAlgorithm = this@toPutObjectRequest.sseCustomerAlgorithm
                sseCustomerKey = this@toPutObjectRequest.sseCustomerKey
                sseCustomerKeyMd5 = this@toPutObjectRequest.sseCustomerKeyMd5
                ssekmsEncryptionContext = this@toPutObjectRequest.ssekmsEncryptionContext
                ssekmsKeyId = this@toPutObjectRequest.ssekmsKeyId
                storageClass = this@toPutObjectRequest.storageClass
                tagging = this@toPutObjectRequest.tagging
                websiteRedirectLocation = this@toPutObjectRequest.websiteRedirectLocation
                writeOffsetBytes = null
            }

        // TODO: Checkout the null values CAREFULLY
        internal fun UploadFileRequest.toCreateMultiPartUploadRequest(): CreateMultipartUploadRequest =
            CreateMultipartUploadRequest {
                acl = this@toCreateMultiPartUploadRequest.acl
                bucket = this@toCreateMultiPartUploadRequest.bucket
                bucketKeyEnabled = this@toCreateMultiPartUploadRequest.bucketKeyEnabled
                cacheControl = this@toCreateMultiPartUploadRequest.cacheControl
                checksumAlgorithm = this@toCreateMultiPartUploadRequest.checksumAlgorithm
                checksumType = null
                contentDisposition = this@toCreateMultiPartUploadRequest.contentDisposition
                contentEncoding = this@toCreateMultiPartUploadRequest.contentEncoding
                contentLanguage = this@toCreateMultiPartUploadRequest.contentLanguage
                contentType = this@toCreateMultiPartUploadRequest.contentType
                expectedBucketOwner = this@toCreateMultiPartUploadRequest.expectedBucketOwner
                expires = this@toCreateMultiPartUploadRequest.expires
                grantFullControl = this@toCreateMultiPartUploadRequest.grantFullControl
                grantRead = this@toCreateMultiPartUploadRequest.grantRead
                grantReadAcp = this@toCreateMultiPartUploadRequest.grantReadAcp
                grantWriteAcp = this@toCreateMultiPartUploadRequest.grantWriteAcp
                key = this@toCreateMultiPartUploadRequest.key
                metadata = this@toCreateMultiPartUploadRequest.metadata
                objectLockLegalHoldStatus = this@toCreateMultiPartUploadRequest.objectLockLegalHoldStatus
                objectLockMode = this@toCreateMultiPartUploadRequest.objectLockMode
                objectLockRetainUntilDate = this@toCreateMultiPartUploadRequest.objectLockRetainUntilDate
                requestPayer = this@toCreateMultiPartUploadRequest.requestPayer
                serverSideEncryption = this@toCreateMultiPartUploadRequest.serverSideEncryption
                sseCustomerAlgorithm = this@toCreateMultiPartUploadRequest.sseCustomerAlgorithm
                sseCustomerKey = this@toCreateMultiPartUploadRequest.sseCustomerKey
                sseCustomerKeyMd5 = this@toCreateMultiPartUploadRequest.sseCustomerKeyMd5
                ssekmsEncryptionContext = this@toCreateMultiPartUploadRequest.ssekmsEncryptionContext
                ssekmsKeyId = this@toCreateMultiPartUploadRequest.ssekmsKeyId
                storageClass = this@toCreateMultiPartUploadRequest.storageClass
                tagging = this@toCreateMultiPartUploadRequest.tagging
                websiteRedirectLocation = this@toCreateMultiPartUploadRequest.websiteRedirectLocation
            }
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
        public var contentLength: Long? = null
        public var contentType: String? = null
        public var expectedBucketOwner: String? = null
        public var expires: Instant? = null // TODO: Is this the right instant? Write some tests to see if conversions work properly?
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
        public var source: String? = null
        public var serverSideEncryption: ServerSideEncryption? = null
        public var sseCustomerAlgorithm: String? = null
        public var sseCustomerKey: String? = null
        public var sseCustomerKeyMd5: String? = null
        public var ssekmsEncryptionContext: String? = null
        public var ssekmsKeyId: String? = null
        public var storageClass: StorageClass? = null
        public var tagging: String? = null
        public var websiteRedirectLocation: String? = null

        internal fun build(): UploadFileRequest =
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
                contentLength ?: error("contentLength must be set"),
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
                serverSideEncryption,
                source,
                sseCustomerAlgorithm,
                sseCustomerKey,
                sseCustomerKeyMd5,
                ssekmsEncryptionContext,
                ssekmsKeyId,
                storageClass,
                tagging,
                websiteRedirectLocation,
            )
    }
}

/**
 * TODO
 */
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

        // TODO: Double check the conversion
        internal fun fromS3Response(response: Any?): UploadFileResponse =
            when (response) {
                is PutObjectResponse ->
                    UploadFileResponse {
                        bucketKeyEnabled = response.bucketKeyEnabled
                        checksumCrc32 = response.checksumCrc32
                        checksumCrc32C = response.checksumCrc32C
                        checksumCrc64Nvme = response.checksumCrc64Nvme
                        checksumSha1 = response.checksumSha1
                        checksumSha256 = response.checksumSha256
                        checksumType = response.checksumType
                        eTag = response.eTag
                        expiration = response.expiration
                        requestCharged = response.requestCharged
                        serverSideEncryption = response.serverSideEncryption
                        ssekmsKeyId = response.ssekmsKeyId
                        versionId = response.versionId
                    }
                is CompleteMultipartUploadResponse ->
                    UploadFileResponse {
                        bucketKeyEnabled = response.bucketKeyEnabled
                        checksumCrc32 = response.checksumCrc32
                        checksumCrc32C = response.checksumCrc32C
                        checksumCrc64Nvme = response.checksumCrc64Nvme
                        checksumSha1 = response.checksumSha1
                        checksumSha256 = response.checksumSha256
                        checksumType = response.checksumType
                        eTag = response.eTag
                        expiration = response.expiration
                        requestCharged = response.requestCharged
                        serverSideEncryption = response.serverSideEncryption
                        ssekmsKeyId = response.ssekmsKeyId
                        versionId = response.versionId
                    }
                else -> error("Response must be a PutObjectResponse or CompleteMultipartUploadResponse")
            }
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
