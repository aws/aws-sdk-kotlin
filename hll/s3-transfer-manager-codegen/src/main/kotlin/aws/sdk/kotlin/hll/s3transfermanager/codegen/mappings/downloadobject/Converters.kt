/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.downloadobject

import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.ConversionMapping

internal val downloadObjectConversions = listOf(
    ConversionMapping(
        source = TypeRef(
            "aws.sdk.kotlin.hll.s3transfermanager.model",
            "DownloadObjectRequest",
        ),
        destination = TypeRef(
            "aws.sdk.kotlin.services.s3.model",
            "GetObjectRequest",
        ),
        additionalParameters = listOf(
            "partNumber: Int? = null",
            "range: String? = null",
        ),
        members = setOf(
            "bucket",
            "checksumMode",
            "expectedBucketOwner",
            "ifMatch",
            "ifModifiedSince",
            "ifNoneMatch",
            "ifUnmodifiedSince",
            "key",
            "requestPayer",
            "responseCacheControl",
            "responseContentDisposition",
            "responseContentEncoding",
            "responseContentLanguage",
            "responseContentType",
            "responseExpires",
            "sseCustomerAlgorithm",
            "sseCustomerKey",
            "sseCustomerKeyMd5",
            "versionId",
        ),
        additionalLogic = """
            this@GetObjectRequest.partNumber = partNumber
            this@GetObjectRequest.range = range
        """.trimIndent(),
    ),
    ConversionMapping(
        source = TypeRef(
            "aws.sdk.kotlin.services.s3.model",
            "GetObjectResponse",
        ),
        destination = TypeRef(
            "aws.sdk.kotlin.hll.s3transfermanager.model",
            "DownloadObjectResponse",
        ),
        additionalParameters = listOf(
            "_contentLength: Long? = null",
            "_contentRange: String? = null",
        ),
        members = setOf(
            "acceptRanges",
            "bucketKeyEnabled",
            "cacheControl",
            "checksumCrc32",
            "checksumCrc32C",
            "checksumCrc64Nvme",
            "checksumSha1",
            "checksumSha256",
            "checksumType",
            "contentDisposition",
            "contentEncoding",
            "contentLanguage",
            "contentLength",
            "contentRange",
            "contentType",
            "deleteMarker",
            "eTag",
            "expiration",
            "expires",
            "lastModified",
            "metadata",
            "missingMeta",
            "objectLockLegalHoldStatus",
            "objectLockMode",
            "objectLockRetainUntilDate",
            "partsCount",
            "replicationStatus",
            "requestCharged",
            "restore",
            "sseCustomerAlgorithm",
            "sseCustomerKeyMd5",
            "ssekmsKeyId",
            "serverSideEncryption",
            "storageClass",
            "tagCount",
            "versionId",
            "websiteRedirectLocation",
        ),
        additionalLogic = """ 
            _contentLength?.let { contentLength = _contentLength }
            _contentRange?.let { contentRange = _contentRange }
        """.trimIndent(),
    )
)
