/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.endpoints

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.*
import aws.sdk.kotlin.runtime.config.resolveEndpointUrl
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy

/**
 * Attempts to resolve the enabled state of FIPS endpoints from the environment.
 */
@InternalSdkApi
public suspend fun resolveUseFips(
    provider: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile> = asyncLazy { loadAwsSharedConfig(provider).activeProfile },
): Boolean? = AwsSdkSetting.AwsUseFipsEndpoint.resolve(provider)
    ?: profile.get().useFips

/**
 * Attempts to resolve the enabled state of dual-stack endpoints from the environment.
 */
@InternalSdkApi
public suspend fun resolveUseDualStack(
    provider: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile> = asyncLazy { loadAwsSharedConfig(provider).activeProfile },
): Boolean? = AwsSdkSetting.AwsUseDualStackEndpoint.resolve(provider)
    ?: profile.get().useDualStack

/**
 * Attempts to find the configured endpoint URL for a specific service.
 *
 * We look at the following sources in-order:
 *
 * 1. The value provided by a service-specific environment setting:
 *    1. The environment variable `AWS_ENDPOINT_URL_${SNAKE_CASE_SERVICE_ID}`.
 *    2. The JVM system property `aws.endpointUrl${JavaSDKClientPrefix}`.
 * 2. The value provided by the global endpoint environment setting:
 *    1. The environment variable `AWS_ENDPOINT_URL`.
 *    2. The JVM system property `aws.endpointUrl`.
 * 3. The value provided by a service-specific parameter from a services definition section in the shared configuration
 *    file (`${snake_case_service_id}.endpoint_url`).
 * 4. The value provided by the global parameter from a profile in the shared configuration file (`endpoint_url`).
 *
 * Endpoint URL settings can be disabled globally through either the environment or shared config, in which case the
 * above list of sources is ignored.
 */
@InternalSdkApi
public suspend fun resolveEndpointUrl(
    sharedConfig: LazyAsyncValue<AwsSharedConfig>,
    sysPropSuffix: String,
    envSuffix: String,
    sharedConfigKey: String,
    provider: PlatformProvider = PlatformProvider.System,
): Url? {
    if (resolveIgnoreEndpointUrls(provider, sharedConfig)) {
        return null // the "disable" directive overrides ALL sources, regardless of where it comes from itself
    }

    return AwsSdkSetting.resolveEndpointUrl(provider, sysPropSuffix, envSuffix)
        ?: sharedConfig.get().resolveEndpointUrl(sharedConfigKey)
}

private suspend fun resolveIgnoreEndpointUrls(
    provider: PlatformProvider,
    sharedConfig: LazyAsyncValue<AwsSharedConfig>,
): Boolean = AwsSdkSetting.AwsIgnoreEndpointUrls.resolve(provider)
    ?: sharedConfig.get().activeProfile.ignoreEndpointUrls
    ?: false

/**
 * Resolve the [AccountIdEndpointMode] from the environment.
 */
@InternalSdkApi
public suspend fun resolveAccountIdEndpointMode(
    provider: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile> = asyncLazy { loadAwsSharedConfig(provider).activeProfile },
): AccountIdEndpointMode = AwsSdkSetting.AwsAccountIdEndpointMode.resolve(provider)
    ?: profile.get().accountIdEndpointMode ?: AccountIdEndpointMode.PREFERRED

/**
 * Resolve the account ID from the given [attributes] while respecting the given [AccountIdEndpointMode]
 */
@InternalSdkApi
public fun resolveAccountId(endpointMode: AccountIdEndpointMode, attributes: Attributes): String? = when (endpointMode) {
    AccountIdEndpointMode.PREFERRED -> attributes.getOrNull(AwsClientOption.AccountId)
    AccountIdEndpointMode.DISABLED -> null
    AccountIdEndpointMode.REQUIRED -> attributes.getOrNull(AwsClientOption.AccountId) ?: throw ConfigurationException("AccountIdEndpointMode is set to required but no AWS account ID found")
}

/**
 * Resolve the endpoint discovery mode
 */
@InternalSdkApi
public suspend fun resolveEndpointDiscoveryEnabled(
    provider: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile> = asyncLazy { loadAwsSharedConfig(provider).activeProfile },
    serviceRequiresEpDiscovery: Boolean,
): Boolean = AwsSdkSetting.AwsEndpointDiscoveryEnabled.resolve(provider)
    ?: profile.get().endpointDiscoveryEnabled
    ?: serviceRequiresEpDiscovery
