/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.simpleClassName
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.telemetry.logging.trace
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlin.coroutines.coroutineContext

private const val PROVIDER_NAME = "Environment"

private val ACCESS_KEY_ID = AwsSdkSetting.AwsAccessKeyId.envVar
private val SECRET_ACCESS_KEY = AwsSdkSetting.AwsSecretAccessKey.envVar
private val SESSION_TOKEN = AwsSdkSetting.AwsSessionToken.envVar
private val ACCOUNT_ID = AwsSdkSetting.AwsAccountId.envVar

/**
 * A [CredentialsProvider] which reads from the `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and `AWS_SESSION_TOKEN` environment variables.
 * Note that the `AWS_SESSION_TOKEN` is only necessary if the credentials are temporary.
 */
public class EnvironmentCredentialsProvider(
    public val getEnv: (String) -> String? = PlatformProvider.System::getenv,
) : CredentialsProvider {

    private fun requireEnv(variable: String): String = getEnv(variable)?.takeIf(String::isNotBlank) ?: throw ProviderConfigurationException("Missing value for environment variable `$variable`")

    override suspend fun resolve(attributes: Attributes): Credentials {
        coroutineContext.trace<EnvironmentCredentialsProvider> {
            "Attempting to load credentials from env vars $ACCESS_KEY_ID/$SECRET_ACCESS_KEY/$SESSION_TOKEN"
        }

        return credentials(
            accessKeyId = requireEnv(ACCESS_KEY_ID),
            secretAccessKey = requireEnv(SECRET_ACCESS_KEY),
            sessionToken = getEnv(SESSION_TOKEN),
            providerName = PROVIDER_NAME,
            accountId = getEnv(ACCOUNT_ID),
        ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_ENV_VARS)
    }

    override fun toString(): String = this.simpleClassName
}
