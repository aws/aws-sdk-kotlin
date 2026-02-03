/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.imds

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.loadAwsSharedConfig
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy

/**
 * Attempts to resolve a named EC2 instance profile to use which allows bypassing auto-discovery
 */
@InternalSdkApi
public suspend fun resolveEc2InstanceProfileName(
    provider: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile> = asyncLazy { loadAwsSharedConfig(provider).activeProfile },
): String? = AwsSdkSetting.AwsEc2InstanceProfileName.resolve(provider)

/**
 * Attempts to resolve the flag which disables the use of IMDS for credentials
 */
public suspend fun resolveDisableEc2Metadata(
    provider: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile> = asyncLazy { loadAwsSharedConfig(provider).activeProfile },
): Boolean? = AwsSdkSetting.AwsEc2MetadataDisabled.resolve(provider)
