/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.auth

import aws.sdk.kotlin.runtime.config.profile.AwsConfigurationSource
import aws.sdk.kotlin.runtime.config.profile.FileType
import aws.sdk.kotlin.runtime.config.profile.parse
import aws.sdk.kotlin.runtime.config.profile.toSharedConfig
import aws.smithy.kotlin.runtime.auth.AuthSchemeId
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveAuthSchemePreferenceTest {
    @Test
    fun testProfile() = runTest {
        assertEquals(
            AuthSchemeId("sigv4"),
            testResolveAuthSchemePreference(
                profileContent = """
                    [default]
                    auth_scheme_preference = sigv4
                """.trimIndent(),
            ).single(),
        )
    }

    @Test
    fun testEnvironment() = runTest {
        // Environment takes precedence over profile
        assertEquals(
            AuthSchemeId("sigv4a"),
            testResolveAuthSchemePreference(
                env = mapOf("AWS_AUTH_SCHEME_PREFERENCE" to "sigv4a"),
                profileContent = """
                    [default]
                    auth_scheme_preference = sigv4
                """.trimIndent(),
            ).single(),
        )
    }

    @Test
    fun testSystemProperties() = runTest {
        // System properties take precedence over environment and profile
        assertEquals(
            AuthSchemeId("httpBearerAuth"),
            testResolveAuthSchemePreference(
                env = mapOf("AWS_AUTH_SCHEME_PREFERENCE" to "sigv4a"),
                sysProps = mapOf("aws.authSchemePreference" to "httpBearerAuth"),
                profileContent = """
                    [default]
                    auth_scheme_preference = sigv4
                """.trimIndent(),
            ).single(),
        )
    }

    @Test
    fun testResolveMultipleSchemes() = runTest {
        assertEquals(
            listOf(AuthSchemeId("httpBearerAuth"), AuthSchemeId("sigv4a"), AuthSchemeId("sigv4")),
            testResolveAuthSchemePreference(
                env = mapOf("AWS_AUTH_SCHEME_PREFERENCE" to "httpBearerAuth, sigv4a, sigv4"),
            ),
        )
    }

    @Test
    fun testIgnoreWhitespace() = runTest {
        assertEquals(
            listOf(AuthSchemeId("httpBearerAuth"), AuthSchemeId("sigv4a")),
            testResolveAuthSchemePreference(
                env = mapOf("AWS_AUTH_SCHEME_PREFERENCE" to "httpBearerAuth  ,        sigv4a     "),
            ),
        )
    }

    @Test
    fun testDontFailOnInvalidSchemes() = runTest {
        assertEquals(
            listOf(AuthSchemeId("httpBearerAuth"), AuthSchemeId("whatIsThisScheme"), AuthSchemeId("sigv4")),
            testResolveAuthSchemePreference(
                env = mapOf("AWS_AUTH_SCHEME_PREFERENCE" to "httpBearerAuth, whatIsThisScheme, sigv4"),
            ),
        )
    }

    private suspend fun testResolveAuthSchemePreference(
        env: Map<String, String> = mapOf(),
        sysProps: Map<String, String> = mapOf(),
        profileContent: String = "",
    ): List<AuthSchemeId> {
        val platform = TestPlatformProvider.of(env = env, props = sysProps)
        val source = AwsConfigurationSource("default", "", "")
        val profile = asyncLazy {
            parse(Logger.None, FileType.CONFIGURATION, profileContent).toSharedConfig(source).activeProfile
        }

        return resolveAuthSchemePreference(platform, profile)
    }
}
