/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http.interceptors

import aws.sdk.kotlin.runtime.http.BUSINESS_METRICS_MAX_LENGTH
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.BusinessMetricsInterceptor
import aws.sdk.kotlin.runtime.http.middleware.USER_AGENT
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.SmithyBusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.emitBusinessMetric
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class BusinessMetricsInterceptorTest {
    @Test
    fun noBusinessMetrics() = runTest {
        val executionContext = ExecutionContext()
        val interceptor = BusinessMetricsInterceptor()
        val request = interceptor.modifyBeforeTransmit(interceptorContext(executionContext))
        val userAgentHeader = request.headers[USER_AGENT]!!

        assertFalse(userAgentHeader.endsWith("m/"))
    }

    @Test
    fun noValidBusinessMetrics() = runTest {
        val executionContext = ExecutionContext()

        val invalidBusinessMetric = object : BusinessMetric {
            override val identifier: String = "All work and no play makes Jack a dull boy".repeat(1000)
        }

        executionContext.emitBusinessMetric(invalidBusinessMetric)

        val interceptor = BusinessMetricsInterceptor()
        val request = interceptor.modifyBeforeTransmit(interceptorContext(executionContext))
        val userAgentHeader = request.headers[USER_AGENT]!!

        assertFalse(userAgentHeader.endsWith("m/"))
    }

    @Test
    fun businessMetrics() = runTest {
        val executionContext = ExecutionContext()
        executionContext.emitBusinessMetric(AwsBusinessMetric.S3_EXPRESS_BUCKET)

        val interceptor = BusinessMetricsInterceptor()
        val request = interceptor.modifyBeforeTransmit(interceptorContext(executionContext))
        val userAgentHeader = request.headers[USER_AGENT]!!

        assertTrue(
            userAgentHeader.endsWith(
                "m/${AwsBusinessMetric.S3_EXPRESS_BUCKET.identifier}",
            ),
        )
    }

    @Test
    fun multipleBusinessMetrics() = runTest {
        val executionContext = ExecutionContext()
        executionContext.emitBusinessMetric(AwsBusinessMetric.S3_EXPRESS_BUCKET)
        executionContext.emitBusinessMetric(SmithyBusinessMetric.GZIP_REQUEST_COMPRESSION)

        val interceptor = BusinessMetricsInterceptor()
        val request = interceptor.modifyBeforeTransmit(interceptorContext(executionContext))
        val userAgentHeader = request.headers[USER_AGENT]!!

        assertTrue(
            userAgentHeader.endsWith(
                "m/${AwsBusinessMetric.S3_EXPRESS_BUCKET.identifier},${SmithyBusinessMetric.GZIP_REQUEST_COMPRESSION.identifier}",
            ),
        )
    }

    @Test
    fun businessMetricsMaxLength() = runTest {
        val executionContext = ExecutionContext()

        for (i in 0..BUSINESS_METRICS_MAX_LENGTH) {
            executionContext.emitBusinessMetric(
                object : BusinessMetric {
                    override val identifier: String = i.toString()
                },
            )
        }

        val interceptor = BusinessMetricsInterceptor()
        val request = interceptor.modifyBeforeTransmit(interceptorContext(executionContext))
        val userAgentHeader = request.headers[USER_AGENT]!!
        val metrics = "m/" + userAgentHeader.substringAfter("m/")

        assertTrue(metrics.encodeToByteArray().size <= BUSINESS_METRICS_MAX_LENGTH)
        assertFalse(metrics.endsWith(","))
    }

    @Test
    fun invalidBusinessMetric() = runTest {
        val executionContext = ExecutionContext()

        val validMetric = AwsBusinessMetric.S3_EXPRESS_BUCKET
        val invalidMetric = object : BusinessMetric {
            override val identifier: String = "All work and no play makes Jack a dull boy".repeat(1000)
        }

        executionContext.attributes.emitBusinessMetric(validMetric)
        executionContext.attributes.emitBusinessMetric(invalidMetric)

        val interceptor = BusinessMetricsInterceptor()
        val request = interceptor.modifyBeforeTransmit(interceptorContext(executionContext))
        val userAgentHeader = request.headers[USER_AGENT]!!

        assertTrue(
            userAgentHeader.contains(validMetric.identifier),
        )
        assertFalse(
            userAgentHeader.contains(invalidMetric.identifier),
        )
    }

    @Test
    fun businessMetricToString() {
        val businessMetricToString = AwsBusinessMetric.S3_EXPRESS_BUCKET.toString()
        val businessMetricIdentifier = AwsBusinessMetric.S3_EXPRESS_BUCKET.identifier

        assertEquals(businessMetricIdentifier, businessMetricToString)
    }
}

private fun interceptorContext(executionContext: ExecutionContext): ProtocolRequestInterceptorContext<Any, HttpRequest> = object : ProtocolRequestInterceptorContext<Any, HttpRequest> {
    override val protocolRequest: HttpRequest = HttpRequest(
        HttpMethod.GET,
        Url.parse("https://test.aws.com?foo=bar"),
        Headers {
            append(USER_AGENT, "aws-sdk-kotlin/1.2.3 ua/2.1 api/test-service#1.2.3...")
        },
    )
    override val executionContext: ExecutionContext = executionContext
    override val request: Any = Unit
}
