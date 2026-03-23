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
)
