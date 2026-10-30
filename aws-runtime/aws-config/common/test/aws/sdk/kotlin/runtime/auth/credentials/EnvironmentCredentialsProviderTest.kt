/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsRefreshBehavior
import aws.smithy.kotlin.runtime.collections.attributesOf
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EnvironmentCredentialsProviderTest {
    private fun provider(vararg vars: Pair<String, String>) = EnvironmentCredentialsProvider((vars.toMap())::get)

    @Test
    fun testReadFromEnvironmentIncludingSessionToken() = runTest {
        val provider = provider(
            AwsSdkSetting.AwsAccessKeyId.envVar to "abc",
            AwsSdkSetting.AwsSecretAccessKey.envVar to "def",
            AwsSdkSetting.AwsSessionToken.envVar to "ghi",
        )
        assertEquals(
            provider.resolve(),
            Credentials(
                "abc",
                "def",
                "ghi",
                providerName = "Environment",
            ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_ENV_VARS)
                .withRefreshBehavior(CredentialsRefreshBehavior.NonRefreshable),
        )
    }

    @Test
    fun testReadFromEnvironmentExcludingSessionToken() = runTest {
        val provider = provider(
            AwsSdkSetting.AwsAccessKeyId.envVar to "abc",
            AwsSdkSetting.AwsSecretAccessKey.envVar to "def",
        )
        assertEquals(
            provider.resolve(),
            Credentials(
                "abc",
                "def",
                null,
                providerName = "Environment",
            ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_ENV_VARS)
                .withRefreshBehavior(CredentialsRefreshBehavior.NonRefreshable),
        )
    }

    @Test
    fun testThrowsWhenMissingAccessKey() = runTest {
        assertFailsWith<ProviderConfigurationException> {
            provider(AwsSdkSetting.AwsSecretAccessKey.envVar to "def").resolve()
        }.message.shouldContain("Missing value for environment variable `AWS_ACCESS_KEY_ID`")
    }

    @Test
    fun testThrowsWhenMissingSecretKey() = runTest {
        assertFailsWith<ProviderConfigurationException> {
            provider(AwsSdkSetting.AwsAccessKeyId.envVar to "abc").resolve()
        }.message.shouldContain("Missing value for environment variable `AWS_SECRET_ACCESS_KEY`")
    }

    @Test
    fun testIgnoresEmptyAccessKey() = runTest {
        assertFailsWith<ProviderConfigurationException> {
            provider(
                AwsSdkSetting.AwsAccessKeyId.envVar to "",
                AwsSdkSetting.AwsSecretAccessKey.envVar to "abc",
            ).resolve()
        }.message.shouldContain("Missing value for environment variable `AWS_ACCESS_KEY_ID`")
    }

    @Test
    fun testIgnoresEmptySecretKey() = runTest {
        assertFailsWith<ProviderConfigurationException> {
            provider(
                AwsSdkSetting.AwsAccessKeyId.envVar to "abc",
                AwsSdkSetting.AwsSecretAccessKey.envVar to "",
            ).resolve()
        }.message.shouldContain("Missing value for environment variable `AWS_SECRET_ACCESS_KEY`")
    }

    @Test
    fun testAccountIdIsResolved() = runTest {
        val provider = provider(
            AwsSdkSetting.AwsAccessKeyId.envVar to "abc",
            AwsSdkSetting.AwsSecretAccessKey.envVar to "def",
            AwsSdkSetting.AwsAccountId.envVar to "12345",
        )

        val actual = provider.resolve()
        val expected = Credentials(
            "abc",
            "def",
            providerName = "Environment",
            attributes = attributesOf { AwsClientOption.AccountId to "12345" },
        ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_ENV_VARS)
            .withRefreshBehavior(CredentialsRefreshBehavior.NonRefreshable)
        assertEquals(expected, actual)
    }
}
