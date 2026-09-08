/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.interceptors

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.model.SchemaAttributes
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LReqContext
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.testMapperContext
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.testSchema
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class PutItemCounterInterceptorTest {
    @Test
    fun testCounterInterceptorIncrementsFields() {
        val counterFields = setOf("counter1", "counter2")
        val interceptor = CounterInterceptor()

        val request = PutItemRequest {
            tableName = "test-table"
            item = mapOf(
                "id" to AttributeValue.S("test"),
                "counter1" to AttributeValue.N("5"),
                "counter2" to AttributeValue.N("10"),
                "other" to AttributeValue.S("value"),
            )
        }

        val ctx = createContext(request, counterFields)
        val result = interceptor.modifyBeforeInvocation(ctx) as PutItemRequest

        assertEquals("test", result.item!!["id"]!!.asS())
        assertEquals("6", result.item!!["counter1"]!!.asN())
        assertEquals("11", result.item!!["counter2"]!!.asN())
        assertEquals("value", result.item!!["other"]!!.asS())
    }

    @Test
    fun testCounterInterceptorInitializesZeroFields() {
        val counterFields = setOf("newCounter")
        val interceptor = CounterInterceptor()

        val request = PutItemRequest {
            tableName = "test-table"
            item = mapOf("id" to AttributeValue.S("test"))
        }

        val ctx = createContext(request, counterFields)
        val result = interceptor.modifyBeforeInvocation(ctx) as PutItemRequest

        assertEquals("1", result.item!!["newCounter"]!!.asN())
    }

    @Test
    fun testCounterInterceptorNoCounterFields() {
        val interceptor = CounterInterceptor()
        val request = PutItemRequest {
            tableName = "test-table"
            item = mapOf("id" to AttributeValue.S("test"))
        }
        val ctx = createContext(request)

        val result = interceptor.modifyBeforeInvocation(ctx)
        assertSame(request, result)
    }

    @Test
    fun testCounterInterceptorNonPutItemRequest() {
        val counterFields = setOf("counter1")
        val interceptor = CounterInterceptor()
        val otherRequest = "not a put item request"
        val ctx = createContext(otherRequest, counterFields)

        val result = interceptor.modifyBeforeInvocation(ctx)
        assertSame(otherRequest, result)
    }

    @Test
    fun testCounterInterceptorThrowsOnUnparseableData() {
        val counterFields = setOf("counter1")
        val interceptor = CounterInterceptor()

        val request = PutItemRequest {
            tableName = "test-table"
            item = mapOf(
                "id" to AttributeValue.S("test"),
                "counter1" to AttributeValue.N("not-a-number"),
            )
        }

        val ctx = createContext(request, counterFields)

        assertFailsWith<NumberFormatException> {
            interceptor.modifyBeforeInvocation(ctx)
        }
    }
}

private fun createContext(
    lowLevelRequest: Any,
    counterFields: Set<String>? = null,
): LReqContext<Any, ItemSchema<Any>, Any, Any> {
    val schema = testSchema { counterFields?.let { SchemaAttributes.CounterFields to it } }
    val mapperContext = testMapperContext()
    return LReqContext("", schema, mapperContext, lowLevelRequest)
}
