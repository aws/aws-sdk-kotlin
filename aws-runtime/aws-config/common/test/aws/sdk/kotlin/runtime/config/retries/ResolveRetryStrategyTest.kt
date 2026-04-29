/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.retries

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.smithy.kotlin.runtime.ClientException
import aws.smithy.kotlin.runtime.retries.AdaptiveRetryStrategy
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ResolveRetryStrategyTest {
    @Test
    fun itResolvesMaxAttemptsFromEnvironmentVariables() = runTest {
        val expectedMaxAttempts = 50

        val platform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsMaxAttempts.envVar to expectedMaxAttempts.toString()),
        )

        val strategy = assertIs<StandardRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
    }

    @Test
    fun itResolvesMaxAttemptsFromSystemProperties() = runTest {
        val expectedMaxAttempts = 10
        val platform = TestPlatformProvider(
            props = mapOf(AwsSdkSetting.AwsMaxAttempts.sysProp to expectedMaxAttempts.toString()),
        )

        val strategy = assertIs<StandardRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
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

        val strategy = assertIs<StandardRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
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

    @Test
    fun itResolvesAdaptiveRetryStrategy() = runTest {
        val expectedMaxAttempts = StandardRetryStrategy.Config.DEFAULT_MAX_ATTEMPTS
        val adaptiveRetryMode = "adaptive"

        val platform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsRetryMode.envVar to adaptiveRetryMode),
        )

        val strategy = assertIs<AdaptiveRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
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

        assertEquals(expectedMaxAttempts, resolveRetryStrategy(platform).config.maxAttempts)
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

        val strategy = assertIs<AdaptiveRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
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

        val strategy = assertIs<StandardRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
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

        val strategy = assertIs<StandardRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
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

        val strategy = assertIs<StandardRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
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

        val strategy = assertIs<StandardRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
    }

    @Test
    fun itUsesDefaultMaxAttemptsWhenNoneAreProvided() = runTest {
        val expectedMaxAttempts = StandardRetryStrategy.Config.DEFAULT_MAX_ATTEMPTS

        val platform = TestPlatformProvider() // no environment variables / system properties / profile

        val strategy = assertIs<StandardRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
    }
}
