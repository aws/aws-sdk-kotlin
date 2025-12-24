/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.HttpRequestBuilder
import aws.smithy.kotlin.runtime.http.request.header
import aws.smithy.kotlin.runtime.http.request.url
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.TestConnection
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.net.HostAddress
import aws.smithy.kotlin.runtime.net.HostResolver
import aws.smithy.kotlin.runtime.net.IpAddr
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes

class EcsCredentialsProviderTest {
    private val epoch = Instant.fromIso8601("2020-10-16T03:56:00Z")
    private val expectedExpiration = epoch + 15.minutes
    private val expectedCredentials = Credentials(
        "AKID",
        "test-secret",
        "test-token",
        expectedExpiration,
        "EcsContainer",
    ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_HTTP)

    private fun ecsResponse(accountId: String? = null): HttpResponse {
        val payload = buildJsonObject {
            put("Code", "Success")
            put("LastUpdated", "2021-09-17T20:57:08Z")
            put("Type", "AWS-HMAC")
            put("AccessKeyId", "AKID")
            put("SecretAccessKey", "test-secret")
            put("Token", "test-token")
            put("Expiration", expectedExpiration.format(TimestampFormat.ISO_8601))
            if (accountId != null) {
                put("AccountId", accountId)
            }
        }.toString().encodeToByteArray()

        return HttpResponse(HttpStatusCode.OK, Headers.Empty, HttpBody.fromBytes(payload))
    }

    private fun errorResponse(
        statusCode: HttpStatusCode = HttpStatusCode.BadRequest,
        headers: Headers = Headers.Empty,
        body: String = "",
    ): HttpResponse =
        HttpResponse(statusCode, headers, HttpBody.fromBytes(body.encodeToByteArray()))

    private fun ecsRequest(url: String, authToken: String? = null): HttpRequest {
        val resolvedUrl = Url.parse(url)
        val builder = HttpRequestBuilder().apply {
            method = HttpMethod.GET
            url(resolvedUrl)
            header("Host", resolvedUrl.host.toString())
            header("Accept", "application/json")
            header("Accept-Encoding", "identity")
            if (authToken != null) {
                header("Authorization", authToken)
            }
        }
        return builder.build()
    }

    /**
     * Mock resolver that always resolves to loopback address
     */
    private object LocalHostResolver : HostResolver {
        override suspend fun resolve(hostname: String): List<HostAddress> =
            listOf(
                HostAddress(
                    "localhost",
                    IpAddr.parse("127.0.0.1"),
                ),
            )
        override fun reportFailure(addr: HostAddress) { }
        override fun purgeCache(addr: HostAddress?) { }
    }

