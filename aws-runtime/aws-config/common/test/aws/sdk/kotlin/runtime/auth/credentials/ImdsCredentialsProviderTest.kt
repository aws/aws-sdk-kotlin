/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.imds.*
import aws.sdk.kotlin.runtime.config.imds.DEFAULT_TOKEN_TTL_SECONDS
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineBase
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineConfig
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.TestEngine
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.io.IOException
import aws.smithy.kotlin.runtime.net.Host
import aws.smithy.kotlin.runtime.net.Scheme
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import aws.smithy.kotlin.runtime.time.fromEpochMilliseconds
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class ImdsCredentialsProviderTest {

    private val ec2MetadataDisabledPlatform = TestPlatformProvider(
        env = mapOf(AwsSdkSetting.AwsEc2MetadataDisabled.envVar to "true"),
    )
    private val ec2MetadataEnabledPlatform = TestPlatformProvider()

    @Test
    fun testImdsDisabled() = runTest {
        val platform = ec2MetadataDisabledPlatform
        val provider = ImdsCredentialsProvider(platformProvider = platform)
        assertFailsWith<CredentialsNotLoadedException> {
            provider.resolve()
        }.message.shouldContain("AWS EC2 metadata is explicitly disabled; credentials not loaded")
    }

    @Test
    fun testSuccess() = runTest {
        val testClock = ManualClock(Instant.fromEpochMilliseconds(Instant.now().epochMilliseconds))
        val expiration0 = Instant.fromEpochMilliseconds(testClock.now().epochMilliseconds)
        val expiration1 = expiration0 + 2.seconds

        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A"),
            )
            expect(
                imdsRequest("http://169.254.169.254/latest/meta-data/iam/security-credentials/", "TOKEN_A"),
                imdsResponse("imds-test-role"),
            )
            expect(
                imdsRequest(
                    "http://169.254.169.254/latest/meta-data/iam/security-credentials/imds-test-role",
                    "TOKEN_A",
                ),
                imdsResponse(
                    """
                    {
                        "Code" : "Success",
                        "LastUpdated" : "2021-09-17T20:57:08Z",
                        "Type" : "AWS-HMAC",
                        "AccessKeyId" : "ASIARTEST0",
                        "SecretAccessKey" : "xjtest0",
                        "Token" : "IQote///test0",
                        "Expiration" : "$expiration0"
                    }
                """,
                ),
            )

            // verify that profile is re-retrieved after credentials expiration
            expect(
                imdsRequest("http://169.254.169.254/latest/meta-data/iam/security-credentials/", "TOKEN_A"),
                imdsResponse("imds-test-role-2"),
            )
            expect(
                imdsRequest(
                    "http://169.254.169.254/latest/meta-data/iam/security-credentials/imds-test-role-2",
                    "TOKEN_A",
                ),
                imdsResponse(
                    """
                    {
                        "Code" : "Success",
                        "LastUpdated" : "2021-09-17T20:57:08Z",
                        "Type" : "AWS-HMAC",
                        "AccessKeyId" : "ASIARTEST1",
                        "SecretAccessKey" : "xjtest1",
                        "Token" : "IQote///test1",
                        "Expiration" : "$expiration1"
                    }
                """,
                ),
            )
        }

        val client = ImdsClient {
            engine = connection
            clock = testClock
        }

        val provider = ImdsCredentialsProvider(
            client = lazyOf(client),
            clock = testClock,
            platformProvider = ec2MetadataEnabledPlatform,
        )

        val actual0 = provider.resolve()
        val expected0 = Credentials(
            "ASIARTEST0",
            "xjtest0",
            "IQote///test0",
            expiration0,
            "IMDSv2",
        ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_IMDS)
        assertEquals(expected0, actual0)

        testClock.advance(1.seconds)

        val actual1 = provider.resolve()
        val expected1 = Credentials(
            "ASIARTEST1",
            "xjtest1",
            "IQote///test1",
            expiration1,
            "IMDSv2",
        ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_IMDS)
        assertEquals(expected1, actual1)

        connection.assertRequests()
    }

    @Test
    fun testSuccessProfileOverride() = runTest {
        val testClock = ManualClock()
        val expiration = Instant.fromEpochMilliseconds(testClock.now().epochMilliseconds)

        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A"),
            )
            // no request for profile, go directly to retrieving role credentials
            expect(
                imdsRequest(
                    "http://169.254.169.254/latest/meta-data/iam/security-credentials/imds-test-role",
                    "TOKEN_A",
                ),
                imdsResponse(
                    """
                    {
                        "Code" : "Success",
                        "LastUpdated" : "2021-09-17T20:57:08Z",
                        "Type" : "AWS-HMAC",
                        "AccessKeyId" : "ASIARTEST",
                        "SecretAccessKey" : "xjtest",
                        "Token" : "IQote///test",
                        "Expiration" : "$expiration"
                    }
                """,
                ),
            )
        }

        val client = ImdsClient {
            engine = connection
            clock = testClock
        }

        val provider = ImdsCredentialsProvider(
            profileOverride = "imds-test-role",
            client = lazyOf(client),
            clock = testClock,
            platformProvider = ec2MetadataEnabledPlatform,
        )

        val actual = provider.resolve()
        val expected = Credentials(
            "ASIARTEST",
            "xjtest",
            "IQote///test",
            expiration,
            "IMDSv2",
        ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_IMDS)
        assertEquals(expected, actual)

        connection.assertRequests()
    }

    @Test
    fun testTokenFailure() = runTest {
        // when attempting to retrieve initial token, IMDS replied with 403, indicating IMDS is disabled or not allowed through permissions
        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                HttpResponse(HttpStatusCode.Forbidden, Headers.Empty, HttpBody.Empty),
            )
        }

        val testClock = ManualClock()
        val client = ImdsClient {
            engine = connection
            clock = testClock
        }

        val provider = ImdsCredentialsProvider(
            client = lazyOf(client),
            clock = testClock,
            platformProvider = ec2MetadataEnabledPlatform,
        )

        val ex = assertFailsWith<CredentialsProviderException> {
            provider.resolve()
        }
        ex.message.shouldContain("failed to load instance profile")
        assertIs<EC2MetadataError>(ex.cause)
        ex.cause!!.message.shouldContain("Request forbidden")

        connection.assertRequests()
    }

    @Test
    fun testNoInstanceProfileConfigured() = runTest {
        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A"),
            )
            expect(
                imdsRequest("http://169.254.169.254/latest/meta-data/iam/security-credentials/", "TOKEN_A"),
                HttpResponse(
                    HttpStatusCode.NotFound,
                    Headers.Empty,
                    HttpBody.fromBytes(
                        """<?xml version="1.0" encoding="iso-8859-1"?>
                        <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
                                "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
                        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
                         <head>
                          <title>404 - Not Found</title>
                         </head>
                         <body>
                          <h1>404 - Not Found</h1>
                         </body>
                        </html>
                        """.trimIndent().encodeToByteArray(),
                    ),
                ),
            )
        }

        val testClock = ManualClock()
        val client = ImdsClient {
            engine = connection
            clock = testClock
        }

        val provider = ImdsCredentialsProvider(
            client = lazyOf(client),
            clock = testClock,
            platformProvider = ec2MetadataEnabledPlatform,
        )

        assertFailsWith<CredentialsProviderException> {
            provider.resolve()
        }.message.shouldContain("failed to load instance profile")

        connection.assertRequests()
    }

    // SDK can send a request if expired credentials are available.
    // If the credentials provider can return expired credentials, that means the SDK can use them,
    // because no other checks are done before using the credentials.
    @Test
    fun testCanReturnExpiredCredentials() = runTest {
        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A"),
            )
            expect(
                imdsRequest(
                    "http://169.254.169.254/latest/meta-data/iam/security-credentials/imds-test-role",
                    "TOKEN_A",
                ),
                imdsResponse(
                    """
                    {
                        "Code" : "Success",
                        "LastUpdated" : "2021-09-17T20:57:08Z",
                        "Type" : "AWS-HMAC",
                        "AccessKeyId" : "ASIARTEST",
                        "SecretAccessKey" : "xjtest",
                        "Token" : "IQote///test",
                        "Expiration" : "2021-09-18T03:31:56Z"
                    }
                """,
                ),
            )
        }

        val testClock = ManualClock()
        val client = ImdsClient {
            engine = connection
            clock = testClock
        }

        val provider = ImdsCredentialsProvider(
            profileOverride = "imds-test-role",
            client = lazyOf(client),
            clock = testClock,
            platformProvider = ec2MetadataEnabledPlatform,
        )

        val actual = provider.resolve()

        val expected = Credentials(
            accessKeyId = "ASIARTEST",
            secretAccessKey = "xjtest",
            sessionToken = "IQote///test",
            expiration = Instant.fromEpochSeconds(1631935916),
            providerName = "IMDSv2",
        ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_IMDS)

        assertEquals(expected, actual)

        connection.assertRequests()
    }

    @Test
    fun testUsesPreviousCredentialsOnReadTimeout() = runTest {
        val testClock = ManualClock()

        // this engine throws read timeout exceptions for any requests after the initial one
        // (i.e allow 1 TTL token and 1 credentials request)
        val readTimeoutEngine = object : HttpClientEngineBase("readTimeout") {
            var successfulCallCount = 0

            override val config: HttpClientEngineConfig = HttpClientEngineConfig.Default

            override suspend fun roundTrip(context: ExecutionContext, request: HttpRequest): HttpCall {
                if (successfulCallCount >= 2) {
                    throw IOException()
                } else {
                    successfulCallCount += 1

                    return when (successfulCallCount) {
                        1 -> HttpCall(
                            tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                            tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A"),
                            testClock.now(),
                            testClock.now(),
                        )

                        else -> HttpCall(
                            imdsRequest(
                                "http://169.254.169.254/latest/meta-data/iam/security-credentials/imds-test-role",
                                "TOKEN_A",
                            ),
                            imdsResponse(
                                """
                            {
                                "Code" : "Success",
                                "LastUpdated" : "2021-09-17T20:57:08Z",
                                "Type" : "AWS-HMAC",
                                "AccessKeyId" : "ASIARTEST",
                                "SecretAccessKey" : "xjtest",
                                "Token" : "IQote///test",
                                "Expiration" : "2021-09-18T03:31:56Z"
                            }""",
                            ),
                            testClock.now(),
                            testClock.now(),
                        )
                    }
                }
            }
        }

        val client = ImdsClient {
            engine = readTimeoutEngine
            clock = testClock
        }

        val previousCredentials = Credentials(
            accessKeyId = "ASIARTEST",
            secretAccessKey = "xjtest",
            sessionToken = "IQote///test",
            expiration = Instant.fromEpochSeconds(1631935916),
            providerName = "IMDSv2",
        ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_IMDS)

        val provider = ImdsCredentialsProvider(
            profileOverride = "imds-test-role",
            client = lazyOf(client),
            clock = testClock,
            platformProvider = ec2MetadataEnabledPlatform,
        )

        // call the engine the first time to get a proper credentials response from IMDS
        val credentials = provider.resolve()
        assertEquals(credentials, previousCredentials)

        // call it again and get a read timeout exception from the engine
        val newCredentials = provider.resolve()

        // should cause provider to return the previously-served credentials
        assertEquals(newCredentials, previousCredentials)
    }

    @Test
    fun testThrowsExceptionOnReadTimeoutWhenMissingPreviousCredentials() = runTest {
        val readTimeoutEngine = TestEngine { _, _ -> throw IOException() }
        val testClock = ManualClock()

        val client = ImdsClient {
            engine = readTimeoutEngine
            clock = testClock
        }

        val provider = ImdsCredentialsProvider(
            profileOverride = "imds-test-role",
            client = lazyOf(client),
            clock = testClock,
            platformProvider = ec2MetadataEnabledPlatform,
        )

        // a read timeout should cause an exception to be thrown, because we have no previous credentials to re-serve
        assertFailsWith<CredentialsProviderException> {
            provider.resolve()
        }
    }

    @Test
    fun testUsesPreviousCredentialsOnServerError() = runTest {
        val testClock = ManualClock()

        // this engine returns 500 errors for any requests after the initial one (i.e allow 1 TTL token and 1 credentials request)
        val internalServerErrorEngine = object : HttpClientEngineBase("internalServerError") {
            var successfulCallCount = 0

            override val config: HttpClientEngineConfig = HttpClientEngineConfig.Default

            override suspend fun roundTrip(context: ExecutionContext, request: HttpRequest): HttpCall {
                if (successfulCallCount >= 2) {
                    return HttpCall(
                        HttpRequest(
                            HttpMethod.GET,
                            Url {
                                scheme = Scheme.HTTP
                                host = Host.parse("test")
                                path.encoded = "/path/foo/bar"
                            },
                            Headers.Empty,
                            HttpBody.Empty,
                        ),
                        HttpResponse(HttpStatusCode.InternalServerError, Headers.Empty, HttpBody.Empty),
                        testClock.now(),
                        testClock.now(),
                    )
                } else {
                    successfulCallCount += 1

                    return when (successfulCallCount) {
                        1 -> HttpCall(
                            tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                            tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A"),
                            testClock.now(),
                            testClock.now(),
                        )

                        else -> HttpCall(
                            imdsRequest(
                                "http://169.254.169.254/latest/meta-data/iam/security-credentials/imds-test-role",
                                "TOKEN_A",
                            ),
                            imdsResponse(
                                """
                            {
                                "Code" : "Success",
                                "LastUpdated" : "2021-09-17T20:57:08Z",
                                "Type" : "AWS-HMAC",
                                "AccessKeyId" : "ASIARTEST",
                                "SecretAccessKey" : "xjtest",
                                "Token" : "IQote///test",
                                "Expiration" : "2021-09-18T03:31:56Z"
                            }""",
                            ),
                            testClock.now(),
                            testClock.now(),
                        )
                    }
                }
            }
        }

        val client = ImdsClient {
            engine = internalServerErrorEngine
            clock = testClock
        }

        val previousCredentials = Credentials(
            accessKeyId = "ASIARTEST",
            secretAccessKey = "xjtest",
            sessionToken = "IQote///test",
            expiration = Instant.fromEpochSeconds(1631935916),
            providerName = "IMDSv2",
        ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_IMDS)

        val provider = ImdsCredentialsProvider(
            profileOverride = "imds-test-role",
            client = lazyOf(client),
            clock = testClock,
            platformProvider = ec2MetadataEnabledPlatform,
        )

        // call the engine the first time to get a proper credentials response from IMDS
        val credentials = provider.resolve()
        assertEquals(previousCredentials, credentials)

        // call it again and get a 500 error from the engine
        val newCredentials = provider.resolve()

        // should cause provider to return the previously-served credentials
        assertEquals(newCredentials, previousCredentials)
    }

    @Test
    fun testThrowsExceptionOnServerErrorWhenMissingPreviousCredentials() = runTest {
        val testClock = ManualClock()

        // this engine just returns 500 errors
        val internalServerErrorEngine = TestEngine { _, _ ->
            HttpCall(
                HttpRequest(
                    HttpMethod.GET,
                    Url {
                        scheme = Scheme.HTTP
                        host = Host.parse("test")
                        path.encoded = "/path/foo/bar"
                    },
                    Headers.Empty,
                    HttpBody.Empty,
                ),
                HttpResponse(HttpStatusCode.InternalServerError, Headers.Empty, HttpBody.Empty),
                testClock.now(),
                testClock.now(),
            )
        }

        val client = ImdsClient {
            engine = internalServerErrorEngine
            clock = testClock
        }

        val provider = ImdsCredentialsProvider(
            profileOverride = "imds-test-role",
            client = lazyOf(client),
            clock = testClock,
            platformProvider = ec2MetadataEnabledPlatform,
        )

        assertFailsWith<CredentialsProviderException> {
            provider.resolve()
        }
    }
}
