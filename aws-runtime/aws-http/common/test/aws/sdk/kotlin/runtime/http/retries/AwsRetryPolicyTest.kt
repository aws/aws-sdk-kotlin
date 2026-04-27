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

    @Test
    fun testIDPCommunicationErrorRetriedForSts() {
        val policy = AwsRetryPolicy("STS")
        val ex = ServiceException()
        ex.sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorCode] = "IDPCommunicationError"
        assertEquals(RetryDirective.RetryError(RetryErrorType.Transient), policy.evaluate(Result.failure(ex)))
    }

    @Test
    fun testIDPCommunicationErrorNotRetriedForOtherServices() {
        val policy = AwsRetryPolicy("IAM")
        val ex = ServiceException()
        ex.sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorCode] = "IDPCommunicationError"
        assertEquals(RetryDirective.TerminateAndFail, policy.evaluate(Result.failure(ex)))
    }

    @Test
    fun testIDPCommunicationErrorNotRetriedWithoutServiceName() {
        val policy = AwsRetryPolicy()
        val ex = ServiceException()
        ex.sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorCode] = "IDPCommunicationError"
        assertEquals(RetryDirective.TerminateAndFail, policy.evaluate(Result.failure(ex)))
    }
}
