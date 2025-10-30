/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.uploadfile

import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.IoMapping
import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.MappingType

internal val uploadFileIoMappings = listOf(
    IoMapping(
        MappingType.REQUEST,
        "UploadFileRequest",
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
        "UploadFileResponse",
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
