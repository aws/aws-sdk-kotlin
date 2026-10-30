/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.util.testAttributes
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsRefreshBehavior
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SystemPropertyCredentialsProviderTest {
    private fun provider(vararg vars: Pair<String, String>) = SystemPropertyCredentialsProvider((vars.toMap())::get)

    @Test
    fun testReadAllSystemProperties() = runTest {
        val provider = provider(
            AwsSdkSetting.AwsAccessKeyId.sysProp to "abc",
            AwsSdkSetting.AwsSecretAccessKey.sysProp to "def",
            AwsSdkSetting.AwsSessionToken.sysProp to "ghi",
        )
        assertEquals(
            provider.resolve(),
            Credentials(
                "abc",
                "def",
                "ghi",
                providerName = "SystemProperties",
                attributes = testAttributes(
                    AwsBusinessMetric.Credentials.CREDENTIALS_JVM_SYSTEM_PROPERTIES,
                ),
            ).withRefreshBehavior(CredentialsRefreshBehavior.NonRefreshable),
        )
    }

    @Test
    fun testReadAllSystemPropertiesExceptSessionToken() = runTest {
        val provider = provider(
            AwsSdkSetting.AwsAccessKeyId.sysProp to "abc",
            AwsSdkSetting.AwsSecretAccessKey.sysProp to "def",
        )
        assertEquals(
            provider.resolve(),
            Credentials(
                "abc",
                "def",
                null,
                providerName = "SystemProperties",
                attributes = testAttributes(
                    AwsBusinessMetric.Credentials.CREDENTIALS_JVM_SYSTEM_PROPERTIES,
                ),
            ).withRefreshBehavior(CredentialsRefreshBehavior.NonRefreshable),
        )
    }

    @Test
    fun testThrowsExceptionWhenMissingAccessKey() = runTest {
        assertFailsWith<ProviderConfigurationException> {
            provider(AwsSdkSetting.AwsSecretAccessKey.sysProp to "def").resolve()
        }.message.shouldContain("Missing value for system property `aws.accessKeyId`")
    }

    @Test
    fun testThrowsExceptionWhenMissingSecretKey() = runTest {
        assertFailsWith<ProviderConfigurationException> {
            provider(AwsSdkSetting.AwsAccessKeyId.sysProp to "abc").resolve()
        }.message.shouldContain("Missing value for system property `aws.secretAccessKey`")
    }

    @Test
    fun testIgnoresEmptyAccessKey() = runTest {
        assertFailsWith<ProviderConfigurationException> {
            provider(
                AwsSdkSetting.AwsAccessKeyId.sysProp to "",
                AwsSdkSetting.AwsSecretAccessKey.sysProp to "abc",
            ).resolve()
        }.message.shouldContain("Missing value for system property `aws.accessKeyId`")
    }

    @Test
    fun testIgnoresEmptySecretKey() = runTest {
        assertFailsWith<ProviderConfigurationException> {
            provider(
                AwsSdkSetting.AwsAccessKeyId.sysProp to "abc",
                AwsSdkSetting.AwsSecretAccessKey.sysProp to "",
            ).resolve()
        }.message.shouldContain("Missing value for system property `aws.secretAccessKey`")
    }
}
