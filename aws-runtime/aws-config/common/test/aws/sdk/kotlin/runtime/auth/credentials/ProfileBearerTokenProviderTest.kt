/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.TestEngine
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import aws.smithy.kotlin.runtime.util.TestFile
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ProfileBearerTokenProviderTest {
    @Test
    fun testDefaultProfile() = runTest {
        val sessionName = "my-session"
        val key = getCacheFilename(sessionName)
        val cachePath = "/home/.aws/sso/cache/$key"
        val expiresAt = Instant.fromIso8601("2022-07-07T14:30:00Z")
        val clock = ManualClock(expiresAt - 15.minutes)

        val testProvider = TestPlatformProvider.of(
            env = mapOf(
                "HOME" to "/home",
                "AWS_CONFIG_FILE" to "config",
            ),
            fs = mapOf(
                "config" to TestFile(
                    """
                [default]
                sso_session = $sessionName

                [sso-session $sessionName]
                sso_start_url = https://test-url
                sso_region = us-west-2
                    """.trimIndent(),
                ),
                cachePath to TestFile(
                    """
                {
                    "accessToken": "cachedtoken",
                    "expiresAt": "2022-07-07T14:30:00Z"
                }
                    """.trimIndent(),
                ),
            ),
        )

        val provider = ProfileBearerTokenProvider(
            platformProvider = testProvider,
            httpClient = TestEngine(),
            clock = clock,
        )
        val actual = provider.resolve()
        val expected = SsoToken("cachedtoken", expiresAt)
        assertEquals(expected, actual)
    }

    @Test
    fun testSsoSessionMissingStartUrl() = runTest {
        val sessionName = "my-session"
        val expiresAt = Instant.fromIso8601("2022-07-07T14:30:00Z")
        val clock = ManualClock(expiresAt - 15.minutes)

        val testProvider = TestPlatformProvider.of(
            env = mapOf(
                "HOME" to "/home",
                "AWS_CONFIG_FILE" to "config",
            ),
            fs = mapOf(
                "config" to TestFile(
                    """
                [default]
                sso_session = $sessionName

                [sso-session $sessionName]
                sso_region = us-west-2
                    """.trimIndent(),
                ),
            ),
        )

        val provider = ProfileBearerTokenProvider(
            platformProvider = testProvider,
            httpClient = TestEngine(),
            clock = clock,
        )
        val ex = assertFailsWith<ProviderConfigurationException> { provider.resolve() }
        ex.message.shouldContain("sso-session (my-session) missing `sso_start_url`")
    }

    @Test
    fun testSsoSessionMissingSsoRegion() = runTest {
        val sessionName = "my-session"
        val expiresAt = Instant.fromIso8601("2022-07-07T14:30:00Z")
        val clock = ManualClock(expiresAt - 15.minutes)

        val testProvider = TestPlatformProvider.of(
            env = mapOf(
                "HOME" to "/home",
                "AWS_CONFIG_FILE" to "config",
            ),
            fs = mapOf(
                "config" to TestFile(
                    """
                [default]
                sso_session = $sessionName

                [sso-session $sessionName]
                sso_start_url = https://test-url
                    """.trimIndent(),
                ),
            ),
        )

        val provider = ProfileBearerTokenProvider(
            platformProvider = testProvider,
            httpClient = TestEngine(),
            clock = clock,
        )
        val ex = assertFailsWith<ProviderConfigurationException> { provider.resolve() }
        ex.message.shouldContain("sso-session (my-session) missing `sso_region`")
    }

    @Test
    fun testProfileReferencingMissingSsoSession() = runTest {
        val sessionName = "my-session"
        val expiresAt = Instant.fromIso8601("2022-07-07T14:30:00Z")
        val clock = ManualClock(expiresAt - 15.minutes)

        val testProvider = TestPlatformProvider.of(
            env = mapOf(
                "HOME" to "/home",
                "AWS_CONFIG_FILE" to "config",
            ),
            fs = mapOf(
                "config" to TestFile(
                    """
                [default]
                sso_session = $sessionName
                    """.trimIndent(),
                ),
            ),
        )

        val provider = ProfileBearerTokenProvider(
            platformProvider = testProvider,
            httpClient = TestEngine(),
            clock = clock,
        )
        val ex = assertFailsWith<ProviderConfigurationException> { provider.resolve() }
        ex.message.shouldContain("profile (default) references non-existing sso_session = `$sessionName`")
    }

    @Test
    fun testTokenRefreshAttemptsAreSharedAcrossResolutions() = runTest {
        val sessionName = "my-session"
        val expiresAt = Instant.fromIso8601("2022-07-07T14:30:00Z")
        val clock = ManualClock(expiresAt + 1.minutes)

        val testProvider = TestPlatformProvider.of(
            env = mapOf(
                "HOME" to "/home",
                "AWS_CONFIG_FILE" to "config",
            ),
            fs = mapOf(
                "config" to TestFile(
                    """
                [default]
                sso_session = $sessionName

                [sso-session $sessionName]
                sso_start_url = https://test-url
                sso_region = us-west-2
                    """.trimIndent(),
                ),
                "/home/.aws/sso/cache/${getCacheFilename(sessionName)}" to TestFile(
                    """
                {
                    "accessToken": "cachedtoken",
                    "expiresAt": "2022-07-07T14:30:00Z",
                    "clientId": "clientid",
                    "clientSecret": "YSBzZWNyZXQ=",
                    "refreshToken": "cachedrefreshtoken"
                }
                    """.trimIndent(),
                ),
            ),
        )

        // the SSO OIDC service rejecting the refresh token
        fun invalidGrant() = HttpResponse(
            HttpStatusCode.BadRequest,
            Headers { append("X-Amzn-ErrorType", "InvalidGrantException") },
            HttpBody.fromBytes("""{"error":"invalid_grant"}""".encodeToByteArray()),
        )

        val testEngine = buildTestConnection {
            expect(invalidGrant())
            expect(invalidGrant())
        }

        val provider = ProfileBearerTokenProvider(
            platformProvider = testProvider,
            httpClient = testEngine,
            clock = clock,
        )
        assertFailsWith<InvalidSsoTokenException> { provider.resolve() }
        clock.advance(10.seconds)
        assertFailsWith<InvalidSsoTokenException> { provider.resolve() }
        assertEquals(1, testEngine.requests().size, "the second resolution is within 30 seconds of the first refresh attempt")
    }
}
