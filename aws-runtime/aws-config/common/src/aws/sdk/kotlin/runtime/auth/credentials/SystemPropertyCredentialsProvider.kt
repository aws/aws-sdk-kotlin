/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.telemetry.logging.trace
import aws.smithy.kotlin.runtime.util.Attributes
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlin.coroutines.coroutineContext

private const val PROVIDER_NAME = "SystemProperties"

private val ACCESS_KEY_ID = AwsSdkSetting.AwsAccessKeyId.sysProp
private val SECRET_ACCESS_KEY = AwsSdkSetting.AwsSecretAccessKey.sysProp
private val SESSION_TOKEN = AwsSdkSetting.AwsSessionToken.sysProp

/**
 * A [CredentialsProvider] which reads `aws.accessKeyId`, `aws.secretAccessKey`, and `aws.sessionToken` from system properties.
 */
public class SystemPropertyCredentialsProvider
public constructor(private val getProperty: (String) -> String? = PlatformProvider.System::getProperty) : CredentialsProvider {

    private fun requireProperty(variable: String): String =
        getProperty(variable) ?: throw ProviderConfigurationException("Missing value for system property `$variable`")

    override suspend fun resolve(attributes: Attributes): Credentials {
        coroutineContext.trace<SystemPropertyCredentialsProvider> {
            "Attempting to load credentials from system properties $ACCESS_KEY_ID/$SECRET_ACCESS_KEY/$SESSION_TOKEN"
        }
        return Credentials(
            accessKeyId = requireProperty(ACCESS_KEY_ID),
            secretAccessKey = requireProperty(SECRET_ACCESS_KEY),
            sessionToken = getProperty(SESSION_TOKEN),
            providerName = PROVIDER_NAME,
        )
    }
}
