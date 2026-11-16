/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.TestConnection
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import aws.smithy.kotlin.runtime.util.TestFile
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import io.kotest.matchers.string.shouldMatch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.text.encodeToByteArray
import kotlin.time.Duration.Companion.minutes
import kotlin.to

class LoginCredentialsProviderTest {
    @Test
    fun testCacheFilename() {
        val expected = "36db1d138ff460920374e4c3d8e01f53f9f73537e89c88d639f68393df0e2726.json"
        val actual = getLoginCacheFilename("arn:aws:iam::0123456789012:user/Admin")
        assertEquals(expected, actual)
    }

    @Test
    fun testResolveCacheDirFromEnv() {
        val testPlatform = TestPlatformProvider.of(
            env = mapOf("HOME" to "/home", "AWS_LOGIN_CACHE_DIRECTORY" to "/custom/cache/dir"),
        )
        assertEquals("/custom/cache/dir", resolveCacheDir(testPlatform))
    }

    @Test
    fun testResolveCacheDirFromLegacyEnv() {
        val testPlatform = TestPlatformProvider.of(
            env = mapOf("HOME" to "/home", "AWS_LOGIN_IN_CACHE_DIRECTORY" to "/legacy/cache/dir"),
        )
        assertEquals("/legacy/cache/dir", resolveCacheDir(testPlatform))
    }

    @Test
    fun testResolveCacheDirEnvTakesPrecedenceOverLegacy() {
        val testPlatform = TestPlatformProvider.of(
            env = mapOf(
                "HOME" to "/home",
                "AWS_LOGIN_CACHE_DIRECTORY" to "/custom/cache/dir",
                "AWS_LOGIN_IN_CACHE_DIRECTORY" to "/legacy/cache/dir",
            ),
        )
        assertEquals("/custom/cache/dir", resolveCacheDir(testPlatform))
    }

    @Test
    fun testResolveCacheDirDefault() {
        val testPlatform = TestPlatformProvider.of(
            env = mapOf("HOME" to "/home"),
        )
        assertEquals(
            testPlatform.filepath("~", ".aws", "login", "cache"),
            resolveCacheDir(testPlatform),
        )
    }

    @Test
    fun testExpiredToken() = runTest(
        // TODO: Figure out why this test takes so long to run on some developer machines (@aoperez)
        // TODO: Remove custom timeout
        timeout = 2.minutes,
    ) {
        val engine = TestConnection()

        val epoch = "2025-09-15T04:05:45Z"
        val testClock = ManualClock(epoch = Instant.fromIso8601(epoch))

        val contents = """
        {
            "accessToken": {
                "accessKeyId": "AKIAIOSFODNN7EXAMPLE",
                "secretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
                "sessionToken": "AQoEXAMPLEH4aoAH0gNCAPyJxz4BlCFFxWNE1OPTgk5TthT+FvwqnKwRcOIfrRh3c/LTo6UDdyJwOOvEVPvLXCrrrUtdnniCEXAMPLE/IvU1dYUg2RVAJBanLiHb4IgRmpRV3zrkuWJOgQs8IZZaIv2BXIa2R4OlgkBN9bkUDNCJiBeb/AXlzBBko7b15fjrBs2+cTQtpZ3CYWFXG8C5zqx37wnOE49mRl/+OtkIKGO7fAE",
                "accountId": "012345678901",
                "expiresAt": "2025-09-14T04:05:45Z"
            },
            "tokenType": "aws_sigv4",
            "refreshToken": "<opaque string>",
            "identityToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.EkN-DOsnsuRjRO6BxXemmJDm3HbxrbRzXglbN2S4sOkopdU4IsDxTI8jO19W_A4K8ZPJijNLis4EZsHeY559a4DFOd50_OqgHs3UjpbCqhpuU5K_TGOj3pY-TJXSw",
            "clientId": "arn:aws:signin:::devtools/same-device",
            "dpopKey": "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIFDZHUzOG1Pzq+6F0mjMlOSp1syN9LRPBuHMoCFXTcXhoAoGCCqGSM49\nAwEHoUQDQgAE9qhj+KtcdHj1kVgwxWWWw++tqoh7H7UHs7oXh8jBbgF47rrYGC+t\ndjiIaHK3dBvvdE7MGj5HsepzLm3Kj91bqA==\n-----END EC PRIVATE KEY-----\n"
        }
        """

        val key = getLoginCacheFilename("arn:aws:iam::0123456789012:user/Admin")

        val testPlatform = TestPlatformProvider.of(
            env = mapOf("HOME" to "/home"),
            fs = mapOf("/home/.aws/login/cache/$key" to TestFile(contents)),
        )

        val provider = LoginCredentialsProvider(
            loginSession = "arn:aws:iam::0123456789012:user/Admin",
            httpClient = engine,
            platformProvider = testPlatform,
            clock = testClock,
        )

        assertFailsWith<InvalidLoginTokenException> {
            provider.resolve()
        }.message.shouldMatch(Regex("Login token for login-session: .* is expired"))
    }

