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
import aws.smithy.kotlin.runtime.CoreSettings
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

/**
 * Resolved retry configuration sourced from environment variables and profile settings.
 */
internal data class ResolvedRetryConfig(
    val maxAttempts: Int?,
    val retryMode: RetryMode,
    val useNewRetries: Boolean,
)

/**
 * Resolve the retry configuration (max attempts, retry mode, and new retries flag) from environment variables
 * and profile settings.
 */
internal suspend fun resolveRetryConfig(
    platformProvider: PlatformProvider,
    profile: LazyAsyncValue<AwsProfile>,
): ResolvedRetryConfig {
    val useNewRetries = AwsSdkSetting.AwsNewRetries.resolve(platformProvider)
        ?: CoreSettings.resolveNewRetriesEnabled(platformProvider)

    val maxAttempts = AwsSdkSetting.AwsMaxAttempts.resolve(platformProvider)
        ?: profile.get().maxAttempts

    maxAttempts?.let {
        if (it < 1) throw ConfigurationException("max attempts was $it, but should be at least 1")
    }

    val retryMode = AwsSdkSetting.AwsRetryMode.resolve(platformProvider)
        ?: profile.get().retryMode
        ?: RetryMode.STANDARD

    return ResolvedRetryConfig(maxAttempts, retryMode, useNewRetries)
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
    val factory = when (config.retryMode) {
        RetryMode.STANDARD, RetryMode.LEGACY -> StandardRetryStrategy
        RetryMode.ADAPTIVE -> AdaptiveRetryStrategy
    }

    return factory {
        configureRetryDefaults(
            configuredMaxAttempts = config.maxAttempts,
            useNewRetries = config.useNewRetries,
            defaultMaxAttempts = defaultMaxAttempts,
            defaultInitialDelay = defaultInitialDelay,
        )
    }
}

/**
 * Configures the retry strategy builder with the resolved max attempts and, when new retries are enabled,
 * all standard defaults as defined in the New Retry Behavior.
 */
internal fun StandardRetryStrategy.Config.Builder.configureRetryDefaults(
    configuredMaxAttempts: Int? = null,
    useNewRetries: Boolean = false,
    defaultMaxAttempts: Int? = null,
    defaultInitialDelay: Duration? = null,
) {
    if (!useNewRetries) {
        configuredMaxAttempts?.let { maxAttempts = it }
        return
    }

    delayProvider {
        initialDelay = defaultInitialDelay ?: STANDARD_INITIAL_DELAY
        scaleFactor = STANDARD_SCALE_FACTOR
    }

    tokenBucket {
        retryCost = STANDARD_RETRY_COST
        timeoutRetryCost = STANDARD_THROTTLING_RETRY_COST
    }

    maxAttempts = configuredMaxAttempts
        ?: defaultMaxAttempts
        ?: StandardRetryStrategy.Config.DEFAULT_MAX_ATTEMPTS
}
