/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.retries

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.testing.TestPlatformProvider
import aws.smithy.kotlin.runtime.ClientException
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategyOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class ResolveRetryStrategyTest {

    @Test
    fun itResolvesMaxAttemptsFromEnvironmentVariables() = runTest {
        val expectedMaxAttempts = 50

        val platform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsMaxAttempts.envVar to expectedMaxAttempts.toString()),
        )

        assertEquals(expectedMaxAttempts, resolveRetryStrategy(platform).options.maxAttempts)
    }

    @Test
    fun itResolvesMaxAttemptsFromSystemProperties() = runTest {
        val expectedMaxAttempts = 10
        val platform = TestPlatformProvider(
            props = mapOf(AwsSdkSetting.AwsMaxAttempts.sysProp to expectedMaxAttempts.toString()),
        )

        assertEquals(expectedMaxAttempts, resolveRetryStrategy(platform).options.maxAttempts)
    }

    @Test
    fun itResolvesMaxAttemptsFromProfile() = runTest {
        val expectedMaxAttempts = 30

        val platform = TestPlatformProvider(
            env = mapOf("AWS_CONFIG_FILE" to "config"),
            fs = mapOf(
                "config" to """
                [default]
                max_attempts=$expectedMaxAttempts
                """.trimIndent(),
            ),
        )

        assertEquals(expectedMaxAttempts, resolveRetryStrategy(platform).options.maxAttempts)
    }

    @Test
    fun itThrowsOnInvalidMaxAttemptsValues() = runTest {
        val invalidMaxAttemptsValues = listOf(-91, -5, 0)

        for (invalidMaxAttempts in invalidMaxAttemptsValues) {
            val platform = TestPlatformProvider(
                env = mapOf(AwsSdkSetting.AwsMaxAttempts.envVar to invalidMaxAttempts.toString()),
            )

            assertFailsWith<ConfigurationException> { resolveRetryStrategy(platform) }
        }
    }

    @Test
    fun itThrowsOnUnsupportedRetryModes() = runTest {
        val retryMode = "unsupported-retry-mode"

        val platform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsRetryMode.envVar to retryMode),
        )

        assertFailsWith<ClientException> { resolveRetryStrategy(platform) }
    }

    @Test
    fun itThrowsOnUnsupportedRetryModesFromProfile() = runTest {
        val expectedMaxAttempts = 30
        val retryMode = "unsupported-retry-mode"

        val platform = TestPlatformProvider(
            env = mapOf("AWS_CONFIG_FILE" to "config"),
            fs = mapOf(
                "config" to """
                [default]
                max_attempts=$expectedMaxAttempts
                retry_mode=$retryMode
                """.trimIndent(),
            ),
        )

        assertFailsWith<ConfigurationException> { resolveRetryStrategy(platform) }
    }

    // TODO: Remove this test after https://github.com/awslabs/aws-sdk-kotlin/issues/701 is complete
    @Test
    fun itThrowsOnUnimplementedAdaptiveRetryStrategy() = runTest {
        val adaptiveRetryMode = "adaptive"

        val platform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsRetryMode.envVar to adaptiveRetryMode),
        )

        assertFailsWith<NotImplementedError> { resolveRetryStrategy(platform) }
    }

    @Test
    fun itResolvesNonLowercaseRetryModeValuesFromEnvironmentVariables() = runTest {
        val expectedMaxAttempts = 16
        val retryMode = "lEgACY"

        val platform = TestPlatformProvider(
            env = mapOf(
                AwsSdkSetting.AwsMaxAttempts.envVar to expectedMaxAttempts.toString(),
                AwsSdkSetting.AwsRetryMode.envVar to retryMode,
            ),
        )

        assertEquals(expectedMaxAttempts, resolveRetryStrategy(platform).options.maxAttempts)
    }

    @Test
    fun itResolvesNonLowercaseRetryModeValuesFromProfile() = runTest {
        val expectedMaxAttempts = 19
        val retryMode = "aDAPtive"

        val platform = TestPlatformProvider(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
            ),
            fs = mapOf(
                "config" to """
                [default]
                max_attempts=$expectedMaxAttempts
                retry_mode=$retryMode
                """.trimIndent(),
            ),
        )

        assertFailsWith<NotImplementedError> { resolveRetryStrategy(platform) }
    }

    @Test
    fun itResolvesMaxAttemptsAndRetryModeFromEnvironmentVariables() = runTest {
        val expectedMaxAttempts = 1
        val retryMode = "legacy"

        val platform = TestPlatformProvider(
            env = mapOf(
                AwsSdkSetting.AwsMaxAttempts.envVar to expectedMaxAttempts.toString(),
                AwsSdkSetting.AwsRetryMode.envVar to retryMode,
            ),
        )

        assertEquals(expectedMaxAttempts, resolveRetryStrategy(platform).options.maxAttempts)
    }

    @Test
    fun itResolvesMaxAttemptsAndRetryModeFromEnvironmentVariablesAndSystemProperty() = runTest {
        val expectedMaxAttempts = 90
        val retryMode = "legacy"

        val platform = TestPlatformProvider(
            env = mapOf(
                AwsSdkSetting.AwsRetryMode.envVar to retryMode,
            ),
            props = mapOf(
                AwsSdkSetting.AwsMaxAttempts.sysProp to expectedMaxAttempts.toString(),
            ),
        )

        assertEquals(expectedMaxAttempts, resolveRetryStrategy(platform).options.maxAttempts)
    }

    @Test
    fun itResolvesMaxAttemptsAndRetryModeFromEnvironmentVariablesAndProfile() = runTest {
        val expectedMaxAttempts = 33
        val retryMode = "standard"

        val platform = TestPlatformProvider(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
                AwsSdkSetting.AwsRetryMode.envVar to retryMode,
            ),
            fs = mapOf(
                "config" to """
                [default]
                max_attempts=$expectedMaxAttempts
                retry_mode=invalid-retry-mode-should-be-ignored
                """.trimIndent(),
            ),
        )

        assertEquals(expectedMaxAttempts, resolveRetryStrategy(platform).options.maxAttempts)
    }

    @Test
    fun itResolvesWithEnvironmentVariablePriority() = runTest {
        // set the profile and environment variable max_attempts. resolution should prioritize environment variables
        val expectedMaxAttempts = 40
        val retryMode = "standard"

        val platform = TestPlatformProvider(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
                AwsSdkSetting.AwsRetryMode.envVar to retryMode,
                AwsSdkSetting.AwsMaxAttempts.envVar to expectedMaxAttempts.toString(),
            ),
            fs = mapOf(
                "config" to """
                [default]
                max_attempts=55
                retry_mode=invalid-retry-mode-should-be-ignored
                """.trimIndent(),
            ),
        )

        assertEquals(expectedMaxAttempts, resolveRetryStrategy(platform).options.maxAttempts)
    }

    @Test
    fun itUsesDefaultMaxAttemptsWhenNoneAreProvided() = runTest {
        val expectedMaxAttempts = StandardRetryStrategyOptions.Default.maxAttempts

        val platform = TestPlatformProvider() // no environment variables / system properties / profile

        assertEquals(expectedMaxAttempts, resolveRetryStrategy(platform).options.maxAttempts)
    }
}
