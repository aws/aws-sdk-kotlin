/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.interceptors

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AdditiveExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AdditiveOperation
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AttributePath
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.KeyFilter
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.LiteralExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.ScalarFunc
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.ScalarFuncExpr
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UpdateItemCounterInterceptorTest {
    @Test
    fun testCounterInterceptor() {
        val counterFields = setOf("counter1", "counter2")
        val interceptor = CounterInterceptor()

        val req = UpdateItemRequest.PartitionKey<KeyType.Key1<String>> {
            update {
                set {
                    attr["foo"] = "bar"
                    attr["baz"] = "qux"
                }
            }
        }

        val ctx = createContext(req, counterFields)

        @Suppress("UNCHECKED_CAST")
        val result = interceptor.modifyBeforeSerialization(ctx).highLevelRequest as UpdateItemRequest.PartitionKey<KeyType.Key1<String>>

        val updateExpr = assertNotNull(result.update, "Found null update expression")
        val updates = updateExpr.set.updates.associateBy { it.target.toString() }
        assertEquals(4, updates.size)

        assertEquals(UpdateClauseExpr(UpdateAction.SET, AttributePath("foo"), LiteralExpr("bar")), updates["foo"])
        assertEquals(UpdateClauseExpr(UpdateAction.SET, AttributePath("baz"), LiteralExpr("qux")), updates["baz"])

        assertEquals(
            UpdateClauseExpr(
                UpdateAction.SET,
                AttributePath("counter1"),
                AdditiveExpr(
                    AdditiveOperation.ADD,
                    ScalarFuncExpr(
                        ScalarFunc.IF_NOT_EXISTS,
                        AttributePath("counter1"),
                        LiteralExpr(0),
                    ),
                    LiteralExpr(1),
                ),
            ),
            updates["counter1"],
        )

        assertEquals(
            UpdateClauseExpr(
                UpdateAction.SET,
                AttributePath("counter2"),
                AdditiveExpr(
                    AdditiveOperation.ADD,
                    ScalarFuncExpr(
                        ScalarFunc.IF_NOT_EXISTS,
                        AttributePath("counter2"),
                        LiteralExpr(0),
                    ),
                    LiteralExpr(1),
                ),
            ),
            updates["counter2"],
        )
    }

    @Test
    fun testCounterInterceptorNoCounterFields() {
        val interceptor = CounterInterceptor()

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
    fun testCounterInterceptorNonUpdateItemRequest() {
        val interceptor = CounterInterceptor()

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
    counterFields: Set<String>? = null,
): HReqContext<Any, ItemSchema<Any>, Any> {
    val schema = testSchema { counterFields?.let { SchemaAttributes.CounterFields to it } }
    val mapperContext = testMapperContext()
    return HReqContext(highLevelRequest, schema, mapperContext)
}
