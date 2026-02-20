/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper

import aws.sdk.kotlin.hll.dynamodbmapper.items.AttributeDescriptor
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.SimpleItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.operations.scanPaginated
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.DdbLocalTest
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.services.dynamodb.scan
import aws.sdk.kotlin.services.dynamodb.withConfig
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetrics
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamoDbMapperTest : DdbLocalTest() {
    companion object {
        private const val TABLE_NAME = "dummy"

        private data class DummyData(var foo: String = "", var bar: Int = 0)

        private val dummyConverter = SimpleItemConverter(
            ::DummyData,
            { this },
            AttributeDescriptor("foo", DummyData::foo, DummyData::foo::set, StringValueConverter),
            AttributeDescriptor("bar", DummyData::bar, DummyData::bar::set, NumberValueConverters.Int),
        )

        private val dummySchema = ItemSchema(dummyConverter, KeySpec.string("foo"), KeySpec.int("bar"))
    }

    @BeforeAll
    fun setUp() = runTest {
        createTable(TABLE_NAME, dummySchema)
    }

    @Test
    fun testBusinessMetricEmission() = runTest {
        val interceptor = MetricCapturingInterceptor()

        val ddb = lowLevelAccess { withConfig { interceptors += interceptor } }
        interceptor.assertEmpty()

        // No metric for low-level client
        lowLevelAccess { scan { tableName = TABLE_NAME } }
        interceptor.assertMetric(AwsBusinessMetric.DDB_MAPPER, exists = false)
        interceptor.reset()

        // Metric for high-level client
        val mapper = mapper(ddb)
        val table = mapper.getTable(TABLE_NAME, dummySchema)
        table.scanPaginated { }.collect()
        interceptor.assertMetric(AwsBusinessMetric.DDB_MAPPER)
        interceptor.reset()

        // Still no metric for low-level client (i.e., LL wasn't modified by HL)
        lowLevelAccess { scan { tableName = TABLE_NAME } }
        interceptor.assertMetric(AwsBusinessMetric.DDB_MAPPER, exists = false)
        interceptor.reset()

        // Original client can be closed, mapper is unaffected
        lowLevelAccess<Unit> { close() }
        table.scanPaginated { }.collect()
        interceptor.assertMetric(AwsBusinessMetric.DDB_MAPPER)
    }
}

private class MetricCapturingInterceptor : HttpInterceptor {
    private val capturedMetrics = mutableSetOf<BusinessMetric>()

    override fun readBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>) {
        capturedMetrics += context.executionContext[BusinessMetrics]
    }

    fun assertMetric(metric: BusinessMetric, exists: Boolean = true) {
        if (exists) {
            assertTrue(
                metric in capturedMetrics,
                "Expected metrics to contain $metric. Actual values: $capturedMetrics",
            )
        } else {
            assertFalse(
                metric in capturedMetrics,
                "Expected metrics *not* to contain $metric. Actual values: $capturedMetrics",
            )
        }
    }

    fun assertEmpty() {
        assertTrue(capturedMetrics.isEmpty(), "Expected metrics to be empty. Actual values: $capturedMetrics")
    }

    fun reset() {
        capturedMetrics.clear()
    }
}
