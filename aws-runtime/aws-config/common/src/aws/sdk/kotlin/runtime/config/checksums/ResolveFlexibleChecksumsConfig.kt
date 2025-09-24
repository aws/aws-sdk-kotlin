/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.checksums

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.requestChecksumCalculation
import aws.sdk.kotlin.runtime.config.profile.responseChecksumValidation
import aws.smithy.kotlin.runtime.client.config.RequestHttpChecksumConfig
import aws.smithy.kotlin.runtime.client.config.ResponseHttpChecksumConfig
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Attempts to resolve requestChecksumCalculation from the specified sources.
 * @return requestChecksumCalculation setting if found, the default value if not.
 */
@InternalSdkApi
public suspend fun resolveRequestChecksumCalculation(
    platform: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile>,
): RequestHttpChecksumConfig =
    AwsSdkSetting.AwsRequestChecksumCalculation.resolve(platform) ?: profile.get().requestChecksumCalculation ?: RequestHttpChecksumConfig.WHEN_SUPPORTED

/**
 * Attempts to resolve responseChecksumValidation from the specified sources.
 * @return responseChecksumValidation setting if found, the default value if not.
 */
@InternalSdkApi
public suspend fun resolveResponseChecksumValidation(
    platform: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile>,
): ResponseHttpChecksumConfig =
    AwsSdkSetting.AwsResponseChecksumValidation.resolve(platform) ?: profile.get().responseChecksumValidation ?: ResponseHttpChecksumConfig.WHEN_SUPPORTED
