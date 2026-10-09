/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.StsClient
import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.assumeRole
import aws.smithy.kotlin.runtime.ServiceException
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsRefreshBehavior
import aws.smithy.kotlin.runtime.auth.awscredentials.resilientlyCached
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.RequestComparands
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.hours

/**
 * End to end through a generated client: a request rejected with `ExpiredToken` is retried only when the provider that
 * resolved its credentials caches them, and the retry is signed with refreshed credentials.
 */
class RejectedCredentialsRetryTest {
    private fun expiredToken() = HttpResponse(
        HttpStatusCode.BadRequest,
        Headers.Empty,
        HttpBody.fromBytes(
            """
            <ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
              <Error><Type>Sender</Type><Code>ExpiredToken</Code><Message>The security token included in the request is expired</Message></Error>
              <RequestId>err</RequestId>
            </ErrorResponse>
            """.trimIndent().encodeToByteArray(),
        ),
    )

    private class CountingSource : CredentialsProvider {
        var calls = 0

        override suspend fun resolve(attributes: Attributes): Credentials {
            calls++
            return credentials(
                accessKeyId = "AKID$calls",
                secretAccessKey = "secret",
                sessionToken = "token",
                expiration = Instant.now() + 2.hours,
                refreshBehavior = CredentialsRefreshBehavior.RefreshableWithStaticStability,
            )
        }
    }

    private fun signingKeys(requests: List<RequestComparands>): List<String> = requests.map { request ->
        Regex("Credential=([^/]+)/").find(request.actual.headers["Authorization"]!!)!!.groupValues[1]
    }

    @Test
    fun testARejectedRequestIsRetriedWithRefreshedCredentialsFromACachingProvider() = runTest {
        val engine = buildTestConnection {
            expect(expiredToken())
            expect(StsTestUtils.stsResponse())
        }
        val source = CountingSource()

        StsClient {
            region = "us-east-1"
            credentialsProvider = source.resilientlyCached()
            httpClient = engine
        }.use { client ->
            client.assumeRole {
                roleArn = StsTestUtils.ARN
                roleSessionName = "session"
            }
        }

        assertEquals(listOf("AKID1", "AKID2"), signingKeys(engine.requests()))
    }

    @Test
    fun testARejectedRequestIsNotRetriedWithCredentialsThatCannotBeRefreshed() = runTest {
        val engine = buildTestConnection {
            expect(expiredToken())
            expect(StsTestUtils.stsResponse())
        }

        StsClient {
            region = "us-east-1"
            credentialsProvider = StaticCredentialsProvider(credentials("AKID", "secret", "token"))
            httpClient = engine
        }.use { client ->
            assertFailsWith<ServiceException> {
                client.assumeRole {
                    roleArn = StsTestUtils.ARN
                    roleSessionName = "session"
                }
            }
        }

        assertEquals(listOf("AKID"), signingKeys(engine.requests()))
    }
}
