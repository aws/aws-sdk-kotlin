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
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.hours

class TtlInterceptorTest {
    @Test
    fun testPutItemWithTtlField() {
        val testClock = ManualClock(Instant.fromEpochSeconds(0))
        val interceptor = TtlInterceptor<String>(testClock)
        val ttlField = "expiresAt" to 1.hours.inWholeSeconds

        val request = PutItemRequest {
            tableName = "test-table"
            item = mapOf("id" to AttributeValue.S("test-id"))
        }

        val context = createTestContext(request, ttlField)
        val result1 = interceptor.modifyBeforeInvocation(context) as PutItemRequest
        assertEquals(AttributeValue.N("3600"), result1.item?.get("expiresAt"))

        // Advance clock by an hour
        testClock.advance(1.hours)
        val result2 = interceptor.modifyBeforeInvocation(context) as PutItemRequest
        assertEquals(AttributeValue.N("7200"), result2.item?.get("expiresAt"))
    }

    @Test
    fun testPutItemWithoutTtlField() {
        val interceptor = TtlInterceptor<String>()

        val request = PutItemRequest {
            tableName = "test-table"
            item = mapOf("id" to AttributeValue.S("test-id"))
        }

        val context = createTestContext(request, null)
        val result = interceptor.modifyBeforeInvocation(context)

        assertSame(request, result)
    }

    @Test
    fun testNonPutItemRequest() {
        val interceptor = TtlInterceptor<String>()
        val otherRequest = "not-a-put-request"
        val ttlField = "expiresAt" to 3600L

        val context = createTestContext(otherRequest, ttlField)
        val result = interceptor.modifyBeforeInvocation(context)

        assertSame(otherRequest, result)
    }

    @Test
    fun testPutItemWithEmptyItem() {
        val interceptor = TtlInterceptor<String>()
        val ttlField = "expiresAt" to 3600L

        val request = PutItemRequest {
            tableName = "test-table"
            item = null
        }

        val context = createTestContext(request, ttlField)
        val result = interceptor.modifyBeforeInvocation(context)

        assertSame(request, result)
    }

    private fun createTestContext(lowLevelRequest: Any, ttlField: Pair<String, Long>?): LReqContext<String, Any, Any> {
        val attributes = attributesOf {
            ttlField?.let { SchemaAttributes.TtlField to it }
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
