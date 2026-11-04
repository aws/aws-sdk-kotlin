/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.retries

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.loadAwsSharedConfig
import aws.sdk.kotlin.runtime.config.profile.maxAttempts
import aws.sdk.kotlin.runtime.config.profile.retryMode
import aws.smithy.kotlin.runtime.client.config.RetryMode
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.retries.AdaptiveRetryStrategy
import aws.smithy.kotlin.runtime.retries.RetryStrategy
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// Standard retry defaults (New Retry Behavior)
internal val STANDARD_INITIAL_DELAY = 50.milliseconds
internal const val STANDARD_SCALE_FACTOR = 2.0
internal const val STANDARD_RETRY_COST = 14
internal const val STANDARD_THROTTLING_RETRY_COST = 5

// Legacy retry defaults, used only when the legacy retry mode is configured
internal val LEGACY_INITIAL_DELAY = 10.milliseconds
internal const val LEGACY_SCALE_FACTOR = 1.5
internal const val LEGACY_RETRY_COST = 5
internal const val LEGACY_TIMEOUT_RETRY_COST = 10

/**
 * Resolved retry configuration sourced from environment variables and profile settings.
 */
internal data class ResolvedRetryConfig(
    val maxAttempts: Int?,
    val retryMode: RetryMode,
)

/**
 * Resolve the retry configuration (max attempts and retry mode) from environment variables and profile settings.
 */
internal suspend fun resolveRetryConfig(
    platformProvider: PlatformProvider,
    profile: LazyAsyncValue<AwsProfile>,
): ResolvedRetryConfig {
    val maxAttempts = AwsSdkSetting.AwsMaxAttempts.resolve(platformProvider)
        ?: profile.get().maxAttempts

    maxAttempts?.let {
        if (it < 1) throw ConfigurationException("max attempts was $it, but should be at least 1")
    }

    val retryMode = AwsSdkSetting.AwsRetryMode.resolve(platformProvider)
        ?: profile.get().retryMode
        ?: RetryMode.STANDARD

    return ResolvedRetryConfig(maxAttempts, retryMode)
}

/**
 * Attempt to resolve the retry strategy from environment variables and profile settings.
 */
@InternalSdkApi
public suspend fun resolveRetryStrategy(
    platformProvider: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile> = asyncLazy { loadAwsSharedConfig(platformProvider).activeProfile },
): RetryStrategy {
    val config = resolveRetryConfig(platformProvider, profile)
    return buildRetryStrategy(config)
}

internal fun buildRetryStrategy(config: ResolvedRetryConfig): RetryStrategy = buildRetryStrategy(config, defaultMaxAttempts = null, defaultInitialDelay = null)

internal fun buildRetryStrategy(
    config: ResolvedRetryConfig,
    defaultMaxAttempts: Int?,
    defaultInitialDelay: Duration?,
): RetryStrategy {
    val configuredMaxAttempts = config.maxAttempts

    return when (config.retryMode) {
        RetryMode.STANDARD -> StandardRetryStrategy {
            configureRetryDefaults(configuredMaxAttempts, defaultMaxAttempts, defaultInitialDelay)
        }
        RetryMode.ADAPTIVE -> AdaptiveRetryStrategy {
            configureRetryDefaults(configuredMaxAttempts, defaultMaxAttempts, defaultInitialDelay)
        }
        RetryMode.LEGACY -> StandardRetryStrategy { configureLegacyRetryDefaults(configuredMaxAttempts) }
    }
}

/**
 * Configures the retry strategy builder with the resolved max attempts and the standard retry behavior: a 50 ms base
 * delay (or the service's default), a 2.0 scale factor, the standard token costs, and long-polling backoff.
 */
internal fun StandardRetryStrategy.Config.Builder.configureRetryDefaults(
    configuredMaxAttempts: Int? = null,
    defaultMaxAttempts: Int? = null,
    defaultInitialDelay: Duration? = null,
) {
    enableLongPollingBackoff = true

    delayProvider {
        initialDelay = defaultInitialDelay ?: STANDARD_INITIAL_DELAY
        scaleFactor = STANDARD_SCALE_FACTOR
    }

    tokenBucket {
        this.useNewRetries = true
        retryCost = STANDARD_RETRY_COST
        timeoutRetryCost = STANDARD_THROTTLING_RETRY_COST
    }

    maxAttempts = configuredMaxAttempts
        ?: defaultMaxAttempts
        ?: StandardRetryStrategy.Config.DEFAULT_MAX_ATTEMPTS
}

/**
 * Configures the retry strategy builder with the legacy retry behavior: a 10 ms base delay, a 1.5 scale factor,
 * the legacy token costs, and no long-polling backoff. Service-specific defaults are not applied.
 */
internal fun StandardRetryStrategy.Config.Builder.configureLegacyRetryDefaults(
    configuredMaxAttempts: Int? = null,
) {
    enableLongPollingBackoff = false

    delayProvider {
        initialDelay = LEGACY_INITIAL_DELAY
        scaleFactor = LEGACY_SCALE_FACTOR
    }

    tokenBucket {
        this.useNewRetries = false
        retryCost = LEGACY_RETRY_COST
        timeoutRetryCost = LEGACY_TIMEOUT_RETRY_COST
    }

    maxAttempts = configuredMaxAttempts ?: StandardRetryStrategy.Config.DEFAULT_MAX_ATTEMPTS
}
