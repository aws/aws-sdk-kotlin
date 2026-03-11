/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.express

import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.collections.AttributeKey
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.operation.HttpOperationContext
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.operation.ExecutionContext

internal const val S3_EXPRESS_ENDPOINT_PROPERTY_KEY = "backend"
internal const val S3_EXPRESS_ENDPOINT_PROPERTY_VALUE = "S3Express"

/**
 * Re-configures the default checksum algorithm for S3 Express
 * NOTE: Default checksums are disabled for s3:UploadPart.
 */
internal class S3ExpressDefaultChecksumAlgorithm(
    private val isS3UploadPart: Boolean,
) : HttpInterceptor {
    override suspend fun modifyBeforeSigning(context: ProtocolRequestInterceptorContext<Any, HttpRequest>): HttpRequest {
        if (context.executionContext.usingS3Express()) {
            if (isS3UploadPart) {
                context.executionContext.remove(HttpOperationContext.DefaultChecksumAlgorithm)
            } else {
                context.executionContext[HttpOperationContext.DefaultChecksumAlgorithm] = "CRC32"
            }
        }
        return super.modifyBeforeSigning(context)
    }
}

private fun ExecutionContext.usingS3Express(): Boolean = this.getOrNull(AttributeKey(S3_EXPRESS_ENDPOINT_PROPERTY_KEY)) != S3_EXPRESS_ENDPOINT_PROPERTY_VALUE
