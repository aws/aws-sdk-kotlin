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
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.hours
import aws.sdk.kotlin.services.dynamodb.model.BatchWriteItemRequest as LowLevelBatchWriteItemRequest

class BatchWriteItemTtlInterceptorTest {
    @Test
    fun testSetsTtlOnPutItemsOnly() {
        val clock = ManualClock(Instant.fromEpochSeconds(0))
        val interceptor = TtlInterceptor(clock)
        val ttlFields = mapOf("expiresAt" to 1.hours.inWholeSeconds)
        val schema = ItemSchema(
            testConverter(),
            KeySpec.string("id"),
            attributesOf { SchemaAttributes.TtlFields to ttlFields },
        )

        val highLevelRequest = BatchWriteItemRequest {
            tables = listOf(BatchWriteItemRequestTable(emptyList(), "table1", emptyList(), schema))
        }

        val lowLevelRequest = LowLevelBatchWriteItemRequest {
            requestItems = mapOf(
                "table1" to listOf(
                    WriteRequest { putRequest { item = mapOf("id" to AttributeValue.S("a")) } },
                    WriteRequest { deleteRequest { key = mapOf("id" to AttributeValue.S("b")) } }, // untouched
                ),
            )
        }

        val result = interceptor.modifyBeforeInvocation(
            createContext(highLevelRequest, lowLevelRequest),
        ) as LowLevelBatchWriteItemRequest

        val writes = result.requestItems!!["table1"]!!
        assertEquals(AttributeValue.N("3600"), writes[0].putRequest!!.item["expiresAt"])
        assertNull(writes[1].putRequest)
        assertNull(writes[1].deleteRequest!!.key["expiresAt"])
    }

    @Test
    fun testRoutesTtlFieldsByTable() {
        val clock = ManualClock(Instant.fromEpochSeconds(0))
        val interceptor = TtlInterceptor(clock)
        val ttlFields = mapOf("expiresAt" to 1.hours.inWholeSeconds)
        val schemaWithTtl = ItemSchema(
            testConverter(),
            KeySpec.string("id"),
            attributesOf { SchemaAttributes.TtlFields to ttlFields },
        )
        val schemaWithoutTtl = ItemSchema(testConverter(), KeySpec.string("id"))

        val highLevelRequest = BatchWriteItemRequest {
            tables = listOf(
                BatchWriteItemRequestTable(emptyList(), "withTtl", emptyList(), schemaWithTtl),
                BatchWriteItemRequestTable(emptyList(), "withoutTtl", emptyList(), schemaWithoutTtl),
            )
        }

        val lowLevelRequest = LowLevelBatchWriteItemRequest {
            requestItems = mapOf(
                "withTtl" to listOf(WriteRequest { putRequest { item = mapOf("id" to AttributeValue.S("a")) } }),
                "withoutTtl" to listOf(WriteRequest { putRequest { item = mapOf("id" to AttributeValue.S("b")) } }),
            )
        }

        val result = interceptor.modifyBeforeInvocation(
            createContext(highLevelRequest, lowLevelRequest),
        ) as LowLevelBatchWriteItemRequest

        assertEquals(AttributeValue.N("3600"), result.requestItems!!["withTtl"]!![0].putRequest!!.item["expiresAt"])
        assertNull(result.requestItems!!["withoutTtl"]!![0].putRequest!!.item["expiresAt"])
    }

    @Test
    fun testNoTtlFieldsIsNoOp() {
        val interceptor = TtlInterceptor(ManualClock(Instant.fromEpochSeconds(0)))
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
