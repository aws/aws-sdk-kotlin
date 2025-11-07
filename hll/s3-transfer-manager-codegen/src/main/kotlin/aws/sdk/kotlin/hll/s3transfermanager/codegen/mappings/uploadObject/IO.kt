/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.uploadObject

import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.IoMapping
import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.MappingType

internal val uploadObjectIoMappings = listOf(
    IoMapping(
        MappingType.REQUEST,
        "UploadObjectRequest",
        "putObject",
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
    IoMapping(
        MappingType.RESPONSE,
        "UploadObjectResponse",
        "putObject",
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
)
