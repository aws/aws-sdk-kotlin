/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.interceptors

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.model.SchemaAttributes
import aws.sdk.kotlin.hll.dynamodbmapper.operations.BatchWriteItemRequest
import aws.sdk.kotlin.hll.dynamodbmapper.operations.BatchWriteItemRequestTable
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LReqContext
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.testConverter
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.testMapperContext
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.testSchema
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.WriteRequest
import aws.smithy.kotlin.runtime.collections.attributesOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import aws.sdk.kotlin.services.dynamodb.model.BatchWriteItemRequest as LowLevelBatchWriteItemRequest

class BatchWriteItemCounterInterceptorTest {
    @Test
    fun testIncrementsPutItemsOnly() {
        val interceptor = CounterInterceptor()
        val schema = ItemSchema(
            testConverter(),
            KeySpec.string("id"),
            attributesOf { SchemaAttributes.CounterFields to setOf("counter1") },
        )

        val highLevelRequest = BatchWriteItemRequest {
            tables = listOf(BatchWriteItemRequestTable(emptyList(), "table1", emptyList(), schema))
        }

        val lowLevelRequest = LowLevelBatchWriteItemRequest {
            requestItems = mapOf(
                "table1" to listOf(
                    WriteRequest {
                        putRequest { item = mapOf("id" to AttributeValue.S("a"), "counter1" to AttributeValue.N("5")) }
                    },
                    WriteRequest {
                        putRequest { item = mapOf("id" to AttributeValue.S("b")) } // missing counter -> defaults to 0
                    },
                    WriteRequest {
                        deleteRequest { key = mapOf("id" to AttributeValue.S("c")) } // must be left untouched
                    },
                ),
            )
        }

        val result = interceptor.modifyBeforeInvocation(
            createContext(highLevelRequest, lowLevelRequest),
        ) as LowLevelBatchWriteItemRequest

        val writes = result.requestItems!!["table1"]!!
        assertEquals("6", writes[0].putRequest!!.item["counter1"]!!.asN())
        assertEquals("1", writes[1].putRequest!!.item["counter1"]!!.asN())
        assertNull(writes[2].putRequest)
        assertEquals(mapOf("id" to AttributeValue.S("c")), writes[2].deleteRequest!!.key)
    }

    @Test
    fun testRoutesCounterFieldsByTable() {
        val interceptor = CounterInterceptor()
        val schemaWithCounter = ItemSchema(
            testConverter(),
            KeySpec.string("id"),
            attributesOf { SchemaAttributes.CounterFields to setOf("counter1") },
        )
        val schemaWithoutCounter = ItemSchema(testConverter(), KeySpec.string("id"))

        val highLevelRequest = BatchWriteItemRequest {
            tables = listOf(
                BatchWriteItemRequestTable(emptyList(), "withCounter", emptyList(), schemaWithCounter),
                BatchWriteItemRequestTable(emptyList(), "withoutCounter", emptyList(), schemaWithoutCounter),
            )
        }

        val lowLevelRequest = LowLevelBatchWriteItemRequest {
            requestItems = mapOf(
                "withCounter" to listOf(WriteRequest { putRequest { item = mapOf("counter1" to AttributeValue.N("5")) } }),
                "withoutCounter" to listOf(WriteRequest { putRequest { item = mapOf("counter1" to AttributeValue.N("5")) } }),
            )
        }

        val result = interceptor.modifyBeforeInvocation(
            createContext(highLevelRequest, lowLevelRequest),
        ) as LowLevelBatchWriteItemRequest

        assertEquals("6", result.requestItems!!["withCounter"]!![0].putRequest!!.item["counter1"]!!.asN())
        assertEquals("5", result.requestItems!!["withoutCounter"]!![0].putRequest!!.item["counter1"]!!.asN())
    }

    @Test
    fun testNoCounterFieldsIsNoOp() {
        val interceptor = CounterInterceptor()
        val schema = ItemSchema(testConverter(), KeySpec.string("id"))

        val highLevelRequest = BatchWriteItemRequest {
            tables = listOf(BatchWriteItemRequestTable(emptyList(), "table1", emptyList(), schema))
        }

        val lowLevelRequest = LowLevelBatchWriteItemRequest {
            requestItems = mapOf("table1" to listOf(WriteRequest { putRequest { item = mapOf("id" to AttributeValue.S("a")) } }))
        }

        val result = interceptor.modifyBeforeInvocation(createContext(highLevelRequest, lowLevelRequest))
        assertSame(lowLevelRequest, result)
    }
}

private fun createContext(
    highLevelRequest: Any,
    lowLevelRequest: Any,
): LReqContext<Any, ItemSchema<Any>, Any, Any> = LReqContext(highLevelRequest, testSchema { }, testMapperContext(), lowLevelRequest)
