/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.uploadObject

import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.ConversionMapping

internal val uploadObjectConversions = listOf(
    ConversionMapping(
        source = TypeRef(
            "aws.sdk.kotlin.services.s3.model",
            "PutObjectResponse",
        ),
        destination = TypeRef(
            "aws.sdk.kotlin.hll.s3transfermanager.model",
            "UploadObjectResponse",
        ),
        setOf(
            "bucketKeyEnabled",
            "checksumCrc32",
            "checksumCrc32C",
            "checksumCrc64Nvme",
            "checksumSha1",
            "checksumSha256",
            "checksumType",
            "eTag",
            "expiration",
            "requestCharged",
            "sseCustomerAlgorithm",
            "sseCustomerKeyMd5",
            "ssekmsEncryptionContext",
            "ssekmsKeyId",
            "serverSideEncryption",
            "versionId",
        ),
    ),
    ConversionMapping(
        source = TypeRef(
            "aws.sdk.kotlin.services.s3.model",
            "CompleteMultipartUploadResponse",
        ),
        destination = TypeRef(
            "aws.sdk.kotlin.hll.s3transfermanager.model",
            "UploadObjectResponse",
        ),
        setOf(
            "bucketKeyEnabled",
            "checksumCrc32",
            "checksumCrc32C",
            "checksumCrc64Nvme",
            "checksumSha1",
            "checksumSha256",
            "checksumType",
            "eTag",
            "expiration",
            "requestCharged",
            "ssekmsKeyId",
            "serverSideEncryption",
            "versionId",
        ),
    ),
    ConversionMapping(
        source = TypeRef(
            "aws.sdk.kotlin.hll.s3transfermanager.model",
            "UploadObjectRequest",
        ),
        destination = TypeRef(
            "aws.sdk.kotlin.services.s3.model",
            "PutObjectRequest",
        ),
        setOf(
            "acl",
            "body",
            "bucket",
            "bucketKeyEnabled",
            "cacheControl",
            "checksumAlgorithm",
            "checksumCrc32",
            "checksumCrc32C",
            "checksumCrc64Nvme",
            "checksumSha1",
            "checksumSha256",
            "contentDisposition",
            "contentEncoding",
            "contentLanguage",
            "contentLength",
            "contentType",
            "expectedBucketOwner",
            "expires",
            "grantFullControl",
            "grantRead",
            "grantReadAcp",
            "grantWriteAcp",
            "ifMatch",
            "ifNoneMatch",
            "key",
            "metadata",
            "objectLockLegalHoldStatus",
            "objectLockMode",
            "objectLockRetainUntilDate",
            "requestPayer",
            "sseCustomerAlgorithm",
            "sseCustomerKey",
            "sseCustomerKeyMd5",
            "ssekmsEncryptionContext",
            "ssekmsKeyId",
            "serverSideEncryption",
            "storageClass",
            "tagging",
            "websiteRedirectLocation",
        ),
    ),
    ConversionMapping(
        source = TypeRef(
            "aws.sdk.kotlin.hll.s3transfermanager.model",
            "UploadObjectRequest",
        ),
        destination = TypeRef(
            "aws.sdk.kotlin.services.s3.model",
            "CreateMultipartUploadRequest",
        ),
        setOf(
            "acl",
            "bucket",
            "bucketKeyEnabled",
            "cacheControl",
            "checksumAlgorithm",
            "contentDisposition",
            "contentEncoding",
            "contentLanguage",
            "contentType",
            "expectedBucketOwner",
            "expires",
            "grantFullControl",
            "grantRead",
            "grantReadAcp",
            "grantWriteAcp",
            "key",
            "metadata",
            "objectLockLegalHoldStatus",
            "objectLockMode",
            "objectLockRetainUntilDate",
            "requestPayer",
            "sseCustomerAlgorithm",
            "sseCustomerKey",
            "sseCustomerKeyMd5",
            "ssekmsEncryptionContext",
            "ssekmsKeyId",
            "serverSideEncryption",
            "storageClass",
            "tagging",
            "websiteRedirectLocation",
        ),
    ),
    ConversionMapping(
        source = TypeRef(
            "aws.sdk.kotlin.hll.s3transfermanager.model",
            "UploadObjectRequest",
        ),
        destination = TypeRef(
            "aws.sdk.kotlin.services.s3.model",
            "UploadPartRequest",
        ),
        setOf(
            "bucket",
            "checksumAlgorithm",
            "expectedBucketOwner",
            "key",
            "requestPayer",
            "sseCustomerAlgorithm",
            "sseCustomerKey",
            "sseCustomerKeyMd5",
        ),
        listOf(
            TypeRef(
                "aws.smithy.kotlin.runtime.io",
                "SdkBuffer",
            ),
            TypeRef(
                "aws.smithy.kotlin.runtime.io",
                "SdkSource",
            ),
            TypeRef(
                "aws.smithy.kotlin.runtime.content",
                "ByteStream",
            ),
        ),
        listOf(
            "currentPart: SdkBuffer",
            "currentPartNumber: Int",
            "mpuUploadId: String",
        ),
        """
            uploadId = mpuUploadId
            body = object : ByteStream.SourceStream() {
                override fun readFrom(): SdkSource = currentPart
                override val contentLength: Long = currentPart.size
            }
            partNumber = currentPartNumber
        """.trimIndent(),
    ),
    ConversionMapping(
        source = TypeRef(
            "aws.sdk.kotlin.hll.s3transfermanager.model",
            "UploadObjectRequest",
        ),
        destination = TypeRef(
            "aws.sdk.kotlin.services.s3.model",
            "CompleteMultipartUploadRequest",
        ),
        setOf(
            "bucket",
            "checksumCrc32",
            "checksumCrc32C",
            "checksumCrc64Nvme",
            "checksumSha1",
            "checksumSha256",
            "expectedBucketOwner",
            "ifMatch",
            "ifNoneMatch",
            "key",
            "requestPayer",
            "sseCustomerAlgorithm",
            "sseCustomerKey",
            "sseCustomerKeyMd5",
        ),
        listOf(
            TypeRef(
                "aws.sdk.kotlin.services.s3.model",
                "CompletedPart",
            ),
        ),
        listOf(
            "mpuUploadId: String",
            "uploadedParts: List<CompletedPart>",
        ),
        """
            uploadId = mpuUploadId
            multipartUpload {
                parts = uploadedParts
            }
        """.trimIndent(),
    ),
)
