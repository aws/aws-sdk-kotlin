/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.interceptors

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.ItemSchemaPartitionKeyImpl
import aws.sdk.kotlin.hll.dynamodbmapper.model.PersistenceSpec
import aws.sdk.kotlin.hll.dynamodbmapper.model.SchemaAttributes
import aws.sdk.kotlin.hll.dynamodbmapper.model.buildItem
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LReqContext
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.MapperContext
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest
import aws.smithy.kotlin.runtime.collections.attributesOf
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.hours

class TtlInterceptorTest {
    @Test
    fun testPutItemWithSingleTtlField() {
        val testClock = ManualClock(Instant.fromEpochSeconds(0))
        val interceptor = TtlInterceptor(testClock)
        val ttlFields = setOf("expiresAt" to 1.hours.inWholeSeconds)

        val request = PutItemRequest {
            tableName = "test-table"
            item = mapOf("id" to AttributeValue.S("test-id"))
        }

        val context = createTestContext(request, ttlFields)
        val result1 = interceptor.modifyBeforeInvocation(context) as PutItemRequest
        assertEquals(AttributeValue.N("3600"), result1.item?.get("expiresAt"))

        // Advance clock by an hour
        testClock.advance(1.hours)
        val result2 = interceptor.modifyBeforeInvocation(context) as PutItemRequest
        assertEquals(AttributeValue.N("7200"), result2.item?.get("expiresAt"))
    }

    @Test
    fun testPutItemWithMultipleTtlFields() {
        val testClock = ManualClock(Instant.fromEpochSeconds(0))
        val interceptor = TtlInterceptor(testClock)
        val ttlFields = setOf(
            "expiresAt" to 1.hours.inWholeSeconds,
            "actuallyExpiresAt" to 2.hours.inWholeSeconds,
        )

        val request = PutItemRequest {
            tableName = "test-table"
            item = mapOf("id" to AttributeValue.S("test-id"))
        }

        val context = createTestContext(request, ttlFields)
        val result = interceptor.modifyBeforeInvocation(context) as PutItemRequest

        assertEquals(AttributeValue.N("3600"), result.item?.get("expiresAt"))
        assertEquals(AttributeValue.N("7200"), result.item?.get("actuallyExpiresAt"))
    }

    @Test
    fun testPutItemWithoutTtlFields() {
        val interceptor = TtlInterceptor()

        val request = PutItemRequest {
            tableName = "test-table"
            item = mapOf("id" to AttributeValue.S("test-id"))
        }

        val context = createTestContext(request, emptySet())
        val result = interceptor.modifyBeforeInvocation(context)

        assertSame(request, result)
    }

    @Test
    fun testNonPutItemRequest() {
        val interceptor = TtlInterceptor()
        val otherRequest = "not-a-put-request"
        val ttlFields = setOf("expiresAt" to 3600L)

        val context = createTestContext(otherRequest, ttlFields)
        val result = interceptor.modifyBeforeInvocation(context)

        assertSame(otherRequest, result)
    }

    @Test
    fun testPutItemWithEmptyItem() {
        val interceptor = TtlInterceptor()
        val ttlFields = setOf("expiresAt" to 3600L)

        val request = PutItemRequest {
            tableName = "test-table"
            item = null
        }

        val context = createTestContext(request, ttlFields)
        val result = interceptor.modifyBeforeInvocation(context)

        assertSame(request, result)
    }

    private fun createTestContext(
        lowLevelRequest: Any,
        ttlFields: Set<Pair<String, Long>>,
    ): LReqContext<Any, ItemSchema<Any>, Any, Any> {
        val attributes = attributesOf {
            if (ttlFields.isNotEmpty()) {
                SchemaAttributes.TtlFields to ttlFields
            }
        }

        val converter: ItemConverter<Any> = Converter(right = { buildItem { } }, left = { "" })

        val schema = ItemSchemaPartitionKeyImpl(
            converter = converter,
            partitionKey = KeySpec.string("id"),
            attributes = attributes,
        )

        val mapperContext = object : MapperContext<Any> {
            override val persistenceSpec: PersistenceSpec<Any>
                get() = error("Not needed for test")
            override val operation: String
                get() = error("Not needed for test")
        }

        return LReqContext("", schema, mapperContext, lowLevelRequest)
    }
}
