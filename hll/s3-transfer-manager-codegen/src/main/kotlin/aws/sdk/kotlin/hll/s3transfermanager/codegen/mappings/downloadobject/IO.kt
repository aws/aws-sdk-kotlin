/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.downloadobject

import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.IoMapping
import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.MappingType

internal val downloadObjectIoMappings = listOf(
    IoMapping(
        MappingType.REQUEST,
        "DownloadObjectRequest",
        "getObject",
        setOf(
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
    ),
    IoMapping(
        MappingType.RESPONSE,
        "DownloadObjectResponse",
        "getObject",
        setOf(
            "acceptRanges",
            "bucketKeyEnabled",
            "cacheControl",
            "checksumCRC32",
            "checksumCRC32C",
            "checksumCRC64NVME",
            "checksumSHA1",
            "checksumSHA256",
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
            "serverSideEncryption",
            "sseCustomerAlgorithm",
            "sseCustomerKeyMD5",
            "ssekmsKeyId",
            "storageClass",
            "tagCount",
            "versionId",
            "websiteRedirectLocation",
        ),
    ),
)
