/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.TestConnection
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.also
import kotlin.collections.filterKeys
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.map
import kotlin.collections.mapValues
import kotlin.collections.set
import kotlin.getOrThrow
import kotlin.runCatching
import kotlin.test.*
import kotlin.text.decodeToString
import kotlin.text.encodeToByteArray
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.to
import kotlin.toString

class LoginTokenProviderTest {
    private data class LoginTestCase(
        val name: String,
        val configContents: String,
        val cacheContents: Map<String, String>,
        val mockApiCalls: JsonArray?,
        val outcomes: List<TestOutcome>,
    ) {
        companion object {
            fun fromJson(json: JsonObject): LoginTestCase {
                val name = json["documentation"]!!.jsonPrimitive.content
                val configContents = json["configContents"]!!.jsonPrimitive.content
                val cacheContents = json["cacheContents"]!!.jsonObject.mapValues { (_, value) ->
                    value.toString()
                }
                val mockApiCalls = json["mockApiCalls"]?.jsonArray
                val outcomes = json["outcomes"]!!.jsonArray.map { outcome ->
                    val outcomeObj = outcome.jsonObject
                    val result = outcomeObj["result"]!!.jsonPrimitive.content
                    when (result) {
                        "credentials" -> TestOutcome.Credentials(
                            accessKeyId = outcomeObj["accessKeyId"]!!.jsonPrimitive.content,
                            secretAccessKey = outcomeObj["secretAccessKey"]!!.jsonPrimitive.content,
                            sessionToken = outcomeObj["sessionToken"]!!.jsonPrimitive.content,
                            accountId = outcomeObj["accountId"]!!.jsonPrimitive.content,
                            expiresAt = Instant.fromIso8601(outcomeObj["expiresAt"]!!.jsonPrimitive.content),
                        )
                        "cacheContents" -> TestOutcome.CacheContents(
                            cacheContents = outcomeObj.filterKeys { it != "result" }.mapValues { it.value.toString() },
                        )
                        "error" -> TestOutcome.Error(
                            message = outcomeObj["message"]!!.jsonPrimitive.content,
                        )
                        else -> error("Unknown result type: $result")
                    }
                }
                return LoginTestCase(name, configContents, cacheContents, mockApiCalls, outcomes)
            }
        }
    }

    private sealed class TestOutcome {
        data class Credentials(
            val accessKeyId: String,
            val secretAccessKey: String,
            val sessionToken: String,
            val accountId: String,
            val expiresAt: Instant,
        ) : TestOutcome()

        data class CacheContents(
            val cacheContents: Map<String, String>,
        ) : TestOutcome()

        data class Error(val message: String) : TestOutcome()
    }

