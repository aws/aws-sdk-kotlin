/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.retries

import aws.smithy.kotlin.runtime.retries.RetryContext
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.retries.getOrThrow
import aws.smithy.kotlin.runtime.retries.policy.RetryDirective
import aws.smithy.kotlin.runtime.retries.policy.RetryErrorType
import aws.smithy.kotlin.runtime.retries.policy.RetryPolicy
import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

private val DYNAMODB_SERVICES = setOf("dynamodb", "dynamodb streams")

// DynamoDB retry defaults — must match DynamoDbRetryDefaultsIntegration constants
private const val DYNAMODB_MAX_ATTEMPTS = 4
private val DYNAMODB_INITIAL_DELAY = 25.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class AwsRetryIntegrationTest {

    @Test
    fun testAwsServiceSpecificRetryCases() = runTest {
        val testCases = awsRetryTestCases.mapValues {
            Yaml.default.decodeFromString(AwsRetryTestCase.serializer(), it.value)
        }

        testCases.forEach { (name, tc) ->
            // Map service name to codegen defaults (simulating what codegen would generate)
            val isDynamoDb = tc.given.service.lowercase() in DYNAMODB_SERVICES
            val defaultMaxAttempts = if (isDynamoDb) DYNAMODB_MAX_ATTEMPTS else null
            val defaultInitialDelay = if (isDynamoDb) DYNAMODB_INITIAL_DELAY else null

            val strategy = StandardRetryStrategy {
                configureRetryDefaults(
                    configuredMaxAttempts = tc.given.maxAttempts,
                    useNewRetries = true,
                    defaultMaxAttempts = defaultMaxAttempts,
                    defaultInitialDelay = defaultInitialDelay,
                )
                delayProvider {
                    if (tc.given.exponentialBase == 1.0) jitter = 0.0
                }
            }

            val policy = object : RetryPolicy<Unit> {
                override fun evaluate(result: Result<Unit>): RetryDirective = when {
                    result.isSuccess -> RetryDirective.TerminateAndSucceed
                    else -> RetryDirective.RetryError(RetryErrorType.ServerSide)
                }
            }

            var attempt = 0
            val startTime = currentTime

            val result = runCatching {
                withContext(RetryContext()) {
                    strategy.retry(policy) {
                        val code = tc.responses[attempt++].response.statusCode
                        if (code != 200) throw TestServerException(code)
                    }
                }
            }

            val totalDelayMs = currentTime - startTime
            val expectedDelayMs = tc.responses.mapNotNull { it.expected.delay }.sumOf { (it * 1000).toLong() }
            val finalOutcome = tc.responses.last().expected.outcome

            when (finalOutcome) {
                AwsRetryOutcome.Success ->
                    assertTrue(result.isSuccess, "Expected success for '$name' but got ${result.exceptionOrNull()}")
                AwsRetryOutcome.MaxAttemptsExceeded ->
                    assertIs<TestServerException>(result.exceptionOrNull(), "Expected exception for '$name'")
                AwsRetryOutcome.RetryRequest ->
                    fail("Final outcome should not be retry_request for '$name'")
            }

            assertEquals(expectedDelayMs, totalDelayMs, "Delay mismatch for '$name'")
        }
    }
}

private class TestServerException(val code: Int) : Exception("HTTP $code")
