/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JavaSystemPropertiesFallbackTest {
    @Test
    fun testUserAgentAppIdKotlinPropertyTakesPrecedence() {
        val platform = TestPlatformProvider(
            props = mapOf(
                "aws.userAgentAppId" to "kotlin-value",
                "sdk.ua.appId" to "java-value",
            ),
            env = mapOf("AWS_SDK_UA_APP_ID" to "env-value"),
        )

        val result = AwsSdkSetting.AwsAppId.resolve(platform)
        assertEquals("kotlin-value", result)
    }

    @Ignore
    @Test
    fun testUserAgentAppIdJavaFallback() {
        val platform = TestPlatformProvider(
            props = mapOf("sdk.ua.appId" to "java-value"),
            env = mapOf("AWS_SDK_UA_APP_ID" to "env-value"),
        )

        val result = AwsSdkSetting.AwsAppId.resolve(platform)
        assertEquals("java-value", result)
    }

    @Test
    fun testUserAgentAppIdEnvironmentFallback() {
        val platform = TestPlatformProvider(
            env = mapOf("AWS_SDK_UA_APP_ID" to "env-value"),
        )

        val result = AwsSdkSetting.AwsAppId.resolve(platform)
        assertEquals("env-value", result)
    }

    @Test
    fun testUserAgentAppIdNullWhenNothingSet() {
        val platform = TestPlatformProvider()

        val result = AwsSdkSetting.AwsAppId.resolve(platform)
        assertNull(result)
    }

    @Test
    fun testSigV4aSigningRegionSetKotlinPropertyTakesPrecedence() {
        val platform = TestPlatformProvider(
            props = mapOf(
                "aws.sigV4aSigningRegionSet" to "kotlin-regions",
                "aws.sigv4a.signing.region.set" to "java-regions",
            ),
            env = mapOf("AWS_SIGV4A_SIGNING_REGION_SET" to "env-regions"),
        )

        val result = AwsSdkSetting.AwsSigV4aSigningRegionSet.resolve(platform)
        assertEquals("kotlin-regions", result)
    }

    @Ignore
    @Test
    fun testSigV4aSigningRegionSetJavaFallback() {
        val platform = TestPlatformProvider(
            props = mapOf("aws.sigv4a.signing.region.set" to "java-regions"),
            env = mapOf("AWS_SIGV4A_SIGNING_REGION_SET" to "env-regions"),
        )

        val result = AwsSdkSetting.AwsSigV4aSigningRegionSet.resolve(platform)
        assertEquals("java-regions", result)
    }

    @Test
    fun testSigV4aSigningRegionSetEnvironmentFallback() {
        val platform = TestPlatformProvider(
            env = mapOf("AWS_SIGV4A_SIGNING_REGION_SET" to "env-regions"),
        )

        val result = AwsSdkSetting.AwsSigV4aSigningRegionSet.resolve(platform)
        assertEquals("env-regions", result)
    }

    @Test
    fun testNewRetriesSystemPropertyTakesPrecedence() {
        val platform = TestPlatformProvider(
            props = mapOf("aws.newRetries2026" to "true"),
            env = mapOf("AWS_NEW_RETRIES_2026" to "false"),
        )

        val result = AwsSdkSetting.AwsNewRetries.resolve(platform)
        assertTrue(result!!)
    }

    @Test
    fun testNewRetriesEnvironmentFallback() {
        val platform = TestPlatformProvider(
            env = mapOf("AWS_NEW_RETRIES_2026" to "true"),
        )

        val result = AwsSdkSetting.AwsNewRetries.resolve(platform)
        assertTrue(result!!)
    }

    @Test
    fun testNewRetriesNullWhenNothingSet() {
        val platform = TestPlatformProvider()

        val result = AwsSdkSetting.AwsNewRetries.resolve(platform)
        assertNull(result)
    }
}
