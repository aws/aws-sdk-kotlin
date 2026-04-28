/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http.retries

import aws.smithy.kotlin.runtime.ServiceErrorMetadata
import aws.smithy.kotlin.runtime.ServiceException
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.retries.policy.RetryDirective
import aws.smithy.kotlin.runtime.retries.policy.RetryErrorType
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class AwsRetryPolicyTest {
    @Test
    fun testErrorsByErrorCode() {
        AwsRetryPolicy.knownErrorTypes.forEach { (errorCode, errorType) ->
            val ex = ServiceException()
            ex.sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorCode] = errorCode
            val result = AwsRetryPolicy.Default.evaluate(Result.failure(ex))
            assertEquals(RetryDirective.RetryError(errorType), result)
        }
    }

    @Test
    fun testErrorsByStatusCode() {
        AwsRetryPolicy.knownStatusCodes.forEach { (statusCode, errorType) ->
            val modeledStatusCode = HttpStatusCode.fromValue(statusCode)
            val response = HttpResponse(modeledStatusCode, Headers.Empty, HttpBody.Empty)
            val ex = ServiceException()
            ex.sdkErrorMetadata.attributes[ServiceErrorMetadata.ProtocolResponse] = response
            val result = AwsRetryPolicy.Default.evaluate(Result.failure(ex))
            assertEquals(RetryDirective.RetryError(errorType), result)
        }
    }

    // When new retry flag is off (default), IDPCommunicationError is retryable for all services (legacy behavior)
    @Test
    fun testIDPCommunicationErrorRetriedForAllServicesWhenFlagOff() {
        val platform = TestPlatformProvider()
        listOf("STS", "IAM", null).forEach { service ->
            val policy = AwsRetryPolicy(service, platform)
            val ex = ServiceException()
            ex.sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorCode] = "IDPCommunicationError"
            assertEquals(RetryDirective.RetryError(RetryErrorType.Transient), policy.evaluate(Result.failure(ex)))
        }
    }

    // When new retry flag is on, IDPCommunicationError is only retried for STS
    @Test
    fun testIDPCommunicationErrorRetriedOnlyForStsWhenFlagOn() {
        val platform = TestPlatformProvider(env = mapOf("AWS_NEW_RETRIES_2026" to "true"))
        val stsPolicy = AwsRetryPolicy("STS", platform)
        val ex = ServiceException()
        ex.sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorCode] = "IDPCommunicationError"
        assertEquals(RetryDirective.RetryError(RetryErrorType.Transient), stsPolicy.evaluate(Result.failure(ex)))
    }

    @Test
    fun testIDPCommunicationErrorNotRetriedForOtherServicesWhenFlagOn() {
        val platform = TestPlatformProvider(env = mapOf("AWS_NEW_RETRIES_2026" to "true"))
        val iamPolicy = AwsRetryPolicy("IAM", platform)
        val ex = ServiceException()
        ex.sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorCode] = "IDPCommunicationError"
        assertEquals(RetryDirective.TerminateAndFail, iamPolicy.evaluate(Result.failure(ex)))
    }

    @Test
    fun testIDPCommunicationErrorNotRetriedWithoutServiceNameWhenFlagOn() {
        val platform = TestPlatformProvider(env = mapOf("AWS_NEW_RETRIES_2026" to "true"))
        val policy = AwsRetryPolicy(null, platform)
        val ex = ServiceException()
        ex.sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorCode] = "IDPCommunicationError"
        assertEquals(RetryDirective.TerminateAndFail, policy.evaluate(Result.failure(ex)))
    }
}