    @Test
    fun testRelativeUri() = runTest {
        val engine = buildTestConnection {
            expect(
                ecsRequest("http://169.254.170.2/relative?foo=bar"),
                ecsResponse(),
            )
        }

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsRelativeUri.envVar to "/relative?foo=bar"),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        val actual = provider.resolve()
        assertEquals(expectedCredentials, actual)
        engine.assertRequests()
    }

    @Test
    fun testFullUri() = runTest {
        val uri = "http://127.0.0.1/full?foo=bar"
        val engine = buildTestConnection {
            expect(
                ecsRequest(uri),
                ecsResponse(),
            )
        }

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsFullUri.envVar to uri),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        val actual = provider.resolve()
        assertEquals(expectedCredentials, actual)
        engine.assertRequests()
    }

    @Test
    fun testNonLocalFullUri() = runTest {
        val uri = "http://amazonaws.com/full"
        val engine = TestConnection()

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsFullUri.envVar to uri),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        assertFailsWith<ProviderConfigurationException> {
            provider.resolve()
        }.message.shouldContain("The container credentials full URI (http://amazonaws.com/full) is specified via a hostname whose IP address(es) do not resolve to the loopback device.")
    }

    @Test
    fun testLocalFullUri() = runTest {
        val uri = "http://localhost"
        val engine = buildTestConnection {
            expect(
                ecsRequest(uri),
                ecsResponse(),
            )
        }

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsFullUri.envVar to uri),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine, LocalHostResolver)
        val actual = provider.resolve()
        assertEquals(expectedCredentials, actual)
        engine.assertRequests()
    }

    @Test
    fun testNonexistentFullUri() = runTest {
        val uri = "http://amazonaws.net/full"
        val engine = TestConnection()

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsFullUri.envVar to uri),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        assertFailsWith<ProviderConfigurationException> {
            provider.resolve()
        }.message.shouldContain("The container credentials full URI (http://amazonaws.net/full) is specified via a hostname whose IP address(es) could not be resolved.")
    }

    @Test
    fun testNonLocalFullUriHttps() = runTest {
        val uri = "https://amazonaws.com/full"
        val engine = buildTestConnection {
            expect(
                ecsRequest(uri),
                ecsResponse(),
            )
        }

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsFullUri.envVar to uri),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        val actual = provider.resolve()
        assertEquals(expectedCredentials, actual)
        engine.assertRequests()
    }

    @Test
    fun testFullUriEcs() = runTest {
        val uri = "https://169.254.170.2/full"
        val engine = buildTestConnection {
            expect(
                ecsRequest(uri),
                ecsResponse(),
            )
        }

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsFullUri.envVar to uri),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        val actual = provider.resolve()
        assertEquals(expectedCredentials, actual)
        engine.assertRequests()
    }

    @Test
    fun testFullUriEksV4() = runTest {
        val uri = "https://169.254.170.23/full"
        val engine = buildTestConnection {
            expect(
                ecsRequest(uri),
                ecsResponse(),
            )
        }

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsFullUri.envVar to uri),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        val actual = provider.resolve()
        assertEquals(expectedCredentials, actual)
        engine.assertRequests()
    }

    @Test
    fun testFullUriEksV6() = runTest {
        val uri = "https://[fd00:ec2:0::23]/full"
        val engine = buildTestConnection {
            expect(
                ecsRequest(uri),
                ecsResponse(),
            )
        }

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsFullUri.envVar to uri),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        val actual = provider.resolve()
        assertEquals(expectedCredentials, actual)
        engine.assertRequests()
    }

    @Test
    fun testFullUriForbiddenV4() = runTest {
        val uri = "http://192.168.1.1/full"
        val engine = TestConnection()

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsFullUri.envVar to uri),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        assertFailsWith<ProviderConfigurationException> {
            provider.resolve()
        }.message.shouldContain("The container credentials full URI (http://192.168.1.1/full) has an invalid host.")
    }

    @Test
    fun testFullUriForbiddenV6() = runTest {
        val uri = "http://[fd00:0:ec2::23]/full"
        val engine = TestConnection()

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsFullUri.envVar to uri),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        assertFailsWith<ProviderConfigurationException> {
            provider.resolve()
        }.message.shouldContain("The container credentials full URI (http://[fd00:0:ec2::23]/full) has an invalid host.")
    }

    @Test
    fun testNoUri() = runTest {
        val engine = TestConnection()
        val testPlatform = TestPlatformProvider()

        val provider = EcsCredentialsProvider(testPlatform, engine)
        assertFailsWith<ProviderConfigurationException> {
            provider.resolve()
        }.message.shouldContain("Container credentials URI not set")
    }

    @Test
    fun testAuthToken() = runTest {
        val token = "auth-token"
        val engine = buildTestConnection {
            expect(
                ecsRequest("http://169.254.170.2/relative", token),
                ecsResponse(),
            )
        }

        val testPlatform = TestPlatformProvider(
            env = mapOf(
                AwsSdkSetting.AwsContainerCredentialsRelativeUri.envVar to "/relative",
                AwsSdkSetting.AwsContainerAuthorizationToken.envVar to token,
            ),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        val actual = provider.resolve()
        assertEquals(expectedCredentials, actual)
        engine.assertRequests()
    }

    @Test
    fun testAuthTokenFile() = runTest {
        val tokenFile = "/path/to/token"
        val token = "auth-token"
        val staticToken = "static-auth-token"
        val engine = buildTestConnection {
            expect(
                ecsRequest("http://169.254.170.2/relative", token),
                ecsResponse(),
            )
        }

        val testPlatform = TestPlatformProvider(
            env = mapOf(
                AwsSdkSetting.AwsContainerCredentialsRelativeUri.envVar to "/relative",
                AwsSdkSetting.AwsContainerAuthorizationTokenFile.envVar to tokenFile,
                AwsSdkSetting.AwsContainerAuthorizationToken.envVar to staticToken, // should be ignored
            ),
            fs = mapOf(
                tokenFile to token,
            ),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        val actual = provider.resolve()
        assertEquals(expectedCredentials, actual)
        engine.assertRequests()
    }

    @Test
    fun testAuthTokenFileInvalid() = runTest {
        val tokenFile = "/path/to/token"
        val engine = TestConnection()

        val testPlatform = TestPlatformProvider(
            env = mapOf(
                AwsSdkSetting.AwsContainerCredentialsRelativeUri.envVar to "/relative",
                AwsSdkSetting.AwsContainerAuthorizationTokenFile.envVar to tokenFile,
            ),
            fs = mapOf(),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        assertFailsWith<CredentialsProviderException> {
            provider.resolve()
        }.message.shouldContain("Could not read token file.")
    }

    @Test
    fun testAuthTokenIllegal() = runTest {
        val token = "auth\r\ntoken"
        val engine = TestConnection()

        val testPlatform = TestPlatformProvider(
            env = mapOf(
                AwsSdkSetting.AwsContainerCredentialsRelativeUri.envVar to "/relative",
                AwsSdkSetting.AwsContainerAuthorizationToken.envVar to token,
            ),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        assertFailsWith<CredentialsProviderException> {
            provider.resolve()
        }.message.shouldContain("Token contains illegal line break sequence.")
    }

    @Test
    fun testAuthTokenFileIllegal() = runTest {
        val tokenFile = "/path/to/token"
        val token = "auth\r\ntoken"
        val engine = TestConnection()

        val testPlatform = TestPlatformProvider(
            env = mapOf(
                AwsSdkSetting.AwsContainerCredentialsRelativeUri.envVar to "/relative",
                AwsSdkSetting.AwsContainerAuthorizationTokenFile.envVar to tokenFile,
            ),
            fs = mapOf(
                tokenFile to token,
            ),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        assertFailsWith<CredentialsProviderException> {
            provider.resolve()
        }.message.shouldContain("Token contains illegal line break sequence.")
    }

    @Test
    fun testErrorResponse() = runTest {
        val engine = buildTestConnection {
            expect(
                ecsRequest("http://169.254.170.2/relative"),
                errorResponse(),
            )
        }

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsRelativeUri.envVar to "/relative"),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        assertFailsWith<CredentialsProviderException> {
            provider.resolve()
        }.message.shouldContain("Error retrieving credentials from container service: HTTP 400: Bad Request")

        engine.assertRequests()
    }

    @Test
    fun testThrottledErrorResponse() = runTest {
        val engine = buildTestConnection {
            repeat(StandardRetryStrategy.Config.DEFAULT_MAX_ATTEMPTS) {
                expect(
                    ecsRequest("http://169.254.170.2/relative"),
                    errorResponse(statusCode = HttpStatusCode.TooManyRequests),
                )
            }
        }

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsRelativeUri.envVar to "/relative"),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        assertFailsWith<CredentialsProviderException> {
            provider.resolve()
        }.message.shouldContain("Error retrieving credentials from container service: HTTP 429: Too Many Requests")

        engine.assertRequests()
    }

    @Test
    fun testJsonErrorResponse() = runTest {
        val engine = buildTestConnection {
            expect(
                ecsRequest("http://169.254.170.2/relative"),
                errorResponse(
                    HttpStatusCode.BadRequest,
                    Headers { append("Content-Type", "application/json") },
                    """
                        {
                            "Code": "failed",
                            "Message": "request was malformed"
                        }
                    """,
                ),
            )
        }

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsRelativeUri.envVar to "/relative"),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        assertFailsWith<CredentialsProviderException> {
            provider.resolve()
        }.message.shouldContain("Error retrieving credentials from container service: code=failed; message=request was malformed")

        engine.assertRequests()
    }

    @Test
    fun testThrottledJsonErrorResponse() = runTest {
        val engine = buildTestConnection {
            repeat(StandardRetryStrategy.Config.DEFAULT_MAX_ATTEMPTS) {
                expect(
                    ecsRequest("http://169.254.170.2/relative"),
                    errorResponse(
                        HttpStatusCode.TooManyRequests,
                        Headers { append("Content-Type", "application/json") },
                        """
                        {
                            "Code": "failed",
                            "Message": "too many requests"
                        }
                    """,
                    ),
                )
            }
        }

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsRelativeUri.envVar to "/relative"),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        assertFailsWith<CredentialsProviderException> {
            provider.resolve()
        }.message.shouldContain("Error retrieving credentials from container service: code=failed; message=too many requests")

        engine.assertRequests()
    }

    @Test
    fun test5XXErrorResponse() = runTest {
        val engine = buildTestConnection {
            repeat(StandardRetryStrategy.Config.DEFAULT_MAX_ATTEMPTS) {
                expect(
                    ecsRequest("http://169.254.170.2/relative"),
                    errorResponse(
                        HttpStatusCode.BadGateway,
                    ),
                )
            }
        }

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsRelativeUri.envVar to "/relative"),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        assertFailsWith<CredentialsProviderException> {
            provider.resolve()
        }.message.shouldContain("Error retrieving credentials from container service: HTTP 502: Bad Gateway")

        engine.assertRequests()
    }

    @Test
    fun testAccountIdResolves() = runTest {
        val engine = buildTestConnection {
            expect(
                ecsRequest("http://169.254.170.2/relative?foo=bar"),
                ecsResponse("12345"),
            )
        }

        val testPlatform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsContainerCredentialsRelativeUri.envVar to "/relative?foo=bar"),
        )

        val provider = EcsCredentialsProvider(testPlatform, engine)
        val actual = provider.resolve()
        val expected = credentials(
            "AKID",
            "test-secret",
            "test-token",
            expectedExpiration,
            "EcsContainer",
            "12345",
        ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_HTTP)
        assertEquals(expected, actual)
        engine.assertRequests()
    }
}
