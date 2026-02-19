/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.useragent

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.sdkUserAgentAppId
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Attempts to resolve user agent from specified sources.
 * @return The user agent app id if found, null if not
 */
@InternalSdkApi
public suspend fun resolveUserAgentAppId(platform: PlatformProvider = PlatformProvider.System, profile: LazyAsyncValue<AwsProfile>): String? = AwsSdkSetting.AwsAppId.resolve(platform) ?: profile.get().sdkUserAgentAppId
