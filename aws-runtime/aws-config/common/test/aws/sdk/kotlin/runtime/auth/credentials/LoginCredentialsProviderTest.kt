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
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.HttpTestConnectionBuilder
import aws.smithy.kotlin.runtime.httptest.TestConnection
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import io.kotest.matchers.string.shouldMatch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.text.encodeToByteArray
import kotlin.to

class LoginCredentialsProviderTest {

    @Test
    fun testCacheFilename() {
        val expected = "36db1d138ff460920374e4c3d8e01f53f9f73537e89c88d639f68393df0e2726.json"
        val actual = getLoginCacheFilename("arn:aws:iam::0123456789012:user/Admin")
        assertEquals(expected, actual)
    }

    @Test
    fun testExpiredToken() = runTest {
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

        val testPlatform = TestPlatformProvider(
            env = mapOf("HOME" to "/home"),
            fs = mapOf("/home/.aws/login/cache/$key" to contents),
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

        val testPlatform = TestPlatformProvider(
            env = mapOf("HOME" to "/home"),
            fs = mapOf("/home/.aws/login/cache/$key" to contents),
        )

        val provider = LoginCredentialsProvider(
            loginSession = "arn:aws:iam::123456789:user/TestUser",
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
}
