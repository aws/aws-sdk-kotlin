/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.imds.*
import aws.sdk.kotlin.runtime.config.imds.DEFAULT_TOKEN_TTL_SECONDS
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes

class ImdsCredentialsProviderTestJvm {
    private val ec2MetadataEnabledPlatform = TestPlatformProvider.of()

    // FIXME Refactor mocking for KMP
    // SDK can perform 3 successive requests with expired credentials. IMDS must only be called once.
    @Test
    fun testSuccessiveRequestsOnlyCallIMDSOnce() = runTest {
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

        val client = spyk(
            ImdsClient {
                engine = connection
                clock = testClock
            },
        )

        val provider = ImdsCredentialsProvider(
            profileOverride = "imds-test-role",
            client = lazyOf(client),
            clock = testClock,
            platformProvider = ec2MetadataEnabledPlatform,
        )

        // call resolve 3 times
        repeat(3) {
            provider.resolve()
        }

        // make sure ImdsClient only gets called once
        coVerify(exactly = 1) {
            client.get(any())
        }
    }

    // FIXME Refactor mocking for KMP
    // A resolution inside the refresh window is served from the cache; one past it goes back to IMDS and returns
    // what IMDS vended.
    @Test
    fun testDontRefreshUntilNextRefreshTimeHasPassed() = runTest {
        // Pinned so the expirations below are ahead of the clock: a set that is already expired when it arrives is an
        // availability signal rather than a refresh, and is not what this case is about.
        val testClock = ManualClock(Instant.fromIso8601("2021-09-17T20:57:08Z"))

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
                        "Expiration" : "2021-09-17T21:27:08Z"
                    }
                """,
                ),
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
                        "LastUpdated" : "2021-09-17T21:27:08Z",
                        "Type" : "AWS-HMAC",
                        "AccessKeyId" : "NEWCREDENTIALS",
                        "SecretAccessKey" : "shhh",
                        "Token" : "IQote///test",
                        "Expiration" : "2021-09-17T22:57:08Z"
                    }
                """,
                ),
            )
        }

        val client = spyk(
            ImdsClient {
                engine = connection
                clock = testClock
            },
        )

        val provider = ImdsCredentialsProvider(
            profileOverride = "imds-test-role",
            client = lazyOf(client),
            clock = testClock,
            platformProvider = ec2MetadataEnabledPlatform,
        )

        val first = provider.resolve()

        // A 30 minute lifetime puts the advisory deadline 15 minutes out, so this resolution is still inside the
        // window and must not go back to IMDS.
        testClock.advance(5.minutes)
        assertEquals(first, provider.resolve())
        coVerify(exactly = 1) { client.get(any()) }

        // Now past the deadline, so the next resolution refreshes.
        testClock.advance(25.minutes)
        val second = provider.resolve()

        coVerify(exactly = 2) {
            client.get(any())
        }

        // make sure we did not just serve the previous credentials
        assertNotEquals(first, second)
        assertEquals("NEWCREDENTIALS", second.accessKeyId)
    }
}
