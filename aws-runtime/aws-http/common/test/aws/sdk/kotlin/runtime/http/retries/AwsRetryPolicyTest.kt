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
    fun testInvalidCredentialErrorsAreRetriedWhenTheCredentialsWereMarkedForRefresh() {
        listOf("ExpiredToken", "InvalidToken").forEach { errorCode ->
            val ex = ServiceException()
            ex.sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorCode] = errorCode
            ex.sdkErrorMetadata.attributes[CredentialsMarkedForRefresh] = true
            val result = AwsRetryPolicy.Default.evaluate(Result.failure(ex))
            assertEquals(RetryDirective.RetryError(RetryErrorType.Transient), result, "expected $errorCode to be retryable")
        }
    }

    @Test
    fun testInvalidCredentialErrorsAreNotRetriedOtherwise() {
        // Without a refresh, a retry would be signed with the same rejected credentials and cannot succeed.
        listOf("ExpiredToken", "InvalidToken").forEach { errorCode ->
            val ex = ServiceException()
            ex.sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorCode] = errorCode
            val result = AwsRetryPolicy.Default.evaluate(Result.failure(ex))
            assertEquals(RetryDirective.TerminateAndFail, result, "expected $errorCode not to be retried")
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
}