    @Test
    fun testLoginTokenCacheBehavior() = runTest(timeout = 2.minutes) {
        val testList = Json.parseToJsonElement(LOGIN_TOKEN_PROVIDER_TEST_SUITE).jsonArray
        testList.map { testCase ->
            runCatching {
                LoginTestCase.fromJson(testCase.jsonObject)
            }.also {
                if (it.isFailure) {
                    fail("failed to parse test case: `$testCase`", it.exceptionOrNull())
                }
            }.getOrThrow()
        }.forEachIndexed { idx, testCase ->
            val loginSessionName = "arn:aws:sts::012345678910:assumed-role/Admin/admin"

            // Setup filesystem with cache files
            val fs = mutableMapOf<String, String>()
            testCase.cacheContents.forEach { (filename, content) ->
                fs["/home/.aws/login/cache/$filename"] = content
            }

            val testPlatform = TestPlatformProvider(
                env = mapOf("HOME" to "/home"),
                fs = fs,
            )

            val testClock = ManualClock(Instant.fromIso8601("2025-11-19T00:00:00Z"))

            val httpClient = if (testCase.mockApiCalls != null) {
                buildTestConnection {
                    testCase.mockApiCalls.forEach { mockCall ->
                        val responseCode = mockCall.jsonObject["responseCode"]?.jsonPrimitive?.int ?: 200
                        val statusCode = HttpStatusCode.fromValue(responseCode)
                        if (responseCode == 200) {
                            val response = mockCall.jsonObject["response"]?.jsonObject["tokenOutput"]?.jsonObject
                            val body = response.toString().encodeToByteArray()
                            expect(
                                HttpResponse(
                                    statusCode,
                                    Headers.Empty,
                                    HttpBody.fromBytes(body),
                                ),
                            )
                        } else {
                            expect(HttpResponse(statusCode, Headers.Empty, HttpBody.Empty))
                        }
                    }
                }
            } else {
                TestConnection()
            }

            val tokenProvider = LoginTokenProvider(
                loginSessionName = loginSessionName,
                region = "us-west-2",
                refreshBufferWindow = 0.seconds,
                httpClient = httpClient,
                platformProvider = testPlatform,
                clock = testClock,
                cacheDirectory = resolveCacheDir(testPlatform),
                client = signinClient(providedHttpClient = httpClient),
            )

            testCase.outcomes.forEach { expectedOutcome ->
                when (expectedOutcome) {
                    is TestOutcome.Credentials -> {
                        // Verify that credentials are successfully resolved and match expected values
                        val credentials = tokenProvider.resolve()
                        assertEquals(expectedOutcome.accessKeyId, credentials.accessKeyId, "[idx=$idx]: $testCase")
                        assertEquals(
                            expectedOutcome.secretAccessKey,
                            credentials.secretAccessKey,
                            "[idx=$idx]: $testCase",
                        )
                        assertEquals(expectedOutcome.sessionToken, credentials.sessionToken, "[idx=$idx]: $testCase")
                        assertEquals(
                            expectedOutcome.accountId,
                            credentials.attributes.getOrNull(AwsClientOption.AccountId),
                            "[idx=$idx]: $testCase",
                        )
                        assertEquals(expectedOutcome.expiresAt, credentials.expiration, "[idx=$idx]: $testCase")
                    }

                    is TestOutcome.CacheContents -> {
                        // Verify cache contents after token refresh
                        expectedOutcome.cacheContents.forEach { (filename, expectedContent) ->
                            val actualContent =
                                testPlatform.readFileOrNull("/home/.aws/login/cache/$filename")?.decodeToString()
                            assertNotNull(actualContent, "Cache file $filename should exist")

                            val expectedJson = Json.parseToJsonElement(expectedContent).jsonObject
                            val actualJson = Json.parseToJsonElement(actualContent).jsonObject
                            assertEquals(expectedJson, actualJson, "Cache content mismatch for $filename")
                        }
                    }

                    is TestOutcome.Error -> {
                        val exception = assertFails("[idx=$idx]: $testCase") {
                            tokenProvider.resolve()
                        }
                        assertEquals(
                            exception.message?.contains(expectedOutcome.message),
                            true,
                            "[idx=$idx]: Expected error message to contain '${expectedOutcome.message}', but got: ${exception.message}",
                        )
                    }
                }
            }
        }
    }
}

