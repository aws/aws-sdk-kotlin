/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.CallerHasCachedCredentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CallerOwnsCredentialsRefresh
import aws.smithy.kotlin.runtime.auth.awscredentials.isNonRecoverableCredentialsError
import aws.smithy.kotlin.runtime.auth.awscredentials.resilientlyCached
import aws.smithy.kotlin.runtime.collections.attributesOf
import aws.smithy.kotlin.runtime.collections.emptyAttributes
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.TestConnection
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.util.TestFile
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * How a failed Login token refresh is reported. Used directly, the provider keeps its previous behavior. Under a
 * cache, which owns the refresh lifecycle, a failure is handed to the cache instead of being hidden behind the old
 * token, so the cache can apply static stability and the refresh backoff.
 */
class LoginRefreshFailureTest {
    private val sessionName = "arn:aws:sts::012345678910:assumed-role/Admin/admin"
    private val cacheFile = "/home/.aws/login/cache/4b0ba8f99f075c0633e122fd73346ce203a3faf18ea0310eb2d29df1bab2e255.json"
    private val epoch = Instant.fromIso8601("2025-11-19T00:00:00Z")

    private val underWarmCache = attributesOf {
        CallerOwnsCredentialsRefresh to true
        CallerHasCachedCredentials to true
    }
    private val underColdCache = attributesOf {
        CallerOwnsCredentialsRefresh to true
        CallerHasCachedCredentials to false
    }

    private fun cachedToken(expiresAt: Instant) = """
        {
          "accessToken": {
            "accessKeyId": "OLDKEY",
            "secretAccessKey": "oldSecret",
            "sessionToken": "oldSessionToken",
            "accountId": "012345678901",
            "expiresAt": "${expiresAt.format(TimestampFormat.ISO_8601)}"
          },
          "clientId": "arn:aws:signin:::devtools/same-device",
          "refreshToken": "refresh_token",
          "dpopKey": "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIPt/u8InPLpQeQLJTvVX+sNDzni8vMDMt3Liu+nMBigfoAoGCCqGSM49\nAwEHoUQDQgAEILkGG7rNOnxiIJlMgimY1UPP8eDMFP0DAY6WGjngP4bvTAiUCQ/I\nffut2379uP+OBCm2ovGpBOJRgrl1RspUOQ==\n-----END EC PRIVATE KEY-----\n"
        }
    """.trimIndent()

    /** A Sign-In failure that is not one of the rejections needing reauthentication. */
    private fun failedRefresh() = HttpResponse(HttpStatusCode.BadRequest, Headers.Empty, HttpBody.Empty)

    private fun rejectedRefresh(error: String) = HttpResponse(
        HttpStatusCode.BadRequest,
        Headers { append("x-amzn-errortype", "AccessDeniedException") },
        HttpBody.fromBytes("""{"error":"$error","message":"rejected"}""".encodeToByteArray()),
    )

    private suspend fun provider(clock: ManualClock, expiresIn: Duration, connection: TestConnection, rawToken: String? = null): LoginTokenProvider {
        val platform = TestPlatformProvider.of(
            env = mapOf("HOME" to "/home"),
            fs = mapOf(cacheFile to TestFile(rawToken ?: cachedToken(clock.now() + expiresIn))),
        )
        return LoginTokenProvider(
            loginSessionName = sessionName,
            region = "us-west-2",
            refreshBufferWindow = 5.minutes,
            httpClient = connection,
            platformProvider = platform,
            clock = clock,
            cacheDirectory = resolveCacheDir(platform),
            client = signinClient("us-west-2", providedHttpClient = connection),
        )
    }

    @Test
    fun testUsedDirectlyAValidTokenIsStillReturnedWhenRefreshFails() = runTest {
        val clock = ManualClock(epoch)
        val connection = buildTestConnection { expect(failedRefresh()) }

        val creds = provider(clock, 3.minutes, connection).resolve(emptyAttributes())

        assertEquals("OLDKEY", creds.accessKeyId)
    }

