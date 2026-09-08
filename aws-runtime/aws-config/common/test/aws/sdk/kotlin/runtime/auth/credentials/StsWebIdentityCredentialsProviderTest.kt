/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.CallAsserter
import aws.smithy.kotlin.runtime.httptest.TestConnection
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.util.TestFile
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes

private const val TOKEN_PATH = "token-path"
private const val TOKEN_VALUE = "jwt-token"

private val CREDENTIALS = credentials(
    "AKIDTest",
    "test-secret",
    "test-token",
    StsTestUtils.EPOCH + 15.minutes,
    "WebIdentityToken",
    "1234567",
).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_STS_ASSUME_ROLE_WEB_ID)

class StsWebIdentityCredentialsProviderTest {
    // see https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRoleWithWebIdentity.html#API_AssumeRoleWithWebIdentity_ResponseElements
    private fun stsResponse(
        roleArn: String = StsTestUtils.ARN,
        expiration: Instant? = null,
    ): HttpResponse {
        val roleId = roleArn.split("/").last()
        val expiry = expiration ?: CREDENTIALS.expiration!!
        val body = """
        <AssumeRoleWithWebIdentityResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
          <AssumeRoleWithWebIdentityResult>
            <SubjectFromWebIdentityToken>amzn1.account.AF6RHO7KZU5XRVQJGXK6HB56KR2A</SubjectFromWebIdentityToken>
            <Audience>client.5498841531868486423.1548@apps.example.com</Audience>
            <AssumedRoleUser>
              <Arn>$roleArn</Arn>
              <AssumedRoleId>ARO123EXAMPLE123:$roleId</AssumedRoleId>
            </AssumedRoleUser>
            <Credentials>
              <AccessKeyId>AKIDTest</AccessKeyId>
              <SecretAccessKey>test-secret</SecretAccessKey>
              <SessionToken>test-token</SessionToken>
              <Expiration>${expiry.format(TimestampFormat.ISO_8601)}</Expiration>
            </Credentials>
            <SourceIdentity>SourceIdentityValue</SourceIdentity>
            <Provider>www.amazon.com</Provider>
          </AssumeRoleWithWebIdentityResult>
          <ResponseMetadata>
            <RequestId>ad4156e9-bce1-11e2-82e6-6b6efEXAMPLE</RequestId>
          </ResponseMetadata>
        </AssumeRoleWithWebIdentityResponse>
        """.trimIndent()

        return HttpResponse(HttpStatusCode.OK, Headers.Empty, HttpBody.fromBytes(body.encodeToByteArray()))
    }

    @Test
    fun testSuccess() = runTest {
        val expectedBody = buildMap {
            put("Action", "AssumeRoleWithWebIdentity")
            put("Version", "2011-06-15")
            put("DurationSeconds", "900")
            put("RoleArn", StsTestUtils.ARN)
            put("RoleSessionName", StsTestUtils.SESSION_NAME)
            put("WebIdentityToken", TOKEN_VALUE)
        }

        val testEngine = buildTestConnection {
            expect(StsTestUtils.stsRequest(expectedBody), stsResponse())
        }

        val testPlatform = TestPlatformProvider.of(
            fs = mapOf(TOKEN_PATH to TestFile(TOKEN_VALUE)),
        )

        val provider = StsWebIdentityCredentialsProvider(
            roleArn = StsTestUtils.ARN,
            roleSessionName = StsTestUtils.SESSION_NAME,
            webIdentityTokenFilePath = TOKEN_PATH,
            region = StsTestUtils.REGION,
            httpClient = testEngine,
            platformProvider = testPlatform,
        )

        val actual = provider.resolve()
        assertEquals(CREDENTIALS, actual)

        testEngine.assertRequests(CallAsserter.MatchingBodies)
    }

    @Test
    fun testSuccessWithAdditionalParams() = runTest {
        val expectedBody = buildMap {
            put("Action", "AssumeRoleWithWebIdentity")
            put("Version", "2011-06-15")
            put("DurationSeconds", "900")
            put("Policy", StsTestUtils.POLICY)
            StsTestUtils.POLICY_ARNS.forEachIndexed { i, arn ->
                put("PolicyArns.member.${i + 1}.arn", arn)
            }
            put("RoleArn", StsTestUtils.ARN)
            put("RoleSessionName", StsTestUtils.SESSION_NAME)
            put("WebIdentityToken", TOKEN_VALUE)
        }

        val testEngine = buildTestConnection {
            expect(StsTestUtils.stsRequest(expectedBody), stsResponse())
        }

        val testPlatform = TestPlatformProvider.of(
            fs = mapOf(TOKEN_PATH to TestFile(TOKEN_VALUE)),
        )

        val provider = StsWebIdentityCredentialsProvider(
            AssumeRoleWithWebIdentityParameters(
                roleArn = StsTestUtils.ARN,
                roleSessionName = StsTestUtils.SESSION_NAME,
                webIdentityTokenFilePath = TOKEN_PATH,
                policyArns = StsTestUtils.POLICY_ARNS,
                policy = StsTestUtils.POLICY,
            ),
            region = StsTestUtils.REGION,
            httpClient = testEngine,
            platformProvider = testPlatform,
        )

        val actual = provider.resolve()
        assertEquals(CREDENTIALS, actual)

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

        val testPlatform = TestPlatformProvider.of(
            fs = mapOf(TOKEN_PATH to TestFile(TOKEN_VALUE)),
        )

        val provider = StsWebIdentityCredentialsProvider(
            roleArn = StsTestUtils.ARN,
            webIdentityTokenFilePath = TOKEN_PATH,
            region = StsTestUtils.REGION,
            httpClient = testEngine,
            platformProvider = testPlatform,
        )

        assertFailsWith<CredentialsProviderException> {
            provider.resolve()
        }.message.shouldContain("STS failed to assume role from web identity")
    }

    @Test
    fun testJwtTokenMissing() = runTest {
        val testEngine = TestConnection()

        val testPlatform = TestPlatformProvider.of()

        val provider = StsWebIdentityCredentialsProvider(
            roleArn = StsTestUtils.ARN,
            webIdentityTokenFilePath = TOKEN_PATH,
            region = StsTestUtils.REGION,
            httpClient = testEngine,
            platformProvider = testPlatform,
        )

        assertFailsWith<CredentialsProviderException> {
            provider.resolve()
        }.message.shouldContain("failed to read webIdentityToken from token-path")
    }

    @Test
    fun testFromEnvironment() {
        val tp1 = TestPlatformProvider.of(
            env = mapOf(
                "AWS_ROLE_ARN" to "my-role",
                "AWS_WEB_IDENTITY_TOKEN_FILE" to "token-file-path",
                "AWS_REGION" to StsTestUtils.REGION,
            ),
        )

        StsWebIdentityCredentialsProvider.fromEnvironment(platformProvider = tp1)

        // missing AWS_ROLE_ARN
        assertFailsWith<ProviderConfigurationException> {
            val tp2 = TestPlatformProvider.of(
                env = mapOf(
                    "AWS_WEB_IDENTITY_TOKEN_FILE" to "token-file-path",
                    "AWS_REGION" to StsTestUtils.REGION,
                ),
            )
            StsWebIdentityCredentialsProvider.fromEnvironment(platformProvider = tp2)
        }.message.shouldContain("Required field `roleArn` could not be automatically inferred for StsWebIdentityCredentialsProvider. Either explicitly pass a value, set the environment variable `AWS_ROLE_ARN`, or set the JVM system property `aws.roleArn`")
    }
}
