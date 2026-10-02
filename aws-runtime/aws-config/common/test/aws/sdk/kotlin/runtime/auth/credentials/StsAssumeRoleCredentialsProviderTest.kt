/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.model.RegionDisabledException
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.util.testAttributes
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.auth.awscredentials.copy
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.CallAsserter
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.net.Host
import aws.smithy.kotlin.runtime.time.Instant
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.hours

class StsAssumeRoleCredentialsProviderTest {
    private val sourceProvider = StaticCredentialsProvider {
        accessKeyId = "AKID"
        secretAccessKey = "secret"
    }

    @Test
    fun testSuccess() = runTest {
        val expectedBody = buildMap {
            put("Action", "AssumeRole")
            put("Version", "2011-06-15")
            put("DurationSeconds", "900")
            put("RoleArn", StsTestUtils.ARN)
            put("RoleSessionName", StsTestUtils.SESSION_NAME)
        }

        val testEngine = buildTestConnection {
            expect(StsTestUtils.stsRequest(expectedBody), StsTestUtils.stsResponse())
        }

        val provider = StsAssumeRoleCredentialsProvider(
            bootstrapCredentialsProvider = sourceProvider,
            roleArn = StsTestUtils.ARN,
            roleSessionName = StsTestUtils.SESSION_NAME,
            httpClient = testEngine,
        )

        val actual = provider.resolve()
        val expected = StsTestUtils.CREDENTIALS.copy(
            attributes = testAttributes(
                StsTestUtils.CREDENTIALS.attributes,
                AwsBusinessMetric.Credentials.CREDENTIALS_STS_ASSUME_ROLE,
            ),
        )
        assertEquals(expected, actual)

        testEngine.assertRequests(CallAsserter.MatchingBodies)
    }

    @Test
    fun testSuccessWithAdditionalParams() = runTest {
        val expectedBody = buildMap {
            put("Action", "AssumeRole")
            put("Version", "2011-06-15")
            put("DurationSeconds", "900")
            StsTestUtils.POLICY_ARNS.forEachIndexed { i, arn ->
                put("PolicyArns.member.${i + 1}.arn", arn)
            }
            put("RoleArn", StsTestUtils.ARN)
            put("RoleSessionName", StsTestUtils.SESSION_NAME)
            StsTestUtils.TAGS.entries.forEachIndexed { i, (key, value) ->
                put("Tags.member.${i + 1}.Key", key)
                put("Tags.member.${i + 1}.Value", value)
            }
        }

        val testEngine = buildTestConnection {
            expect(StsTestUtils.stsRequest(expectedBody), StsTestUtils.stsResponse())
        }

        val provider = StsAssumeRoleCredentialsProvider(
            bootstrapCredentialsProvider = sourceProvider,
            httpClient = testEngine,
            assumeRoleParameters = AssumeRoleParameters(
                roleArn = StsTestUtils.ARN,
                roleSessionName = StsTestUtils.SESSION_NAME,
                tags = StsTestUtils.TAGS,
                policyArns = StsTestUtils.POLICY_ARNS,
            ),
        )

        val actual = provider.resolve()
        val expected = StsTestUtils.CREDENTIALS.copy(
            attributes = testAttributes(
                StsTestUtils.CREDENTIALS.attributes,
                AwsBusinessMetric.Credentials.CREDENTIALS_STS_ASSUME_ROLE,
            ),
        )
        assertEquals(expected, actual)

        testEngine.assertRequests(CallAsserter.MatchingBodies)
    }

    @Test
    fun testServiceFailure() = runTest {
        val errorResponseBody = """
        <ErrorResponse>
            <Error>
                <Type>Sender</Type>
                <Code>AccessDeniedException</Code>
                <Message>You do not have sufficient access to perform this action</Message>
            </Error>
            <RequestId>foo-id</RequestId>
        </ErrorResponse>
        """
        val testEngine = buildTestConnection {
            expect(HttpResponse(HttpStatusCode.BadRequest, Headers.Empty, HttpBody.fromBytes(errorResponseBody.encodeToByteArray())))
        }

        val provider = StsAssumeRoleCredentialsProvider(
            bootstrapCredentialsProvider = sourceProvider,
            roleArn = StsTestUtils.ARN,
            httpClient = testEngine,
        )

        assertFailsWith<CredentialsProviderException> {
            provider.resolve()
        }.message.shouldContain("failed to assume role from STS")
    }

