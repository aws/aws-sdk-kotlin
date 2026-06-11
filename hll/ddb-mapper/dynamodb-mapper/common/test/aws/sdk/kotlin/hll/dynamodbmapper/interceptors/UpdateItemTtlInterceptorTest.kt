/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.interceptors

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AttributePath
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.KeyFilter
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.LiteralExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.UpdateAction
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.UpdateClauseExpr
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.model.SchemaAttributes
import aws.sdk.kotlin.hll.dynamodbmapper.operations.PartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.operations.QueryRequest
import aws.sdk.kotlin.hll.dynamodbmapper.operations.UpdateItemRequest
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.HReqContext
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.testMapperContext
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.testSchema
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.hours

class UpdateItemTtlInterceptorTest {
    @Test
    fun testTtlInterceptor() {
        val testClock = ManualClock(Instant.fromEpochSeconds(0))
        val interceptor = TtlInterceptor(testClock)
        val ttlFields = mapOf("expiresAt" to 1.hours.inWholeSeconds)

        val req = UpdateItemRequest.PartitionKey<KeyType.Key1<String>> {
            update {
                set {
                    attr["foo"] = "bar"
                    attr["baz"] = "qux"
                }
            }
        }

        val ctx = createContext(req, ttlFields)

        @Suppress("UNCHECKED_CAST")
        val result1 = interceptor.modifyBeforeSerialization(ctx).highLevelRequest as UpdateItemRequest.PartitionKey<KeyType.Key1<String>>

        val updateExpr1 = assertNotNull(result1.update, "Found null update expression")
        val updates1 = updateExpr1.set.updates.associateBy { it.target.toString() }
        assertEquals(3, updates1.size)

        assertEquals(UpdateClauseExpr(UpdateAction.SET, AttributePath("foo"), LiteralExpr("bar")), updates1["foo"])
        assertEquals(UpdateClauseExpr(UpdateAction.SET, AttributePath("baz"), LiteralExpr("qux")), updates1["baz"])
        assertEquals(UpdateClauseExpr(UpdateAction.SET, AttributePath("expiresAt"), LiteralExpr(3600)), updates1["expiresAt"])

        testClock.advance(1.hours)

        @Suppress("UNCHECKED_CAST")
        val result2 = interceptor.modifyBeforeSerialization(ctx).highLevelRequest as UpdateItemRequest.PartitionKey<KeyType.Key1<String>>

        val updateExpr2 = assertNotNull(result2.update, "Found null update expression")
        val updates2 = updateExpr2.set.updates.associateBy { it.target.toString() }
        assertEquals(3, updates2.size)

        assertEquals(UpdateClauseExpr(UpdateAction.SET, AttributePath("foo"), LiteralExpr("bar")), updates2["foo"])
        assertEquals(UpdateClauseExpr(UpdateAction.SET, AttributePath("baz"), LiteralExpr("qux")), updates2["baz"])
        assertEquals(UpdateClauseExpr(UpdateAction.SET, AttributePath("expiresAt"), LiteralExpr(7200)), updates2["expiresAt"])
    }

    @Test
    fun testTtlInterceptorNoTtlFields() {
        val testClock = ManualClock(Instant.fromEpochSeconds(0))
        val interceptor = TtlInterceptor(testClock)

        val req = UpdateItemRequest.PartitionKey<KeyType.Key1<String>> {
            update {
                set {
                    attr["foo"] = "bar"
                    attr["baz"] = "qux"
                }
            }
        }

        val ctx = createContext(req)
        val result = interceptor.modifyBeforeSerialization(ctx).highLevelRequest
        assertEquals(req, result)
    }

    @Test
    fun testTtlInterceptorNonUpdateItemRequest() {
        val testClock = ManualClock(Instant.fromEpochSeconds(0))
        val interceptor = TtlInterceptor(testClock)

        val req = QueryRequest.PartitionKey<KeyType.Key1<String>> {
            keyCondition = KeyFilter("foo")
            filter {
                attr["baz"] eq "qux"
            }
        }

        val ctx = createContext(req)
        val result = interceptor.modifyBeforeSerialization(ctx).highLevelRequest
        assertEquals(req, result)
    }
}

private fun createContext(
    highLevelRequest: Any,
    ttlFields: Map<String, Long>? = null,
): HReqContext<Any, ItemSchema<Any>, Any> {
    val schema = testSchema { ttlFields?.let { SchemaAttributes.TtlFields to it } }
    val mapperContext = testMapperContext()
    return HReqContext(highLevelRequest, schema, mapperContext)
}
