/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.retries

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.loadAwsSharedConfig
import aws.smithy.kotlin.runtime.ClientException
import aws.smithy.kotlin.runtime.client.config.RetryMode
import aws.smithy.kotlin.runtime.retries.AdaptiveRetryStrategy
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.retries.delay.ExponentialBackoffWithJitter
import aws.smithy.kotlin.runtime.retries.delay.RetryCapacityExceededException
import aws.smithy.kotlin.runtime.retries.delay.StandardRetryTokenBucket
import aws.smithy.kotlin.runtime.retries.policy.RetryErrorType
import aws.smithy.kotlin.runtime.util.TestFile
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class ResolveRetryStrategyTest {
    @Test
    fun itResolvesMaxAttemptsFromEnvironmentVariables() = runTest {
        val expectedMaxAttempts = 50

        val platform = TestPlatformProvider.of(
            env = mapOf(AwsSdkSetting.AwsMaxAttempts.envVar to expectedMaxAttempts.toString()),
        )

        val strategy = assertIs<StandardRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
    }

    @Test
    fun itResolvesMaxAttemptsFromSystemProperties() = runTest {
        val expectedMaxAttempts = 10
        val platform = TestPlatformProvider.of(
            props = mapOf(AwsSdkSetting.AwsMaxAttempts.sysProp to expectedMaxAttempts.toString()),
        )

        val strategy = assertIs<StandardRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
    }

    @Test
    fun itResolvesMaxAttemptsFromProfile() = runTest {
        val expectedMaxAttempts = 30

        val platform = TestPlatformProvider.of(
            env = mapOf("AWS_CONFIG_FILE" to "config"),
            fs = mapOf(
                "config" to TestFile(
                    """
                [default]
                max_attempts=$expectedMaxAttempts
                    """.trimIndent(),
                ),
            ),
        )

        val strategy = assertIs<StandardRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
    }

    @Test
    fun itThrowsOnInvalidMaxAttemptsValues() = runTest {
        val invalidMaxAttemptsValues = listOf(-91, -5, 0)

        for (invalidMaxAttempts in invalidMaxAttemptsValues) {
            val platform = TestPlatformProvider.of(
                env = mapOf(AwsSdkSetting.AwsMaxAttempts.envVar to invalidMaxAttempts.toString()),
            )

            assertFailsWith<ConfigurationException> { resolveRetryStrategy(platform) }
        }
    }

    @Test
    fun itThrowsOnUnsupportedRetryModes() = runTest {
        val retryMode = "unsupported-retry-mode"

        val platform = TestPlatformProvider.of(
            env = mapOf(AwsSdkSetting.AwsRetryMode.envVar to retryMode),
        )

        assertFailsWith<ClientException> { resolveRetryStrategy(platform) }
    }

    @Test
    fun itThrowsOnUnsupportedRetryModesFromProfile() = runTest {
        val expectedMaxAttempts = 30
        val retryMode = "unsupported-retry-mode"

        val platform = TestPlatformProvider.of(
            env = mapOf("AWS_CONFIG_FILE" to "config"),
            fs = mapOf(
                "config" to TestFile(
                    """
                [default]
                max_attempts=$expectedMaxAttempts
                retry_mode=$retryMode
                    """.trimIndent(),
                ),
            ),
        )

        assertFailsWith<ConfigurationException> { resolveRetryStrategy(platform) }
    }

    @Test
    fun itResolvesAdaptiveRetryStrategy() = runTest {
        val expectedMaxAttempts = StandardRetryStrategy.Config.DEFAULT_MAX_ATTEMPTS
        val adaptiveRetryMode = "adaptive"

        val platform = TestPlatformProvider.of(
            env = mapOf(AwsSdkSetting.AwsRetryMode.envVar to adaptiveRetryMode),
        )

        val strategy = assertIs<AdaptiveRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
    }

    @Test
    fun itResolvesNonLowercaseRetryModeValuesFromEnvironmentVariables() = runTest {
        val expectedMaxAttempts = 16
        val retryMode = "lEgACY"

        val platform = TestPlatformProvider.of(
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

        val platform = TestPlatformProvider.of(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
            ),
            fs = mapOf(
                "config" to TestFile(
                    """
                [default]
                max_attempts=$expectedMaxAttempts
                retry_mode=$retryMode
                    """.trimIndent(),
                ),
            ),
        )

        val strategy = assertIs<AdaptiveRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
    }

    @Test
    fun itResolvesMaxAttemptsAndRetryModeFromEnvironmentVariables() = runTest {
        val expectedMaxAttempts = 1
        val retryMode = "legacy"

        val platform = TestPlatformProvider.of(
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

        val platform = TestPlatformProvider.of(
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

        val platform = TestPlatformProvider.of(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
                AwsSdkSetting.AwsRetryMode.envVar to retryMode,
            ),
            fs = mapOf(
                "config" to TestFile(
                    """
                [default]
                max_attempts=$expectedMaxAttempts
                retry_mode=invalid-retry-mode-should-be-ignored
                    """.trimIndent(),
                ),
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

        val platform = TestPlatformProvider.of(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
                AwsSdkSetting.AwsRetryMode.envVar to retryMode,
                AwsSdkSetting.AwsMaxAttempts.envVar to expectedMaxAttempts.toString(),
            ),
            fs = mapOf(
                "config" to TestFile(
                    """
                [default]
                max_attempts=55
                retry_mode=invalid-retry-mode-should-be-ignored
                    """.trimIndent(),
                ),
            ),
        )

        val strategy = assertIs<StandardRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
    }

    @Test
    fun itUsesDefaultMaxAttemptsWhenNoneAreProvided() = runTest {
        val expectedMaxAttempts = StandardRetryStrategy.Config.DEFAULT_MAX_ATTEMPTS

        val platform = TestPlatformProvider.of() // no environment variables / system properties / profile

        val strategy = assertIs<StandardRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(expectedMaxAttempts, strategy.config.maxAttempts)
    }

    @Test
    fun itAppliesStandardDefaultsWhenNothingIsConfigured() = runTest {
        val platform = TestPlatformProvider.of() // no environment variables / system properties / profile

        val strategy = assertIs<StandardRetryStrategy>(resolveRetryStrategy(platform))
        val delayConfig = assertIs<ExponentialBackoffWithJitter.Config>(strategy.config.delayProvider.config)
        val bucketConfig = assertIs<StandardRetryTokenBucket.Config>(strategy.config.tokenBucket.config)

        assertEquals(STANDARD_INITIAL_DELAY, delayConfig.initialDelay)
        assertEquals(STANDARD_SCALE_FACTOR, delayConfig.scaleFactor)
        assertEquals(STANDARD_RETRY_COST, bucketConfig.retryCost)
        assertEquals(STANDARD_THROTTLING_RETRY_COST, bucketConfig.timeoutRetryCost)
        assertTrue(strategy.config.enableLongPollingBackoff)
    }

    @Test
    fun itAppliesStandardDefaultsToAdaptiveMode() = runTest {
        val platform = TestPlatformProvider.of(
            env = mapOf(AwsSdkSetting.AwsRetryMode.envVar to "adaptive"),
        )

        val strategy = assertIs<AdaptiveRetryStrategy>(resolveRetryStrategy(platform))
        val delayConfig = assertIs<ExponentialBackoffWithJitter.Config>(strategy.config.delayProvider.config)
        val bucketConfig = assertIs<StandardRetryTokenBucket.Config>(strategy.config.tokenBucket.config)

        assertEquals(STANDARD_INITIAL_DELAY, delayConfig.initialDelay)
        assertEquals(STANDARD_SCALE_FACTOR, delayConfig.scaleFactor)
        assertEquals(STANDARD_RETRY_COST, bucketConfig.retryCost)
        assertEquals(STANDARD_THROTTLING_RETRY_COST, bucketConfig.timeoutRetryCost)
        assertTrue(strategy.config.enableLongPollingBackoff)
    }

    @Test
    fun itChargesFullRetryCostForTransientRetriesByDefault() = runTest {
        // The token bucket must charge STANDARD_RETRY_COST (14), not STANDARD_THROTTLING_RETRY_COST (5), for
        // transient retries without any opt-in.
        val platform = TestPlatformProvider.of()

        val strategy = assertIs<StandardRetryStrategy>(resolveRetryStrategy(platform))
        assertEquals(500 / STANDARD_RETRY_COST, countTransientRetries(strategy))
    }

    @Test
    fun itAppliesLegacyDefaultsInLegacyMode() = runTest {
        val platform = TestPlatformProvider.of(
            env = mapOf(AwsSdkSetting.AwsRetryMode.envVar to "legacy"),
        )

        val strategy = assertIs<StandardRetryStrategy>(resolveRetryStrategy(platform))
        val delayConfig = assertIs<ExponentialBackoffWithJitter.Config>(strategy.config.delayProvider.config)
        val bucketConfig = assertIs<StandardRetryTokenBucket.Config>(strategy.config.tokenBucket.config)

        assertEquals(LEGACY_INITIAL_DELAY, delayConfig.initialDelay)
        assertEquals(LEGACY_SCALE_FACTOR, delayConfig.scaleFactor)
        assertEquals(LEGACY_RETRY_COST, bucketConfig.retryCost)
        assertEquals(LEGACY_TIMEOUT_RETRY_COST, bucketConfig.timeoutRetryCost)
        assertFalse(strategy.config.enableLongPollingBackoff)
        assertEquals(StandardRetryStrategy.Config.DEFAULT_MAX_ATTEMPTS, strategy.config.maxAttempts)

        // Legacy cost model: transient retries are charged the timeout cost (10)
        assertEquals(500 / LEGACY_TIMEOUT_RETRY_COST, countTransientRetries(strategy))
    }

    @Test
    fun itIgnoresServiceDefaultsInLegacyMode() = runTest {
        val platform = TestPlatformProvider.of()
        val config = resolveRetryConfig(platform, asyncLazy { loadAwsSharedConfig(platform).activeProfile })
        val legacy = config.copy(retryMode = RetryMode.LEGACY)

        val strategy = assertIs<StandardRetryStrategy>(buildRetryStrategy(legacy, defaultMaxAttempts = 4, defaultInitialDelay = 25.milliseconds))
        val delayConfig = assertIs<ExponentialBackoffWithJitter.Config>(strategy.config.delayProvider.config)

        assertEquals(StandardRetryStrategy.Config.DEFAULT_MAX_ATTEMPTS, strategy.config.maxAttempts)
        assertEquals(LEGACY_INITIAL_DELAY, delayConfig.initialDelay)
    }

    @Test
    fun itAppliesServiceDefaultsInStandardMode() = runTest {
        val strategy = assertIs<StandardRetryStrategy>(
            buildRetryStrategy(ResolvedRetryConfig(null, RetryMode.STANDARD), defaultMaxAttempts = 4, defaultInitialDelay = 25.milliseconds),
        )
        val delayConfig = assertIs<ExponentialBackoffWithJitter.Config>(strategy.config.delayProvider.config)

        assertEquals(4, strategy.config.maxAttempts)
        assertEquals(25.milliseconds, delayConfig.initialDelay)
    }

    /**
     * Schedule transient retries on the strategy's bucket (maxCapacity=500, circuit-breaker mode, initialTryCost=0)
     * until capacity is exhausted. The count reveals the per-retry cost.
     */
    private suspend fun countTransientRetries(strategy: StandardRetryStrategy): Int {
        var token = strategy.config.tokenBucket.acquireToken()
        var transientRetries = 0
        val result = runCatching {
            while (true) {
                token = token.scheduleRetry(RetryErrorType.Transient)
                transientRetries++
            }
        }
        assertIs<RetryCapacityExceededException>(result.exceptionOrNull())
        return transientRetries
    }
}
