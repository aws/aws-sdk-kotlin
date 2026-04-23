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
import aws.smithy.kotlin.runtime.retries.newRetriesEnabled
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy

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
    val useNewRetries = AwsSdkSetting.AwsNewRetries.resolve(platformProvider) ?: newRetriesEnabled()
    val maxAttempts = AwsSdkSetting.AwsMaxAttempts.resolve(platformProvider)
        ?: profile.get().maxAttempts

    val retryMode = AwsSdkSetting.AwsRetryMode.resolve(platformProvider)
        ?: profile.get().retryMode
        ?: RetryMode.STANDARD

    val factory = when (retryMode) {
        RetryMode.STANDARD, RetryMode.LEGACY -> StandardRetryStrategy
        RetryMode.ADAPTIVE -> AdaptiveRetryStrategy
    }

    return factory {
        maxAttempts?.let {
            if (it < 1) {
                throw ConfigurationException("max attempts was $it, but should be at least 1")
            }
            this.maxAttempts = it
        }
        serviceName?.let { this.serviceName = it }
        if (useNewRetries) enableStandardRetryDefaults()
    }
}
