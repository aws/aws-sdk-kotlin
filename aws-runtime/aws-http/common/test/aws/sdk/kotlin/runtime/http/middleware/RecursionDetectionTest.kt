/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.http.middleware

import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.http.SdkHttpClient
import aws.smithy.kotlin.runtime.http.operation.*
import aws.smithy.kotlin.runtime.http.request.HttpRequestBuilder
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.TestEngine
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RecursionDetectionTest {
    private class TraceHeaderSerializer(
        private val traceHeader: String,
    ) : HttpSerializer.NonStreaming<Unit> {
        override fun serialize(context: ExecutionContext, input: Unit): HttpRequestBuilder {
            val builder = HttpRequestBuilder()
            builder.headers[HEADER_TRACE_ID] = traceHeader
            return builder
        }
    }

    private val client = SdkHttpClient(TestEngine())

    private suspend fun test(
        env: Map<String, String>,
        existingTraceHeader: String?,
        expectedTraceHeader: String?,
    ) {
        val op = SdkHttpOperation.build<Unit, HttpResponse> {
            serializeWith = if (existingTraceHeader != null) {
                TraceHeaderSerializer(existingTraceHeader)
            } else {
                HttpSerializer.Unit
            }

            deserializeWith = HttpDeserializer.Identity
            operationName = "testOperation"
            serviceName = "TestService"
        }

        val provider = TestPlatformProvider.of(env = env)
        op.install(RecursionDetection(provider))
        op.roundTrip(client, Unit)

        val request = op.context[HttpOperationContext.HttpCallList].last().request
        if (expectedTraceHeader != null) {
            assertEquals(expectedTraceHeader, request.headers[HEADER_TRACE_ID])
        } else {
            assertFalse(request.headers.contains(HEADER_TRACE_ID))
        }
    }

    @Test
    fun `it noops if env unset`() = runTest {
        test(
            emptyMap(),
            null,
            null,
        )
    }

    @Test
    fun `it sets header when both envs are present`() = runTest {
        test(
            mapOf(
                ENV_FUNCTION_NAME to "some-function",
                ENV_TRACE_ID to "Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1;lineage=a87bd80c:0,68fd508a:5,c512fbe3:2",
            ),
            null,
            "Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1;lineage=a87bd80c:0,68fd508a:5,c512fbe3:2",
        )
    }

    @Test
    fun `it noops if trace env set but no lambda env`() = runTest {
        test(
            mapOf(
                ENV_TRACE_ID to "Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1;lineage=a87bd80c:0,68fd508a:5,c512fbe3:2",
            ),
            null,
            null,
        )
    }

    @Test
    fun `it respects existing trace header`() = runTest {
        test(
            mapOf(
                ENV_FUNCTION_NAME to "some-function",
                ENV_TRACE_ID to "EnvValue",
            ),
            "OriginalValue",
            "OriginalValue",
        )
    }

    @Test
    fun `it url encodes new trace header`() = runTest {
        test(
            mapOf(
                ENV_FUNCTION_NAME to "some-function",
                ENV_TRACE_ID to "first\nsecond",
            ),
            null,
            "first%0Asecond",
        )
    }

    @Test
    fun `ignores other chars that are usually percent encoded`() = runTest {
        test(
            mapOf(
                ENV_FUNCTION_NAME to "some-function",
                ENV_TRACE_ID to "test123-=;:+&[]{}\"'",
            ),
            null,
            "test123-=;:+&[]{}\"'",
        )
    }
}
