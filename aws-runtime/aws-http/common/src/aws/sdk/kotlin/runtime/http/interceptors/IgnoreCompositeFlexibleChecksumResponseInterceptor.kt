/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.http.interceptors

import aws.smithy.kotlin.runtime.client.ProtocolResponseInterceptorContext
import aws.smithy.kotlin.runtime.client.config.ResponseHttpChecksumConfig
import aws.smithy.kotlin.runtime.http.interceptors.FlexibleChecksumsResponseInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.telemetry.logging.info

/**
 * Variant of the [FlexibleChecksumsResponseInterceptor] where composite checksums are not validated
 */
public class IgnoreCompositeFlexibleChecksumResponseInterceptor(
    responseValidationRequired: Boolean,
    responseChecksumValidation: ResponseHttpChecksumConfig?,
) : FlexibleChecksumsResponseInterceptor(
    responseValidationRequired,
    responseChecksumValidation,
) {
    override fun ignoreChecksum(
        checksum: String,
        context: ProtocolResponseInterceptorContext<Any, HttpRequest, HttpResponse>,
    ): Boolean =
        checksum.isCompositeChecksum().also { compositeChecksum ->
            if (compositeChecksum) {
                context.executionContext.coroutineContext.info<IgnoreCompositeFlexibleChecksumResponseInterceptor> {
                    "Checksum validation was skipped because it was a composite checksum"
                }
            }
        }
}

/**
 * Verifies if a checksum is composite.
 */
private fun String.isCompositeChecksum(): Boolean {
    // Ends with "-#" where "#" is a number
    val regex = Regex("-(\\d)+$")
    return regex.containsMatchIn(this)
}