    @Test
    fun testSuccess() = runTest {
        val expectedExpiration = Instant.fromIso8601("2020-10-16T04:56:00Z")

        val serviceResp = """
        {
            "accessToken": {
                "accessKeyId": "AKID",
                "secretAccessKey": "secret",
                "sessionToken": "session-token"
            },
            "expiresIn": 3600,
            "refreshToken": "new-refresh-token",
            "tokenType": "aws_sigv4"
        }
        """

        val engine = buildTestConnection {
            expect(
                HttpResponse(HttpStatusCode.OK, Headers.Empty, HttpBody.fromBytes(serviceResp.encodeToByteArray())),
            )
        }

        val epoch = "2020-10-16T03:56:00Z"
        val testClock = ManualClock(epoch = Instant.fromIso8601(epoch))

        val contents = """
        {
            "accessToken": {
                "accessKeyId": "OLD_AKID",
                "secretAccessKey": "old-secret",
                "sessionToken": "old-session-token",
                "accountId": "123456789",
                "expiresAt": "2020-10-16T03:50:00Z"
            },
            "tokenType": "aws_sigv4",
            "refreshToken": "refresh-token",
            "clientId": "test-client-id",
            "dpopKey": "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIFDZHUzOG1Pzq+6F0mjMlOSp1syN9LRPBuHMoCFXTcXhoAoGCCqGSM49\nAwEHoUQDQgAE9qhj+KtcdHj1kVgwxWWWw++tqoh7H7UHs7oXh8jBbgF47rrYGC+t\ndjiIaHK3dBvvdE7MGj5HsepzLm3Kj91bqA==\n-----END EC PRIVATE KEY-----\n"
        }
        """

        val key = getLoginCacheFilename("arn:aws:iam::123456789:user/TestUser")

        val testPlatform = TestPlatformProvider.of(
            env = mapOf("HOME" to "/home"),
            fs = mapOf("/home/.aws/login/cache/$key" to TestFile(contents)),
        )

        val provider = LoginCredentialsProvider(
            loginSession = "arn:aws:iam::123456789:user/TestUser",
            region = "us-west-2",
            httpClient = engine,
            platformProvider = testPlatform,
            clock = testClock,
        )

        val actual = provider.resolve()
        val expected = credentials(
            accessKeyId = "AKID",
            secretAccessKey = "secret",
            sessionToken = "session-token",
            expiration = expectedExpiration,
            providerName = "LOGIN",
            accountId = "123456789",
        ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_LOGIN)

        assertEquals(expected, actual)
    }

    private fun expiredTokenContents(signInRegionLine: String? = null): String = """
        {
            "accessToken": {
                "accessKeyId": "OLD_AKID",
                "secretAccessKey": "old-secret",
                "sessionToken": "old-session-token",
                "accountId": "123456789",
                "expiresAt": "2020-10-16T03:50:00Z"
            },
            "tokenType": "aws_sigv4",
            "refreshToken": "refresh-token",
            "clientId": "test-client-id",${signInRegionLine?.let { "\n            $it," } ?: ""}
            "dpopKey": "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIFDZHUzOG1Pzq+6F0mjMlOSp1syN9LRPBuHMoCFXTcXhoAoGCCqGSM49\nAwEHoUQDQgAE9qhj+KtcdHj1kVgwxWWWw++tqoh7H7UHs7oXh8jBbgF47rrYGC+t\ndjiIaHK3dBvvdE7MGj5HsepzLm3Kj91bqA==\n-----END EC PRIVATE KEY-----\n"
        }
    """

    /**
     * Refreshes an expired token with the given cache contents through a provider configured for `us-west-2` and
     * returns the single request that was sent.
     */
    private suspend fun refreshAndCaptureRequest(contents: String): HttpRequest {
        val serviceResp = """
        {
            "accessToken": {
                "accessKeyId": "AKID",
                "secretAccessKey": "secret",
                "sessionToken": "session-token"
            },
            "expiresIn": 3600,
            "refreshToken": "new-refresh-token",
            "tokenType": "aws_sigv4"
        }
        """

        val engine = buildTestConnection {
            expect(
                HttpResponse(HttpStatusCode.OK, Headers.Empty, HttpBody.fromBytes(serviceResp.encodeToByteArray())),
            )
        }

        val testClock = ManualClock(epoch = Instant.fromIso8601("2020-10-16T03:56:00Z"))
        val key = getLoginCacheFilename("arn:aws:iam::123456789:user/TestUser")
        val testPlatform = TestPlatformProvider.of(
            env = mapOf("HOME" to "/home"),
            fs = mapOf("/home/.aws/login/cache/$key" to TestFile(contents)),
        )

        val provider = LoginCredentialsProvider(
            loginSession = "arn:aws:iam::123456789:user/TestUser",
            region = "us-west-2",
            httpClient = engine,
            platformProvider = testPlatform,
            clock = testClock,
        )

        assertEquals("AKID", provider.resolve().accessKeyId)
        return engine.requests().single().actual
    }

    @Test
    fun testRefreshUsesSignInRegion() = runTest {
        val request = refreshAndCaptureRequest(expiredTokenContents("\"signInRegion\": \"eu-central-1\""))

        val host = request.url.host.toString()
        assertTrue(host.contains("eu-central-1"), "expected eu-central-1 in host, got $host")
        assertFalse(host.contains("us-west-2"), "expected configured region to be ignored, got $host")

        val dpop = assertNotNull(request.headers["DPoP"])
        val payload = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
            .decode(dpop.split(".")[1])
            .decodeToString()
        val htu = Json.parseToJsonElement(payload).jsonObject["htu"]!!.jsonPrimitive.content
        assertTrue(htu.contains("eu-central-1"), "expected eu-central-1 in htu, got $htu")
        assertEquals(request.url.toString(), htu)
    }

    @Test
    fun testRefreshWithoutSignInRegionUsesConfiguredRegion() = runTest {
        val request = refreshAndCaptureRequest(expiredTokenContents())
        val host = request.url.host.toString()
        assertTrue(host.contains("us-west-2"), "expected us-west-2 in host, got $host")
    }

    @Test
    fun testRefreshWithEmptySignInRegionUsesConfiguredRegion() = runTest {
        val request = refreshAndCaptureRequest(expiredTokenContents("\"signInRegion\": \"\""))
        val host = request.url.host.toString()
        assertTrue(host.contains("us-west-2"), "expected us-west-2 in host, got $host")
    }
}
