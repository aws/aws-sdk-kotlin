/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.interceptors

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.ItemSchemaPartitionKeyImpl
import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.PersistenceSpec
import aws.sdk.kotlin.hll.dynamodbmapper.model.SchemaAttributes
import aws.sdk.kotlin.hll.dynamodbmapper.model.buildItem
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LReqContext
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.MapperContext
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest
import aws.smithy.kotlin.runtime.collections.attributesOf
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CounterInterceptorTest {
    @Test
    fun testCounterInterceptorIncrementsFields() {
        val counterFields = setOf("counter1", "counter2")
        val interceptor = CounterInterceptor<String>()

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
        val interceptor = CounterInterceptor<String>()

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
        val interceptor = CounterInterceptor<String>()
        val request = PutItemRequest {
            tableName = "test-table"
            item = mapOf("id" to AttributeValue.S("test"))
        }
        val ctx = createContext(request, null)

        val result = interceptor.modifyBeforeInvocation(ctx)
        assertSame(request, result)
    }

    @Test
    fun testCounterInterceptorNonPutItemRequest() {
        val interceptor = CounterInterceptor<String>()
        val otherRequest = "not a put item request"
        val ctx = createContext(otherRequest, setOf("counter"))

        val result = interceptor.modifyBeforeInvocation(ctx)
        assertSame(otherRequest, result)
    }

    @Test
    fun testCounterInterceptorThrowsOnUnparseableData() {
        val counterFields = setOf("counter1")
        val interceptor = CounterInterceptor<String>()

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

    private fun createContext(lowLevelRequest: Any, counterFields: Set<String>?): LReqContext<String, Any, Any> {
        val attributes = attributesOf {
            counterFields?.let { SchemaAttributes.CounterFields to it }
        }

        val converter = object : ItemConverter<String> {
            override val left = MonoConverter<Item, String> { "" }
            override val right = MonoConverter<String, Item> { buildItem { } }
        }

        val schema = ItemSchemaPartitionKeyImpl(
            converter = converter,
            partitionKey = KeySpec.string("id"),
            attributes = attributes,
        )

        val mapperContext = object : MapperContext<String> {
            override val persistenceSpec: PersistenceSpec<String>
                get() = error("Not needed for test")
            override val operation: String
                get() = error("Not needed for test")
        }

        return LReqContext("", schema, mapperContext, lowLevelRequest)
    }
}
