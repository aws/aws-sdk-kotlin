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
import aws.smithy.kotlin.runtime.retries.CoreSettings
import aws.smithy.kotlin.runtime.retries.RetryStrategy
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy
import kotlin.time.Duration.Companion.milliseconds

/**
 * Services that use a shorter initial backoff delay (25ms instead of the standard 50ms).
 */
private val SHORT_BACKOFF_SERVICES = setOf("dynamodb", "dynamodb streams")

/**
 * The initial backoff delay for DynamoDB and DynamoDB Streams (SEP Retry Behavior 2.1: x = 0.025).
 */
private val DYNAMODB_INITIAL_DELAY = 25.milliseconds

/**
 * Services that use an increased default max attempts (4 instead of the standard 3).
 */
private val INCREASED_MAX_ATTEMPTS_SERVICES = SHORT_BACKOFF_SERVICES

/**
 * The default max attempts for DynamoDB and DynamoDB Streams.
 */
private const val DYNAMODB_DEFAULT_MAX_ATTEMPTS = 4

/**
 * Attempt to resolve the retry strategy used to make requests by fetching the max attempts and retry mode.
 * If `AWS_NEW_RETRIES_2026` (or `aws.newRetries2026` system property) is set to `true`, the standard retry
 * strategy behavior is enabled. Falls back to smithy-kotlin's `SMITHY_NEW_RETRIES_2026` / `smithy.newRetries2026`.
 */
@InternalSdkApi
public suspend fun resolveRetryStrategy(
    platformProvider: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile> = asyncLazy { loadAwsSharedConfig(platformProvider).activeProfile },
    serviceName: String? = null,
): RetryStrategy {
    val useNewRetries = AwsSdkSetting.AwsNewRetries.resolve(platformProvider) ?: CoreSettings.NewRetriesEnabled
    val maxAttempts = AwsSdkSetting.AwsMaxAttempts.resolve(platformProvider)
        ?: profile.get().maxAttempts

    maxAttempts?.let {
        if (it < 1) throw ConfigurationException("max attempts was $it, but should be at least 1")
    }

    val retryMode = AwsSdkSetting.AwsRetryMode.resolve(platformProvider)
        ?: profile.get().retryMode
        ?: RetryMode.STANDARD

    val factory = when (retryMode) {
        RetryMode.STANDARD, RetryMode.LEGACY -> StandardRetryStrategy
        RetryMode.ADAPTIVE -> AdaptiveRetryStrategy
    }

    return factory {
        configureRetryDefaults(serviceName, maxAttempts, useNewRetries)
    }
}

/**
 * Configures the retry strategy builder with the resolved max attempts and, when new retries are enabled,
 * AWS service-specific defaults as defined in the Retry Behavior 2.1 SEP.
 *
 * When [useNewRetries] is `true` and [serviceName] identifies DynamoDB or DynamoDB Streams:
 * - Sets `initialDelay` to 25ms (instead of the standard 50ms)
 * - Sets `maxAttempts` to 4 (instead of the standard 3), unless [configuredMaxAttempts] is provided
 */
@InternalSdkApi
public fun StandardRetryStrategy.Config.Builder.configureRetryDefaults(
    serviceName: String? = null,
    configuredMaxAttempts: Int? = null,
    useNewRetries: Boolean = false,
) {
    if (!useNewRetries) {
        configuredMaxAttempts?.let { maxAttempts = it }
        return
    }

    val normalizedName = serviceName?.lowercase()

    if (normalizedName in SHORT_BACKOFF_SERVICES) {
        delayProvider {
            initialDelay = DYNAMODB_INITIAL_DELAY
        }
    }

    maxAttempts = configuredMaxAttempts
        ?: if (normalizedName in INCREASED_MAX_ATTEMPTS_SERVICES) DYNAMODB_DEFAULT_MAX_ATTEMPTS
        else StandardRetryStrategy.Config.DEFAULT_MAX_ATTEMPTS
}