// Note to implementer: these test cases are copied from SEP: https://code.amazon.com/packages/AwsDrSeps/blobs/aadc5f3e3212c3b0a29a2ab6b1ce8dc548f7cfff/--/seps/accepted/shared/login/login-provider-test-cases.json
// Error messages in 'outcomes' (e.g., "missing `accessToken`") are manually added. Update these when updating test cases.
// language=JSON
private const val LOGIN_TOKEN_PROVIDER_TEST_SUITE = """
[
  {
    "documentation": "Success - Valid credentials are returned immediately",
    "configContents": "[profile signin]\nlogin_session = arn:aws:sts::012345678910:assumed-role/Admin/admin\n",
    "cacheContents": {
      "4b0ba8f99f075c0633e122fd73346ce203a3faf18ea0310eb2d29df1bab2e255.json": {
        "accessToken": {
          "accessKeyId": "AKIAIOSFODNN7EXAMPLE",
          "secretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
          "sessionToken": "AQoEXAMPLEH4aoAH0gNCAPyJxz4BlCFFxWNE1OPTgk5TthT+FvwqnKwRcOIfrRh3c/LTo6UDdyJwOOvEVPvLXCrrrUtdnniCEXAMPLE/IvU1dYUg2RVAJBanLiHb4IgRmpRV3zrkuWJOgQs8IZZaIv2BXIa2R4OlgkBN9bkUDNCJiBeb/AXlzBBko7b15fjrBs2+cTQtpZ3CYWFXG8C5zqx37wnOE49mRl/+OtkIKGO7fAE",
          "accountId": "012345678901",
          "expiresAt": "3025-09-14T04:05:45Z"
        },
        "clientId": "arn:aws:signin:::devtools/same-device",
        "refreshToken": "refresh_token",
        "idToken": "eyJraWQiOiI1MzYxMjY2ZS1mNjI5LTQ0ZGQtOTA1My1jYzJkNTM1OTJiOTIiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzM4NCJ9.eyJzdWIiOiJhcm46YXdzOnN0czo6NzIxNzgxNjAzNzU1OmFzc3VtZWQtcm9sZVwvQWRtaW5cL3Nob3ZsaWEtSXNlbmdhcmQiLCJhdWQiOiJhcm46YXdzOnNpZ25pbjo6OmNsaVwvc2FtZS1kZXZpY2UiLCJpc3MiOiJodHRwczpcL1wvc2lnbmluLmF3cy5hbWF6b24uY29tXC9zaWduaW4iLCJzZXNzaW9uX2FybiI6ImFybjphd3M6c3RzOjo3MjE3ODE2MDM3NTU6YXNzdW1lZC1yb2xlXC9BZG1pblwvc2hvdmxpYS1Jc2VuZ2FyZCIsImV4cCI6MTc2MTE2Nzk0NiwiaWF0IjoxNzYxMTY3MDQ2fQ.EzySTg0K11hwQtIYtcBcnNMmX33F6XrVqXsk8WyTWjYcMQxaMnqXebLwBQBCRZha05hZiIZ5xPVCBIt7hZGyymurSfOL72cz69xHUH6u7rwu8vn10UKLHfyKLneKBlmJ",
        "dpopKey": "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIPt/u8InPLpQeQLJTvVX+sNDzni8vMDMt3Liu+nMBigfoAoGCCqGSM49\nAwEHoUQDQgAEILkGG7rNOnxiIJlMgimY1UPP8eDMFP0DAY6WGjngP4bvTAiUCQ/I\nffut2379uP+OBCm2ovGpBOJRgrl1RspUOQ==\n-----END EC PRIVATE KEY-----\n"
      }
    },
    "outcomes": [
      {
        "result": "credentials",
        "accessKeyId": "AKIAIOSFODNN7EXAMPLE",
        "secretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
        "sessionToken": "AQoEXAMPLEH4aoAH0gNCAPyJxz4BlCFFxWNE1OPTgk5TthT+FvwqnKwRcOIfrRh3c/LTo6UDdyJwOOvEVPvLXCrrrUtdnniCEXAMPLE/IvU1dYUg2RVAJBanLiHb4IgRmpRV3zrkuWJOgQs8IZZaIv2BXIa2R4OlgkBN9bkUDNCJiBeb/AXlzBBko7b15fjrBs2+cTQtpZ3CYWFXG8C5zqx37wnOE49mRl/+OtkIKGO7fAE",
        "accountId": "012345678901",
        "expiresAt": "3025-09-14T04:05:45Z"
      }
    ]
  },
  {
    "documentation": "Failure - No cache file",
    "configContents": "[profile signin]\nlogin_session = arn:aws:sts::012345678910:assumed-role/Admin/admin\n",
    "cacheContents": {
    },
    "outcomes": [
      {
        "result": "error",
        "message": "Invalid or missing login session cache. Run `aws login` to initiate a new session"
      }
    ]
  },
  {
    "documentation": "Failure - Missing accessToken",
    "configContents": "[profile signin]\nlogin_session = arn:aws:sts::012345678910:assumed-role/Admin/admin\n",
    "cacheContents": {
      "4b0ba8f99f075c0633e122fd73346ce203a3faf18ea0310eb2d29df1bab2e255.json": {
        "clientId": "arn:aws:signin:::devtools/same-device",
        "refreshToken": "valid_refresh_token_456",
        "idToken": "eyJraWQiOiI1MzYxMjY2ZS1mNjI5LTQ0ZGQtOTA1My1jYzJkNTM1OTJiOTIiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzM4NCJ9.eyJzdWIiOiJhcm46YXdzOnN0czo6NzIxNzgxNjAzNzU1OmFzc3VtZWQtcm9sZVwvQWRtaW5cL3Nob3ZsaWEtSXNlbmdhcmQiLCJhdWQiOiJhcm46YXdzOnNpZ25pbjo6OmNsaVwvc2FtZS1kZXZpY2UiLCJpc3MiOiJodHRwczpcL1wvc2lnbmluLmF3cy5hbWF6b24uY29tXC9zaWduaW4iLCJzZXNzaW9uX2FybiI6ImFybjphd3M6c3RzOjo3MjE3ODE2MDM3NTU6YXNzdW1lZC1yb2xlXC9BZG1pblwvc2hvdmxpYS1Jc2VuZ2FyZCIsImV4cCI6MTc2MTE2Nzk0NiwiaWF0IjoxNzYxMTY3MDQ2fQ.EzySTg0K11hwQtIYtcBcnNMmX33F6XrVqXsk8WyTWjYcMQxaMnqXebLwBQBCRZha05hZiIZ5xPVCBIt7hZGyymurSfOL72cz69xHUH6u7rwu8vn10UKLHfyKLneKBlmJ",
        "dpopKey": "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIPt/u8InPLpQeQLJTvVX+sNDzni8vMDMt3Liu+nMBigfoAoGCCqGSM49\nAwEHoUQDQgAEILkGG7rNOnxiIJlMgimY1UPP8eDMFP0DAY6WGjngP4bvTAiUCQ/I\nffut2379uP+OBCm2ovGpBOJRgrl1RspUOQ==\n-----END EC PRIVATE KEY-----\n"
      }
    },
    "outcomes": [
      {
        "result": "error",
        "message": "missing `accessToken`"
      }
    ]
  },
  {
    "documentation": "Failure - Missing refreshToken",
    "configContents": "[profile signin]\nlogin_session = arn:aws:sts::012345678910:assumed-role/Admin/admin\n",
    "cacheContents": {
      "4b0ba8f99f075c0633e122fd73346ce203a3faf18ea0310eb2d29df1bab2e255.json": {
        "accessToken": {
          "accessKeyId": "AKIAIOSFODNN7EXAMPLE",
          "secretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
          "sessionToken": "AQoEXAMPLEH4aoAH0gNCAPyJxz4BlCFFxWNE1OPTgk5TthT+FvwqnKwRcOIfrRh3c/LTo6UDdyJwOOvEVPvLXCrrrUtdnniCEXAMPLE/IvU1dYUg2RVAJBanLiHb4IgRmpRV3zrkuWJOgQs8IZZaIv2BXIa2R4OlgkBN9bkUDNCJiBeb/AXlzBBko7b15fjrBs2+cTQtpZ3CYWFXG8C5zqx37wnOE49mRl/+OtkIKGO7fAE",
          "accountId": "012345678901",
          "expiresAt": "2020-01-01T00:00:00Z"
        },
        "clientId": "arn:aws:signin:::devtools/same-device",
        "idToken": "eyJraWQiOiI1MzYxMjY2ZS1mNjI5LTQ0ZGQtOTA1My1jYzJkNTM1OTJiOTIiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzM4NCJ9.eyJzdWIiOiJhcm46YXdzOnN0czo6NzIxNzgxNjAzNzU1OmFzc3VtZWQtcm9sZVwvQWRtaW5cL3Nob3ZsaWEtSXNlbmdhcmQiLCJhdWQiOiJhcm46YXdzOnNpZ25pbjo6OmNsaVwvc2FtZS1kZXZpY2UiLCJpc3MiOiJodHRwczpcL1wvc2lnbmluLmF3cy5hbWF6b24uY29tXC9zaWduaW4iLCJzZXNzaW9uX2FybiI6ImFybjphd3M6c3RzOjo3MjE3ODE2MDM3NTU6YXNzdW1lZC1yb2xlXC9BZG1pblwvc2hvdmxpYS1Jc2VuZ2FyZCIsImV4cCI6MTc2MTE2Nzk0NiwiaWF0IjoxNzYxMTY3MDQ2fQ.EzySTg0K11hwQtIYtcBcnNMmX33F6XrVqXsk8WyTWjYcMQxaMnqXebLwBQBCRZha05hZiIZ5xPVCBIt7hZGyymurSfOL72cz69xHUH6u7rwu8vn10UKLHfyKLneKBlmJ",
        "dpopKey": "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIPt/u8InPLpQeQLJTvVX+sNDzni8vMDMt3Liu+nMBigfoAoGCCqGSM49\nAwEHoUQDQgAEILkGG7rNOnxiIJlMgimY1UPP8eDMFP0DAY6WGjngP4bvTAiUCQ/I\nffut2379uP+OBCm2ovGpBOJRgrl1RspUOQ==\n-----END EC PRIVATE KEY-----\n"
      }
    },
    "outcomes": [
      {
        "result": "error",
        "message": "missing `refreshToken`"
      }
    ]
  },
  {
    "documentation": "Failure - Missing clientId in cache",
    "configContents": "[profile signin]\nlogin_session = arn:aws:sts::012345678910:assumed-role/Admin/admin\n",
    "cacheContents": {
      "4b0ba8f99f075c0633e122fd73346ce203a3faf18ea0310eb2d29df1bab2e255.json": {
        "accessToken": {
          "accessKeyId": "AKIAIOSFODNN7EXAMPLE",
          "secretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
          "sessionToken": "AQoEXAMPLEH4aoAH0gNCAPyJxz4BlCFFxWNE1OPTgk5TthT+FvwqnKwRcOIfrRh3c/LTo6UDdyJwOOvEVPvLXCrrrUtdnniCEXAMPLE/IvU1dYUg2RVAJBanLiHb4IgRmpRV3zrkuWJOgQs8IZZaIv2BXIa2R4OlgkBN9bkUDNCJiBeb/AXlzBBko7b15fjrBs2+cTQtpZ3CYWFXG8C5zqx37wnOE49mRl/+OtkIKGO7fAE",
          "accountId": "012345678901",
          "expiresAt": "2020-01-01T00:00:00Z"
        },
        "refreshToken": "valid_refresh_token_789",
        "idToken": "eyJraWQiOiI1MzYxMjY2ZS1mNjI5LTQ0ZGQtOTA1My1jYzJkNTM1OTJiOTIiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzM4NCJ9.eyJzdWIiOiJhcm46YXdzOnN0czo6NzIxNzgxNjAzNzU1OmFzc3VtZWQtcm9sZVwvQWRtaW5cL3Nob3ZsaWEtSXNlbmdhcmQiLCJhdWQiOiJhcm46YXdzOnNpZ25pbjo6OmNsaVwvc2FtZS1kZXZpY2UiLCJpc3MiOiJodHRwczpcL1wvc2lnbmluLmF3cy5hbWF6b24uY29tXC9zaWduaW4iLCJzZXNzaW9uX2FybiI6ImFybjphd3M6c3RzOjo3MjE3ODE2MDM3NTU6YXNzdW1lZC1yb2xlXC9BZG1pblwvc2hvdmxpYS1Jc2VuZ2FyZCIsImV4cCI6MTc2MTE2Nzk0NiwiaWF0IjoxNzYxMTY3MDQ2fQ.EzySTg0K11hwQtIYtcBcnNMmX33F6XrVqXsk8WyTWjYcMQxaMnqXebLwBQBCRZha05hZiIZ5xPVCBIt7hZGyymurSfOL72cz69xHUH6u7rwu8vn10UKLHfyKLneKBlmJ",
        "dpopKey": "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIPt/u8InPLpQeQLJTvVX+sNDzni8vMDMt3Liu+nMBigfoAoGCCqGSM49\nAwEHoUQDQgAEILkGG7rNOnxiIJlMgimY1UPP8eDMFP0DAY6WGjngP4bvTAiUCQ/I\nffut2379uP+OBCm2ovGpBOJRgrl1RspUOQ==\n-----END EC PRIVATE KEY-----\n"
      }
    },
    "outcomes": [
      {
        "result": "error",
        "message": "missing `clientId`"
      }
    ]
  },
  {
    "documentation": "Failure - Missing dpopKey",
    "configContents": "[profile signin]\nlogin_session = arn:aws:sts::012345678910:assumed-role/Admin/admin\n",
    "cacheContents": {
      "4b0ba8f99f075c0633e122fd73346ce203a3faf18ea0310eb2d29df1bab2e255.json": {
        "accessToken": {
          "accessKeyId": "AKIAIOSFODNN7EXAMPLE",
          "secretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
          "sessionToken": "AQoEXAMPLEH4aoAH0gNCAPyJxz4BlCFFxWNE1OPTgk5TthT+FvwqnKwRcOIfrRh3c/LTo6UDdyJwOOvEVPvLXCrrrUtdnniCEXAMPLE/IvU1dYUg2RVAJBanLiHb4IgRmpRV3zrkuWJOgQs8IZZaIv2BXIa2R4OlgkBN9bkUDNCJiBeb/AXlzBBko7b15fjrBs2+cTQtpZ3CYWFXG8C5zqx37wnOE49mRl/+OtkIKGO7fAE",
          "accountId": "012345678901",
          "expiresAt": "2020-01-01T00:00:00Z"
        },
        "clientId": "arn:aws:signin:::devtools/same-device",
        "refreshToken": "valid_refresh_token_101112",
        "idToken": "eyJraWQiOiI1MzYxMjY2ZS1mNjI5LTQ0ZGQtOTA1My1jYzJkNTM1OTJiOTIiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzM4NCJ9.eyJzdWIiOiJhcm46YXdzOnN0czo6NzIxNzgxNjAzNzU1OmFzc3VtZWQtcm9sZVwvQWRtaW5cL3Nob3ZsaWEtSXNlbmdhcmQiLCJhdWQiOiJhcm46YXdzOnNpZ25pbjo6OmNsaVwvc2FtZS1kZXZpY2UiLCJpc3MiOiJodHRwczpcL1wvc2lnbmluLmF3cy5hbWF6b24uY29tXC9zaWduaW4iLCJzZXNzaW9uX2FybiI6ImFybjphd3M6c3RzOjo3MjE3ODE2MDM3NTU6YXNzdW1lZC1yb2xlXC9BZG1pblwvc2hvdmxpYS1Jc2VuZ2FyZCIsImV4cCI6MTc2MTE2Nzk0NiwiaWF0IjoxNzYxMTY3MDQ2fQ.EzySTg0K11hwQtIYtcBcnNMmX33F6XrVqXsk8WyTWjYcMQxaMnqXebLwBQBCRZha05hZiIZ5xPVCBIt7hZGyymurSfOL72cz69xHUH6u7rwu8vn10UKLHfyKLneKBlmJ"
      }
    },
    "outcomes": [
      {
        "result": "error",
        "message": "missing `dpopKey`"
      }
    ]
  },
  {
    "documentation": "Success - Expired token triggers successful refresh",
    "configContents": "[profile signin]\nlogin_session = arn:aws:sts::012345678910:assumed-role/Admin/admin\n",
    "cacheContents": {
      "4b0ba8f99f075c0633e122fd73346ce203a3faf18ea0310eb2d29df1bab2e255.json": {
        "accessToken": {
          "accessKeyId": "OLDEXPIREDKEY",
          "secretAccessKey": "oldExpiredSecretKey",
          "sessionToken": "oldExpiredSessionToken",
          "accountId": "012345678901",
          "expiresAt": "2020-01-01T00:00:00Z"
        },
        "clientId": "arn:aws:signin:::devtools/same-device",
        "refreshToken": "valid_refresh_token",
        "idToken": "eyJraWQiOiI1MzYxMjY2ZS1mNjI5LTQ0ZGQtOTA1My1jYzJkNTM1OTJiOTIiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzM4NCJ9.eyJzdWIiOiJhcm46YXdzOnN0czo6NzIxNzgxNjAzNzU1OmFzc3VtZWQtcm9sZVwvQWRtaW5cL3Nob3ZsaWEtSXNlbmdhcmQiLCJhdWQiOiJhcm46YXdzOnNpZ25pbjo6OmNsaVwvc2FtZS1kZXZpY2UiLCJpc3MiOiJodHRwczpcL1wvc2lnbmluLmF3cy5hbWF6b24uY29tXC9zaWduaW4iLCJzZXNzaW9uX2FybiI6ImFybjphd3M6c3RzOjo3MjE3ODE2MDM3NTU6YXNzdW1lZC1yb2xlXC9BZG1pblwvc2hvdmxpYS1Jc2VuZ2FyZCIsImV4cCI6MTc2MTE2Nzk0NiwiaWF0IjoxNzYxMTY3MDQ2fQ.EzySTg0K11hwQtIYtcBcnNMmX33F6XrVqXsk8WyTWjYcMQxaMnqXebLwBQBCRZha05hZiIZ5xPVCBIt7hZGyymurSfOL72cz69xHUH6u7rwu8vn10UKLHfyKLneKBlmJ",
        "dpopKey": "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIPt/u8InPLpQeQLJTvVX+sNDzni8vMDMt3Liu+nMBigfoAoGCCqGSM49\nAwEHoUQDQgAEILkGG7rNOnxiIJlMgimY1UPP8eDMFP0DAY6WGjngP4bvTAiUCQ/I\nffut2379uP+OBCm2ovGpBOJRgrl1RspUOQ==\n-----END EC PRIVATE KEY-----\n"
      }
    },
    "mockApiCalls": [
      {
        "request": {
          "tokenInput": {
            "clientId": "arn:aws:signin:::devtools/same-device",
            "refreshToken": "valid_refresh_token",
            "grantType": "refresh_token"
          }
        },
        "response": {
          "tokenOutput": {
            "accessToken": {
              "accessKeyId": "NEWREFRESHEDKEY",
              "secretAccessKey": "newRefreshedSecretKey",
              "sessionToken": "newRefreshedSessionToken"
            },
            "refreshToken": "new_refresh_token",
            "expiresIn": 900
          }
        }
      }
    ],
    "outcomes": [
      {
        "result": "credentials",
        "accessKeyId": "NEWREFRESHEDKEY",
        "secretAccessKey": "newRefreshedSecretKey",
        "sessionToken": "newRefreshedSessionToken",
        "accountId": "012345678901",
        "expiresAt": "2025-11-19T00:15:00Z"
      },
      {
        "result": "cacheContents",
        "4b0ba8f99f075c0633e122fd73346ce203a3faf18ea0310eb2d29df1bab2e255.json": {
          "accessToken": {
            "accessKeyId": "NEWREFRESHEDKEY",
            "secretAccessKey": "newRefreshedSecretKey",
            "sessionToken": "newRefreshedSessionToken",
            "accountId": "012345678901",
            "expiresAt": "2025-11-19T00:15:00Z"
          },
          "clientId": "arn:aws:signin:::devtools/same-device",
          "refreshToken": "new_refresh_token",
          "idToken": "eyJraWQiOiI1MzYxMjY2ZS1mNjI5LTQ0ZGQtOTA1My1jYzJkNTM1OTJiOTIiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzM4NCJ9.eyJzdWIiOiJhcm46YXdzOnN0czo6NzIxNzgxNjAzNzU1OmFzc3VtZWQtcm9sZVwvQWRtaW5cL3Nob3ZsaWEtSXNlbmdhcmQiLCJhdWQiOiJhcm46YXdzOnNpZ25pbjo6OmNsaVwvc2FtZS1kZXZpY2UiLCJpc3MiOiJodHRwczpcL1wvc2lnbmluLmF3cy5hbWF6b24uY29tXC9zaWduaW4iLCJzZXNzaW9uX2FybiI6ImFybjphd3M6c3RzOjo3MjE3ODE2MDM3NTU6YXNzdW1lZC1yb2xlXC9BZG1pblwvc2hvdmxpYS1Jc2VuZ2FyZCIsImV4cCI6MTc2MTE2Nzk0NiwiaWF0IjoxNzYxMTY3MDQ2fQ.EzySTg0K11hwQtIYtcBcnNMmX33F6XrVqXsk8WyTWjYcMQxaMnqXebLwBQBCRZha05hZiIZ5xPVCBIt7hZGyymurSfOL72cz69xHUH6u7rwu8vn10UKLHfyKLneKBlmJ",
          "dpopKey": "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIPt/u8InPLpQeQLJTvVX+sNDzni8vMDMt3Liu+nMBigfoAoGCCqGSM49\nAwEHoUQDQgAEILkGG7rNOnxiIJlMgimY1UPP8eDMFP0DAY6WGjngP4bvTAiUCQ/I\nffut2379uP+OBCm2ovGpBOJRgrl1RspUOQ==\n-----END EC PRIVATE KEY-----\n"
        }
      }
    ]
  },
  {
    "documentation": "Failure - Expired token triggers failed refresh",
    "configContents": "[profile signin]\nlogin_session = arn:aws:sts::012345678910:assumed-role/Admin/admin\n",
    "cacheContents": {
      "4b0ba8f99f075c0633e122fd73346ce203a3faf18ea0310eb2d29df1bab2e255.json": {
        "accessToken": {
          "accessKeyId": "OLDEXPIREDKEY",
          "secretAccessKey": "oldExpiredSecretKey",
          "sessionToken": "oldExpiredSessionToken",
          "accountId": "012345678901",
          "expiresAt": "2020-01-01T00:00:00Z"
        },
        "clientId": "arn:aws:signin:::devtools/same-device",
        "refreshToken": "expired_refresh_token",
        "idToken": "eyJraWQiOiI1MzYxMjY2ZS1mNjI5LTQ0ZGQtOTA1My1jYzJkNTM1OTJiOTIiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzM4NCJ9.eyJzdWIiOiJhcm46YXdzOnN0czo6NzIxNzgxNjAzNzU1OmFzc3VtZWQtcm9sZVwvQWRtaW5cL3Nob3ZsaWEtSXNlbmdhcmQiLCJhdWQiOiJhcm46YXdzOnNpZ25pbjo6OmNsaVwvc2FtZS1kZXZpY2UiLCJpc3MiOiJodHRwczpcL1wvc2lnbmluLmF3cy5hbWF6b24uY29tXC9zaWduaW4iLCJzZXNzaW9uX2FybiI6ImFybjphd3M6c3RzOjo3MjE3ODE2MDM3NTU6YXNzdW1lZC1yb2xlXC9BZG1pblwvc2hvdmxpYS1Jc2VuZ2FyZCIsImV4cCI6MTc2MTE2Nzk0NiwiaWF0IjoxNzYxMTY3MDQ2fQ.EzySTg0K11hwQtIYtcBcnNMmX33F6XrVqXsk8WyTWjYcMQxaMnqXebLwBQBCRZha05hZiIZ5xPVCBIt7hZGyymurSfOL72cz69xHUH6u7rwu8vn10UKLHfyKLneKBlmJ",
        "dpopKey": "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIPt/u8InPLpQeQLJTvVX+sNDzni8vMDMt3Liu+nMBigfoAoGCCqGSM49\nAwEHoUQDQgAEILkGG7rNOnxiIJlMgimY1UPP8eDMFP0DAY6WGjngP4bvTAiUCQ/I\nffut2379uP+OBCm2ovGpBOJRgrl1RspUOQ==\n-----END EC PRIVATE KEY-----\n"
      }
    },
    "mockApiCalls": [
      {
        "request": {
          "tokenInput": {
            "clientId": "arn:aws:signin:::devtools/same-device",
            "refreshToken": "expired_refresh_token",
            "grantType": "refresh_token"
          }
        },
        "responseCode": 400
      }
    ],
    "outcomes": [
      {
        "result": "error",
        "message": "Login token for login-session: arn:aws:sts::012345678910:assumed-role/Admin/admin is expired"
      }
    ]
  }
]
"""
