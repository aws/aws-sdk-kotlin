/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.retries

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * SEP test cases that are service-specific and owned by aws-sdk-kotlin.
 * Matches the YAML from the "Standard Mode Testcases" section of the Retry Behavior 2.1 SEP.
 */
val awsRetryTestCases = mapOf(
    "DynamoDB Base Backoff (25ms) and Increased Retries" to // language=YAML
        """
            given:
              service: dynamodb
              exponential_base: 1
            responses:
              - response:
                  status_code: 500
                expected:
                  outcome: retry_request
                  retry_quota: 486
                  delay: 0.025
              - response:
                  status_code: 500
                expected:
                  outcome: retry_request
                  retry_quota: 472
                  delay: 0.05
              - response:
                  status_code: 500
                expected:
                  outcome: retry_request
                  retry_quota: 458
                  delay: 0.1
              - response:
                  status_code: 500
                expected:
                  outcome: max_attempts_exceeded
                  retry_quota: 458
        """.trimIndent(),
)

@Serializable
data class AwsRetryTestCase(val given: AwsRetryGiven, val responses: List<AwsRetryResponseAndExpectation>)

@Serializable
data class AwsRetryGiven(
    val service: String,
    @SerialName("max_attempts") val maxAttempts: Int? = null,
    @SerialName("exponential_base") val exponentialBase: Double? = null,
)

@Serializable
data class AwsRetryResponseAndExpectation(val response: AwsRetryResponse, val expected: AwsRetryExpectation)

@Serializable
data class AwsRetryResponse(@SerialName("status_code") val statusCode: Int)

@Serializable
data class AwsRetryExpectation(
    val outcome: AwsRetryOutcome,
    @SerialName("retry_quota") val retryQuota: Int? = null,
    val delay: Double? = null,
)

@Serializable
enum class AwsRetryOutcome {
    @SerialName("success")
    Success,

    @SerialName("retry_request")
    RetryRequest,

    @SerialName("max_attempts_exceeded")
    MaxAttemptsExceeded,
}
