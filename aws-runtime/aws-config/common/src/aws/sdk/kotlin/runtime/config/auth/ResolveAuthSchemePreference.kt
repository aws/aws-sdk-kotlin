/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.auth

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.authSchemePreference
import aws.smithy.kotlin.runtime.auth.AuthSchemeId
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider

@InternalSdkApi
public suspend fun resolveAuthSchemePreference(platform: PlatformProvider = PlatformProvider.System, profile: LazyAsyncValue<AwsProfile>): List<AuthSchemeId> {
    val content = AwsSdkSetting.AwsAuthSchemePreference.resolve(platform) ?: profile.get().authSchemePreference

    return content
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.map(::AuthSchemeId)
        ?.toList()
        ?: emptyList()
}
