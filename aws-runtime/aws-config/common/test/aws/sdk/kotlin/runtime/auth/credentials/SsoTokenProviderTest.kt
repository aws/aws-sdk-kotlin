/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.TestConnection
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import aws.smithy.kotlin.runtime.util.MapFilesystem
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class SsoTokenProviderTest {
    @Test
    fun testCacheFilename() {
        assertEquals("d033e22ae348aeb5660fc2140aec35850c4da997.json", getCacheFilename("admin"))
        assertEquals("75e4d41276d8bd17f85986fc6cccef29fd725ce3.json", getCacheFilename("dev-scopes"))
    }

    private data class CacheBehaviorTestCase(
        val name: String,
        val currentTime: Instant,
        val cachedTokenContent: String,
        val expectedToken: SsoToken?,
        val refreshResponse: String?,
        val expectedTokenWritebackContent: String?,
        val expectedError: String?,
    ) {
        companion object {
            fun fromJson(json: JsonObject): CacheBehaviorTestCase {
                val name = (json["documentation"] as JsonPrimitive).content
                val currentTime = (json["currentTime"] as JsonPrimitive).content.let(Instant::fromIso8601)
                val cachedTokenJson = json["cachedToken"]!!.toString()
                val refreshResponse = json["refreshResponse"]?.toString()
                val expectedTokenWriteBack = json["expectedTokenWriteback"]?.toString()
                val expectedToken = json["expectedToken"]?.jsonObject?.let { tokenJson ->
                    val token = tokenJson["token"]!!.jsonPrimitive.content
                    val expiration = tokenJson["expiration"]!!.jsonPrimitive.content.let(Instant::fromIso8601)
                    SsoToken(token, expiration)
                }
                val expectedError = json["expectedException"]?.jsonPrimitive?.content

                return CacheBehaviorTestCase(name, currentTime, cachedTokenJson, expectedToken, refreshResponse, expectedTokenWriteBack, expectedError)
            }
        }
    }

    @Test
    fun testCacheLoadingAndRefreshTestSuite() = runTest {
        // these tests are from the SEP
        val testList = Json.parseToJsonElement(SSO_TOKEN_CACHE_BEHAVIOR_TEST_SUITE).jsonObject["cases"]!!.jsonArray

        testList.map { testCase ->
            runCatching {
                CacheBehaviorTestCase.fromJson(testCase.jsonObject)
            }.also {
                if (it.isFailure) {
                    fail("failed to parse test case: `$testCase`", it.exceptionOrNull())
                }
            }.getOrThrow()
        }
            .forEachIndexed { idx, testCase ->
                val sessionName = "test-session"
                val key = getCacheFilename(sessionName)
                val cachePath = "/home/.aws/sso/cache/$key"
                val testPlatform = TestPlatformProvider(
                    env = mapOf("HOME" to "/home"),
                    fs = mapOf(cachePath to testCase.cachedTokenContent),
                )

                val refreshBufferWindow = 0.seconds
                val testClock = ManualClock(testCase.currentTime)

                val httpClient = if (testCase.refreshResponse != null) {
                    buildTestConnection {
                        val body = testCase.refreshResponse.encodeToByteArray()
                        expect(HttpResponse(HttpStatusCode.OK, Headers.Empty, HttpBody.fromBytes(body)))
                    }
                } else {
                    TestConnection()
                }

                val tokenProvider = SsoTokenProvider(
                    sessionName,
                    // start url and region come from the profile not the cached token, values are irrelevant for the test
                    "start-url",
                    "sso-region",
                    refreshBufferWindow,
                    httpClient,
                    testPlatform,
                    testClock,
                )

                if (testCase.expectedError != null) {
                    val ex = assertFails {
                        tokenProvider.resolve()
                    }
                    ex.message.shouldContain(testCase.expectedError)
                } else {
                    val token = tokenProvider.resolve()
                    val actual = SsoToken(token.token, token.expiration!!)
                    assertEquals(testCase.expectedToken, actual, "[idx=$idx]: $testCase")

                    if (testCase.expectedTokenWritebackContent != null) {
                        val contents = assertNotNull(testPlatform.readFileOrNull(cachePath))
                        val written = deserializeSsoToken(contents)
                        val expected = deserializeSsoToken(testCase.expectedTokenWritebackContent.encodeToByteArray())
                        assertEquals(expected, written, "[idx=$idx]: $testCase")
                    }

                    if (testCase.refreshResponse != null) {
                        val fs = testPlatform.fs as MapFilesystem
                        val actualPermissions = fs.getFilePermissions(cachePath)
                        assertEquals(
                            "600",
                            actualPermissions,
                            """Expected `600` permissions on $cachePath for test "${testCase.name}"""",
                        )
                    }
                }
            }
    }
}

