/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsRefreshBehavior
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.request.HttpRequestBuilder
import aws.smithy.kotlin.runtime.http.request.url
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.text.encoding.PercentEncoding
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.TimestampFormat
import kotlin.time.Duration.Companion.minutes

object StsTestUtils {
    const val ARN = "arn:aws:iam::1234567:role/test-role"
    const val POLICY = "foo!"
    const val REGION = "us-east-2"
    const val SESSION_NAME = "aws-sdk-kotlin-1234567890"

    val POLICY_ARNS = listOf("apple", "banana", "cherry")
    val TAGS = mapOf("foo" to "bar", "baz" to "qux")
    val EPOCH = Instant.fromIso8601("2020-10-16T03:56:00Z")
    val CREDENTIALS = credentials(
        "AKIDTest",
        "test-secret",
        "test-token",
        EPOCH + 15.minutes,
        "AssumeRoleProvider",
        "1234567",
        refreshBehavior = CredentialsRefreshBehavior.RefreshableWithStaticStability,
    )

    fun stsRequest(bodyParameters: Map<String, String>) = HttpRequestBuilder().apply {
        val bodyBytes = bodyParameters
            .entries
            .joinToString("&") { (key, value) ->
                val kEnc = PercentEncoding.FormUrl.encode(key)
                val vEnc = PercentEncoding.FormUrl.encode(value)
                "$kEnc=$vEnc"
            }
            .encodeToByteArray()

        method = HttpMethod.GET
        url(Url.parse("https://sts.$REGION.amazonaws.com/"))
        body = HttpBody.fromBytes(bodyBytes)
    }.build()

    // see https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRole.html#API_AssumeRole_ResponseElements
    fun stsResponse(
        roleArn: String = ARN,
        expiration: Instant? = null,
    ): HttpResponse {
        val roleId = roleArn.split("/").last()
        val expiry = expiration ?: CREDENTIALS.expiration!!
        val body = """
            <AssumeRoleResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
              <AssumeRoleResult>
              <SourceIdentity>Alice</SourceIdentity>
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
                <PackedPolicySize>6</PackedPolicySize>
              </AssumeRoleResult>
              <ResponseMetadata>
                <RequestId>c6104cbe-af31-11e0-8154-cbc7ccf896c7</RequestId>
              </ResponseMetadata>
            </AssumeRoleResponse>
        """.trimIndent()
        return HttpResponse(HttpStatusCode.OK, Headers.Empty, HttpBody.fromBytes(body.encodeToByteArray()))
    }
}
