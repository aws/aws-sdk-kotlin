/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.http.middleware

import aws.sdk.kotlin.runtime.http.ApiMetadata
import aws.sdk.kotlin.runtime.http.loadAwsUserAgentMetadataFromEnvironment
import aws.sdk.kotlin.runtime.http.operation.customUserAgentMetadata
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.http.SdkHttpClient
import aws.smithy.kotlin.runtime.http.operation.*
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.TestEngine
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserAgentTest {
    private val client = SdkHttpClient(TestEngine())

    private fun initializeOp(platformProvider: PlatformProvider = TestPlatformProvider()) = SdkHttpOperation.build<Unit, HttpResponse> {
        serializeWith = HttpSerializer.Unit
        deserializeWith = HttpDeserializer.Identity
        operationName = "testOperation"
        serviceName = "TestService"
    }.apply {
        val apiMd = ApiMetadata("Test Service", "1.2.3")
        val metadata = loadAwsUserAgentMetadataFromEnvironment(platformProvider, apiMd)
        install(UserAgent(metadata))
    }

    @Test
    fun itSetsUAHeaders() = runTest {
        val op = initializeOp()

        op.roundTrip(client, Unit)
        val request = op.context[HttpOperationContext.HttpCallList].last().request
        assertTrue(request.headers.contains(USER_AGENT))
        assertTrue(request.headers.contains(X_AMZ_USER_AGENT))
        assertEquals("aws-sdk-kotlin/1.2.3", request.headers[X_AMZ_USER_AGENT])
        assertTrue(
            request.headers[USER_AGENT]!!.startsWith("aws-sdk-kotlin/1.2.3 ua/2.1 api/test-service#1.2.3"),
            "$USER_AGENT header didn't start with expected value. Found: ${request.headers[USER_AGENT]}",
        )
    }

    @Test
    fun itAddsPerOperationMetadata() = runTest {
        val op = initializeOp()
        op.context.customUserAgentMetadata.add("foo", "bar")

        op.roundTrip(client, Unit)
        val request = op.context[HttpOperationContext.HttpCallList].last().request

        request.headers[USER_AGENT]!!.shouldContain("md/foo#bar")

        // verify per/request metadata is actually per/request
        val op2 = initializeOp()
        op2.context.customUserAgentMetadata.add("baz", "quux")

        op2.roundTrip(client, Unit)
        val request2 = op2.context[HttpOperationContext.HttpCallList].last().request

        request2.headers[USER_AGENT]!!.shouldNotContain("md/foo#bar")
        request2.headers[USER_AGENT]!!.shouldContain("md/baz#quux")
    }

    @Test
    fun itMergesCustomMetadataWithExisting() = runTest {
        // see: https://github.com/awslabs/aws-sdk-kotlin/issues/694
        val platform = TestPlatformProvider(
            props = mapOf(
                "aws.customMetadata.foo" to "bar",
                "aws.customMetadata.baz" to "qux",
            ),
        )
        val op = initializeOp(platform)
        op.context.customUserAgentMetadata.apply {
            add("baz", "quux")
            add("blerg", "blarg")
        }

        op.roundTrip(client, Unit)
        val request = op.context[HttpOperationContext.HttpCallList].last().request
        val uaString = request.headers[USER_AGENT]!!

        uaString.shouldContain("md/foo#bar")
        uaString.shouldContain("md/baz#quux")
        uaString.shouldContain("md/blerg#blarg")
        uaString.shouldNotContain("md/baz#qux") // This was overwritten by "baz#quux"
    }

    @Test
    fun itDoesNotClobberExistingCustomMetadata() = runTest {
        // see: https://github.com/awslabs/aws-sdk-kotlin/issues/694
        val platform = TestPlatformProvider(
            props = mapOf(
                "aws.customMetadata.foo" to "bar",
                "aws.customMetadata.baz" to "qux",
            ),
        )
        val op = initializeOp(platform)

        op.roundTrip(client, Unit)
        val request = op.context[HttpOperationContext.HttpCallList].last().request
        val uaString = request.headers[USER_AGENT]!!

        uaString.shouldContain("md/foo#bar")
        uaString.shouldContain("md/baz#qux")
    }
}