// language=JSON
private const val SSO_TOKEN_CACHE_BEHAVIOR_TEST_SUITE = """
{
    "cases": [
        {
            "documentation": "Valid token with all fields",
            "currentTime": "2021-12-25T13:30:00Z",
            "cachedToken": {
                "startUrl": "https://d-123.awsapps.com/start",
                "region": "us-west-2",
                "accessToken": "cachedtoken",
                "expiresAt": "2021-12-25T21:30:00Z",
                "clientId": "clientid",
                "clientSecret": "YSBzZWNyZXQ=",
                "registrationExpiresAt": "2022-12-25T13:30:00Z",
                "refreshToken": "cachedrefreshtoken"
            },
            "expectedToken": {
                "token": "cachedtoken",
                "expiration": "2021-12-25T21:30:00Z"
            }
        },
        {
            "documentation": "Minimal valid cached token",
            "currentTime": "2021-12-25T13:30:00Z",
            "cachedToken": {
                "accessToken": "cachedtoken",
                "expiresAt": "2021-12-25T21:30:00Z"
            },
            "expectedToken": {
                "token": "cachedtoken",
                "expiration": "2021-12-25T21:30:00Z"
            }
        },
        {
            "documentation": "Minimal expired cached token",
            "currentTime": "2021-12-25T13:30:00Z",
            "cachedToken": {
                "accessToken": "cachedtoken",
                "expiresAt": "2021-12-25T13:00:00Z"
            },
            "expectedException": "SSO token for sso-session: test-session is expired"
        },
        {
            "documentation": "Expired token refresh with refresh token",
            "currentTime": "2021-12-25T13:30:00Z",
            "cachedToken": {
                "startUrl": "https://d-123.awsapps.com/start",
                "region": "us-west-2",
                "accessToken": "cachedtoken",
                "expiresAt": "2021-12-25T13:00:00Z",
                "clientId": "clientid",
                "clientSecret": "YSBzZWNyZXQ=",
                "registrationExpiresAt": "2022-12-25T13:30:00Z",
                "refreshToken": "cachedrefreshtoken"
            },
            "refreshResponse": {
                "tokenType": "Bearer",
                "accessToken": "newtoken",
                "expiresIn": 28800,
                "refreshToken": "newrefreshtoken"
            },
            "expectedTokenWriteback": {
                "startUrl": "https://d-123.awsapps.com/start",
                "region": "us-west-2",
                "accessToken": "newtoken",
                "expiresAt": "2021-12-25T21:30:00Z",
                "clientId": "clientid",
                "clientSecret": "YSBzZWNyZXQ=",
                "registrationExpiresAt": "2022-12-25T13:30:00Z",
                "refreshToken": "newrefreshtoken"
            },
            "expectedToken": {
                "token": "newtoken",
                "expiration": "2021-12-25T21:30:00Z"
            }
        },
        {
            "documentation": "Expired token refresh without new refresh token",
            "currentTime": "2021-12-25T13:30:00Z",
            "cachedToken": {
                "startUrl": "https://d-123.awsapps.com/start",
                "region": "us-west-2",
                "accessToken": "cachedtoken",
                "expiresAt": "2021-12-25T13:00:00Z",
                "clientId": "clientid",
                "clientSecret": "YSBzZWNyZXQ=",
                "registrationExpiresAt": "2022-12-25T13:30:00Z",
                "refreshToken": "cachedrefreshtoken"
            },
            "refreshResponse": {
                "tokenType": "Bearer",
                "accessToken": "newtoken",
                "expiresIn": 28800
            },
            "expectedTokenWriteback": {
                "startUrl": "https://d-123.awsapps.com/start",
                "region": "us-west-2",
                "accessToken": "newtoken",
                "expiresAt": "2021-12-25T21:30:00Z",
                "clientId": "clientid",
                "clientSecret": "YSBzZWNyZXQ=",
                "registrationExpiresAt": "2022-12-25T13:30:00Z"
            },
            "expectedToken": {
                "token": "newtoken",
                "expiration": "2021-12-25T21:30:00Z"
            }
        },
        {
            "documentation": "Expired token and expired client registration",
            "currentTime": "2021-12-25T13:30:00Z",
            "cachedToken": {
                "startUrl": "https://d-123.awsapps.com/start",
                "region": "us-west-2",
                "accessToken": "cachedtoken",
                "expiresAt": "2021-10-25T13:00:00Z",
                "clientId": "clientid",
                "clientSecret": "YSBzZWNyZXQ=",
                "registrationExpiresAt": "2021-11-25T13:30:00Z",
                "refreshToken": "cachedrefreshtoken"
            },
            "expectedException": "SSO token for sso-session: test-session is expired"
        }
    ]
}
"""
