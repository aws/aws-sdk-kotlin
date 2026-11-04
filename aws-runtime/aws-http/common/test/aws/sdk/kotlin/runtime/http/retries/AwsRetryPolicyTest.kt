/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http.retries

import aws.smithy.kotlin.runtime.ErrorMetadata
import aws.smithy.kotlin.runtime.ServiceErrorMetadata
import aws.smithy.kotlin.runtime.ServiceException
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.retries.policy.RetryDirective
import aws.smithy.kotlin.runtime.retries.policy.RetryErrorType
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

    private fun tooManyRequests() = HttpResponse(HttpStatusCode.TooManyRequests, Headers.Empty, HttpBody.Empty)

    private fun ServiceException.with429(errorCode: String? = null): ServiceException = apply {
        sdkErrorMetadata.attributes[ServiceErrorMetadata.ProtocolResponse] = tooManyRequests()
        errorCode?.let { sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorCode] = it }
    }

    private fun evaluate(ex: Throwable) = AwsRetryPolicy.Default.evaluate(Result.failure(ex))

    // A modeled error, as generated code renders it: the error type is set in `init`
    private class ModeledException(retryable: Boolean = false, throttling: Boolean = false) : ServiceException() {
        init {
            sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorType] = ErrorType.Client
            if (retryable) sdkErrorMetadata.attributes[ErrorMetadata.Retryable] = true
            if (throttling) sdkErrorMetadata.attributes[ErrorMetadata.ThrottlingError] = true
        }
    }

    @Test
    fun testUnmodeled429WithoutErrorCodeIsThrottling() {
        val result = evaluate(ServiceException().with429())
        assertEquals(RetryDirective.RetryError(RetryErrorType.Throttling), result)
    }

    @Test
    fun testUnmodeled429WithUnknownErrorCodeIsThrottling() {
        val result = evaluate(ServiceException().with429("SomeUnknownCode"))
        assertEquals(RetryDirective.RetryError(RetryErrorType.Throttling), result)
    }

    @Test
    fun test429WithKnownErrorCodeUsesKnownErrorType() {
        val result = evaluate(ServiceException().with429("RequestTimeout"))
        assertEquals(RetryDirective.RetryError(RetryErrorType.Transient), result)
    }

    @Test
    fun testModeledNonRetryable429IsNotRetried() {
        val result = evaluate(ModeledException().with429("SomeModeledError"))
        assertEquals(RetryDirective.TerminateAndFail, result)
    }

    @Test
    fun testModeledThrottling429IsThrottling() {
        val result = evaluate(ModeledException(retryable = true, throttling = true).with429("SomeModeledError"))
        assertEquals(RetryDirective.RetryError(RetryErrorType.Throttling), result)
    }

    @Test
    fun testUnmodeled429WithQueryCompatibleSenderTypeIsNotRetried() {
        // Pins a known limitation: awsQueryCompatible deserializers overwrite the error type from the
        // `x-amzn-query-error` header (here `SomeUnknownCode;Sender`), so the unmodeled 429 rule does not apply.
        // See the AwsRetryPolicy documentation.
        val ex = ServiceException().with429("SomeUnknownCode")
        ex.sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorType] = ServiceException.ErrorType.Client
        assertEquals(RetryDirective.TerminateAndFail, evaluate(ex))
    }

    private fun evaluateLegacy(ex: Throwable) = LegacyAwsRetryPolicy.evaluate(Result.failure(ex))

    @Test
    fun testLegacyUnmodeled429IsNotRetried() {
        assertEquals(RetryDirective.TerminateAndFail, evaluateLegacy(ServiceException().with429()))
        assertEquals(RetryDirective.TerminateAndFail, evaluateLegacy(ServiceException().with429("SomeUnknownCode")))
    }

    @Test
    fun testLegacy429WithKnownErrorCodeUsesKnownErrorType() {
        val result = evaluateLegacy(ServiceException().with429("ThrottlingException"))
        assertEquals(RetryDirective.RetryError(RetryErrorType.Throttling), result)
    }

    @Test
    fun testLegacyModeledThrottlingErrorUsesErrorType() {
        val result = evaluateLegacy(ModeledException(retryable = true, throttling = true).with429("SomeModeledError"))
        assertEquals(RetryDirective.RetryError(RetryErrorType.ClientSide), result)
    }

    @Test
    fun testLegacyKnownErrorsMatchDefault() {
        AwsRetryPolicy.knownErrorTypes.forEach { (errorCode, errorType) ->
            val ex = ServiceException()
            ex.sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorCode] = errorCode
            assertEquals(RetryDirective.RetryError(errorType), evaluateLegacy(ex))
        }
        AwsRetryPolicy.knownStatusCodes.forEach { (statusCode, errorType) ->
            val ex = ServiceException()
            val response = HttpResponse(HttpStatusCode.fromValue(statusCode), Headers.Empty, HttpBody.Empty)
            ex.sdkErrorMetadata.attributes[ServiceErrorMetadata.ProtocolResponse] = response
            assertEquals(RetryDirective.RetryError(errorType), evaluateLegacy(ex))
        }
    }
}
