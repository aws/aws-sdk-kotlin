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

// Standard retry defaults (New Retry Behavior)
private val STANDARD_INITIAL_DELAY = 50.milliseconds
private const val STANDARD_SCALE_FACTOR = 2.0
private const val STANDARD_RETRY_COST = 14
private const val STANDARD_THROTTLING_RETRY_COST = 5

// DynamoDB / DynamoDB Streams overrides
private val DYNAMODB_SERVICES = setOf("dynamodb", "dynamodb streams")
private val DYNAMODB_INITIAL_DELAY = 25.milliseconds
private const val DYNAMODB_DEFAULT_MAX_ATTEMPTS = 4

/**
 * Attempt to resolve the retry strategy used to make requests by fetching the max attempts and retry mode.
 * If `AWS_NEW_RETRIES_2026` is set to `true`, the standard retry strategy behavior is enabled.
 * Falls back to smithy-kotlin's `SMITHY_NEW_RETRIES_2026`.
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
 * all standard defaults as defined in the New Retry Behavior.
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

    val isDynamoDb = serviceName?.lowercase() in DYNAMODB_SERVICES

    delayProvider {
        initialDelay = if (isDynamoDb) DYNAMODB_INITIAL_DELAY else STANDARD_INITIAL_DELAY
        scaleFactor = STANDARD_SCALE_FACTOR
    }

    tokenBucket {
        retryCost = STANDARD_RETRY_COST
        timeoutRetryCost = STANDARD_THROTTLING_RETRY_COST
    }

    maxAttempts = configuredMaxAttempts
        ?: if (isDynamoDb) {
            DYNAMODB_DEFAULT_MAX_ATTEMPTS
        } else {
            StandardRetryStrategy.Config.DEFAULT_MAX_ATTEMPTS
        }
}
