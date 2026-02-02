/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.http

import aws.sdk.kotlin.runtime.http.operation.ConfigMetadata
import aws.sdk.kotlin.runtime.http.operation.CustomUserAgentMetadata
import aws.sdk.kotlin.runtime.http.operation.FeatureMetadata
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import kotlin.test.assertEquals

class AwsUserAgentMetadataTest {

    @Test
    fun testUserAgent() {
        val provider = TestPlatformProvider()
        val ua = loadAwsUserAgentMetadataFromEnvironment(provider, ApiMetadata("Test Service", "1.2.3"))
        assertEquals("aws-sdk-kotlin/1.2.3", ua.userAgent)
    }

    @Test
    fun testXAmzUserAgent() {
        val apiMeta = ApiMetadata("Test Service", "1.2.3")
        val sdkMeta = SdkMetadata("kotlin", apiMeta.version)
        val osMetadata = OsMetadata(OsFamily.Linux, "ubuntu-20.04")
        val langMeta = LanguageMetadata("1.4.31", mapOf("jvmVersion" to "1.11"))
        val execEnvMeta = ExecutionEnvMetadata("lambda")
        val frameworkMeta = FrameworkMetadata("amplify", "1.2.3")
        val appId = "Foo Service #1.2.3"
        val custom = CustomUserAgentMetadata(
            extras = mapOf("foo" to "bar", "internal" to "true"),
            typedExtras = listOf(
                ConfigMetadata("retry-mode", "standard"),
                ConfigMetadata("http-engine", "okhttp"),
                FeatureMetadata("paginator"),
                FeatureMetadata("ddb-hll", "4.5.6"),
            ),
        )
        val ua = AwsUserAgentMetadata(sdkMeta, apiMeta, osMetadata, langMeta, execEnvMeta, frameworkMeta, appId, custom)
        val expected = listOf(
            "aws-sdk-kotlin/1.2.3",
            "md/internal",
            "ua/2.1",
            "api/test-service#1.2.3",
            "os/linux#ubuntu-20.04",
            "lang/kotlin#1.4.31",
            "md/jvmVersion#1.11",
            "exec-env/lambda",
            "cfg/retry-mode#standard",
            "cfg/http-engine#okhttp",
            "app/Foo_Service_#1.2.3",
            "ft/paginator",
            "ft/ddb-hll#4.5.6",
            "lib/amplify#1.2.3",
            "md/foo#bar",
        ).joinToString(separator = " ")
        assertEquals(expected, ua.xAmzUserAgent)
    }

    data class EnvironmentTest(val provider: TestPlatformProvider, val expected: String)

    @Test
    fun testFrameworkFromEnvironment() {
        val testEnvironments = listOf(
            EnvironmentTest(
                TestPlatformProvider(
                    env = mapOf(FRAMEWORK_METADATA_ENV to "amplify:1.2.3"),
                ),
                "lib/amplify#1.2.3",
            ),
            EnvironmentTest(
                TestPlatformProvider(
                    env = mapOf(FRAMEWORK_METADATA_ENV to "amplify:1.2.3"),
                    props = mapOf(FRAMEWORK_METADATA_PROP to "amplify:4.5.6"),
                ),
                "lib/amplify#4.5.6",
            ),
        )
        testEnvironments.forEach { test ->
            val actual = FrameworkMetadata.fromEnvironment(test.provider).toString()
            assertEquals(test.expected, actual)
        }
    }

    @Test
    fun testAppIdFromEnvironment() {
        val testEnvironments = listOf(
            EnvironmentTest(
                TestPlatformProvider(
                    env = mapOf(AWS_APP_ID_ENV to "app-id-1"),
                ),
                "app/app-id-1",
            ),
            EnvironmentTest(
                TestPlatformProvider(
                    env = mapOf(AWS_APP_ID_ENV to "app-id-1"),
                    props = mapOf(AWS_APP_ID_PROP to "app-id-2"),
                ),
                "app/app-id-2",
            ),
        )
        testEnvironments.forEach { test ->
            val actual = loadAwsUserAgentMetadataFromEnvironment(test.provider, ApiMetadata("Test Service", "1.2.3"))
            actual.xAmzUserAgent.shouldContain(test.expected)
        }
    }

    @Test
    fun testExplicitAppId() {
        val testEnvironments = listOf(
            EnvironmentTest(
                TestPlatformProvider(
                    env = mapOf(AWS_APP_ID_ENV to "app-id-1"),
                ),
                "app/explicit-app-id",
            ),
            EnvironmentTest(
                TestPlatformProvider(
                    env = mapOf(AWS_APP_ID_ENV to "app-id-1"),
                    props = mapOf(AWS_APP_ID_PROP to "app-id-2"),
                ),
                "app/explicit-app-id",
            ),
        )
        testEnvironments.forEach { test ->
            val actual = loadAwsUserAgentMetadataFromEnvironment(test.provider, ApiMetadata("Test Service", "1.2.3"), "explicit-app-id")
            actual.xAmzUserAgent.shouldContain(test.expected)
        }
    }
}