    @Test
    fun testRegionDisabled() = runTest {
        val errorResponseBody = """
        <ErrorResponse>
            <Error>
                <Type>Sender</Type>
                <Code>RegionDisabledException</Code>
                <Message>AWS STS is not activated in the requested region for the account that is being asked to generate credentials</Message>
            </Error>
            <RequestId>foo-id</RequestId>
        </ErrorResponse>
        """
        val testEngine = buildTestConnection {
            expect(HttpResponse(HttpStatusCode.Forbidden, Headers.Empty, HttpBody.fromBytes(errorResponseBody.encodeToByteArray())))
        }

        val provider = StsAssumeRoleCredentialsProvider(
            bootstrapCredentialsProvider = sourceProvider,
            roleArn = StsTestUtils.ARN,
            region = "us-west-2",
            httpClient = testEngine,
        )

        val ex = assertFailsWith<ProviderConfigurationException> {
            provider.resolve()
        }

        ex.message.shouldContain("STS is not activated in the requested region (us-west-2). Please check your configuration and activate STS in the target region if necessary")
        assertIs<RegionDisabledException>(ex.cause)
    }

    @Test
    fun testGlobalEndpoint() = runTest {
        val testEngine = buildTestConnection {
            expect(StsTestUtils.stsResponse())
        }

        val provider = StsAssumeRoleCredentialsProvider(
            bootstrapCredentialsProvider = sourceProvider,
            roleArn = StsTestUtils.ARN,
            httpClient = testEngine,
        )

        val actual = provider.resolve()
        val expected = StsTestUtils.CREDENTIALS.copy(
            attributes = testAttributes(
                StsTestUtils.CREDENTIALS.attributes,
                AwsBusinessMetric.Credentials.CREDENTIALS_STS_ASSUME_ROLE,
            ),
        )
        assertEquals(expected, actual)
        val req = testEngine.requests().first()
        assertEquals(Host.Domain("sts.amazonaws.com"), req.actual.url.host)
    }

    @Test
    fun testRegionalEndpoint() = runTest {
        val testEngine = buildTestConnection {
            expect(StsTestUtils.stsResponse())
        }

        val provider = StsAssumeRoleCredentialsProvider(
            bootstrapCredentialsProvider = sourceProvider,
            roleArn = StsTestUtils.ARN,
            region = "us-west-2",
            httpClient = testEngine,
        )

        val actual = provider.resolve()
        val expected = StsTestUtils.CREDENTIALS.copy(
            attributes = testAttributes(
                StsTestUtils.CREDENTIALS.attributes,
                AwsBusinessMetric.Credentials.CREDENTIALS_STS_ASSUME_ROLE,
            ),
        )
        assertEquals(expected, actual)
        val req = testEngine.requests().first()
        assertEquals(Host.Domain("sts.us-west-2.amazonaws.com"), req.actual.url.host)
    }

    @Test
    fun testCachesBetweenResolutions() = runTest {
        // An hour of lifetime, so both resolutions below land well inside the refresh window. This provider has no
        // injectable clock, so the expiration is stated relative to the real one.
        val testEngine = buildTestConnection {
            // A single expectation: a second `AssumeRole` call would fail the connection assertion below.
            expect(StsTestUtils.stsResponse(expiration = Instant.now() + 1.hours))
        }

        val provider = StsAssumeRoleCredentialsProvider(
            bootstrapCredentialsProvider = sourceProvider,
            roleArn = StsTestUtils.ARN,
            httpClient = testEngine,
        )

        assertEquals(provider.resolve(), provider.resolve())
        testEngine.assertRequests()
    }
}