    @Test
    fun testUnderAWarmCacheAFailedRefreshIsHandedToTheCache() = runTest {
        val clock = ManualClock(epoch)
        val connection = buildTestConnection { expect(failedRefresh()) }

        val ex = assertFailsWith<Exception> { provider(clock, 3.minutes, connection).resolve(underWarmCache) }

        assertFalse(ex.isNonRecoverableCredentialsError(), "a transient failure is recoverable, so the cache applies static stability")
    }

    @Test
    fun testUnderAColdCacheAValidTokenIsStillReturnedWhenRefreshFails() = runTest {
        val clock = ManualClock(epoch)
        val connection = buildTestConnection { expect(failedRefresh()) }

        val creds = provider(clock, 3.minutes, connection).resolve(underColdCache)

        assertEquals("OLDKEY", creds.accessKeyId, "a cold cache has nothing to fall back on")
    }

    @Test
    fun testAnExpiredTokenWhoseRefreshFailedTransientlyIsRecoverable() = runTest {
        val clock = ManualClock(epoch)
        val connection = buildTestConnection { expect(failedRefresh()) }
        val p = provider(clock, 3.minutes, connection)
        clock.advance(4.minutes)

        val ex = assertFailsWith<InvalidLoginTokenException> { p.resolve(underWarmCache) }

        assertFalse(ex.isNonRecoverableCredentialsError())
    }

    @Test
    fun testRejectionsNeedingReauthenticationAreRaisedWhileTheTokenIsValid() = runTest {
        listOf("TOKEN_EXPIRED", "USER_CREDENTIALS_CHANGED", "INSUFFICIENT_PERMISSIONS").forEach { error ->
            val clock = ManualClock(epoch)
            val connection = buildTestConnection { expect(rejectedRefresh(error)) }

            val ex = assertFailsWith<InvalidLoginTokenException>(error) { provider(clock, 3.minutes, connection).resolve(underColdCache) }

            assertTrue(ex.isNonRecoverableCredentialsError(), "expected $error to be non-recoverable")
        }
    }

    @Test
    fun testAnExpiredAuthorizationCodeIsRecoverable() = runTest {
        val clock = ManualClock(epoch)
        val connection = buildTestConnection { expect(rejectedRefresh("AUTHCODE_EXPIRED")) }
        val p = provider(clock, 3.minutes, connection)
        clock.advance(4.minutes)

        val ex = assertFailsWith<InvalidLoginTokenException> { p.resolve(underWarmCache) }

        assertFalse(ex.isNonRecoverableCredentialsError())
    }

    @Test
    fun testAMalformedTokenIsNonRecoverable() = runTest {
        val clock = ManualClock(epoch)
        val ex = assertFailsWith<InvalidLoginTokenException> {
            provider(clock, 3.minutes, TestConnection(), rawToken = """{"clientId": "x"}""").resolve(emptyAttributes())
        }

        assertTrue(ex.isNonRecoverableCredentialsError())
    }

    @Test
    fun testTheCacheServesCachedCredentialsAndBacksOffWhileSignInIsDown() = runTest {
        val clock = ManualClock(epoch)
        // Room for more Sign-In calls than should happen: after the first fails, the refresh backoff stops the rest.
        val connection = buildTestConnection { repeat(3) { expect(failedRefresh()) } }
        val cache = provider(clock, 30.minutes, connection).resilientlyCached(clock = clock)

        val cached = cache.resolve()
        clock.advance(26.minutes) // inside both the token's refresh buffer and the cache's advisory window

        assertEquals(cached.accessKeyId, cache.resolve().accessKeyId)
        assertEquals(cached.accessKeyId, cache.resolve().accessKeyId)
        assertEquals(1, connection.requests().size, "Sign-In is called once, then the backoff applies")
    }
}
