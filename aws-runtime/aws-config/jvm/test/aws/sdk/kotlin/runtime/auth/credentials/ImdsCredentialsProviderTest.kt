/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.imds.*
import aws.sdk.kotlin.runtime.config.imds.DEFAULT_TOKEN_TTL_SECONDS
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
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
    @Test
    fun testDontRefreshUntilNextRefreshTimeHasPassed() = runTest {
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
                        "AccessKeyId" : "NEWCREDENTIALS",
                        "SecretAccessKey" : "shhh",
                        "Token" : "IQote///test",
                        "Expiration" : "2022-10-05T03:31:56Z"
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

        val first = provider.resolve()
        testClock.advance(20.minutes) // 20 minutes later, we should try to refresh the expired credentials
        val second = provider.resolve()

        coVerify(exactly = 2) {
            client.get(any())
        }

        // make sure we did not just serve the previous credentials
        assertNotEquals(first, second)
    }
}
