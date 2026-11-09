/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.paginators.listObjectsV2Paginated
import aws.sdk.kotlin.services.s3.waiters.waitUntilBucketExists
import aws.smithy.kotlin.runtime.businessmetrics.SmithyBusinessMetric
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.TestConnection
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.telemetry.AbstractTelemetryProvider
import aws.smithy.kotlin.runtime.telemetry.TelemetryProvider
import aws.smithy.kotlin.runtime.telemetry.trace.AbstractTracerProvider
import aws.smithy.kotlin.runtime.telemetry.trace.TracerProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BusinessMetricsUserAgentTest {
    private fun newTestClient(
        conn: TestConnection,
        telemetry: TelemetryProvider = TelemetryProvider.None,
    ): S3Client = S3Client {
        region = "us-east-1"
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = "accessKeyId"
            secretAccessKey = "secretAccessKey"
        }
        httpClient = conn
        telemetryProvider = telemetry
    }

    private fun okResponse(body: String? = null) = HttpResponse(
        HttpStatusCode.OK,
        Headers.Empty,
        body?.let { HttpBody.fromBytes(it.encodeToByteArray()) } ?: HttpBody.Empty,
    )

    private fun TestConnection.sentBusinessMetrics(): List<Set<String>> = requests().map { call ->
        val userAgent = checkNotNull(call.actual.headers["User-Agent"])
        val metrics = Regex("m/(\\S+)").find(userAgent)?.groupValues?.get(1).orEmpty()
        metrics.split(",").filter { it.isNotEmpty() }.toSet()
    }

    @Test
    fun testWaiter() = runTest {
        val conn = buildTestConnection { expect(okResponse()) }
        newTestClient(conn).use { it.waitUntilBucketExists { bucket = "bucket" } }

        val sent = conn.sentBusinessMetrics()
        assertEquals(1, sent.size)
        assertTrue(SmithyBusinessMetric.WAITER.identifier in sent.single())
    }

    @Test
    fun testPaginator() = runTest {
        val page = "<ListBucketResult><IsTruncated>false</IsTruncated></ListBucketResult>"
        val conn = buildTestConnection { expect(okResponse(page)) }
        newTestClient(conn).use { it.listObjectsV2Paginated { bucket = "bucket" }.toList() }

        val sent = conn.sentBusinessMetrics()
        assertEquals(1, sent.size)
        assertTrue(SmithyBusinessMetric.PAGINATOR.identifier in sent.single())
    }

    @Test
    fun testDirectCall() = runTest {
        val conn = buildTestConnection { expect(okResponse()) }
        newTestClient(conn).use { it.headBucket { bucket = "bucket" } }

        val sent = conn.sentBusinessMetrics().single()
        assertTrue(SmithyBusinessMetric.RETRY_MODE_STANDARD.identifier in sent, "Expected parsed metrics, got $sent")
        assertFalse(SmithyBusinessMetric.WAITER.identifier in sent)
        assertFalse(SmithyBusinessMetric.PAGINATOR.identifier in sent)
        assertFalse(SmithyBusinessMetric.OBSERVABILITY_TRACING.identifier in sent)
    }

    @Test
    fun testTracingProvider() = runTest {
        val telemetry = object : AbstractTelemetryProvider() {
            override val tracerProvider: TracerProvider = object : AbstractTracerProvider() { }
        }
        val conn = buildTestConnection { expect(okResponse()) }
        newTestClient(conn, telemetry).use { it.headBucket { bucket = "bucket" } }

        assertTrue(SmithyBusinessMetric.OBSERVABILITY_TRACING.identifier in conn.sentBusinessMetrics().single())
    }
}
