/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.interceptors

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AdditiveExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AdditiveOperation
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AttributePath
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.LiteralExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.ScalarFunc
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.ScalarFuncExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.UpdateAction
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.UpdateClauseExpr
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.Key
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.model.SchemaAttributes
import aws.sdk.kotlin.hll.dynamodbmapper.operations.PartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TransactWriteItemsAction
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TransactWriteItemsRequest
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TransactWriteItemsRequestTable
import aws.sdk.kotlin.hll.dynamodbmapper.operations.Update
import aws.sdk.kotlin.hll.dynamodbmapper.operations.UpdateItemRequest
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.HReqContext
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LReqContext
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.testConverter
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.testMapperContext
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.testSchema
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.TransactWriteItem
import aws.smithy.kotlin.runtime.collections.attributesOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import aws.sdk.kotlin.services.dynamodb.model.TransactWriteItemsRequest as LowLevelTransactWriteItemsRequest

class TransactWriteItemsCounterInterceptorTest {
    private val schema = ItemSchema(
        testConverter(),
        KeySpec.string("id"),
        attributesOf { SchemaAttributes.CounterFields to setOf("counter1") },
    )

    @Test
    fun testIncrementsPutActionsOnly() {
        val interceptor = CounterInterceptor()

        val highLevelRequest = TransactWriteItemsRequest {
            tables = listOf(
                TransactWriteItemsRequestTable(
                    conditionChecks = emptyList(),
                    deletes = emptyList(),
                    puts = emptyList(),
                    schema = schema,
                    tableName = "table1",
                    updates = emptyList(),
                ),
            )
        }

        val lowLevelRequest = LowLevelTransactWriteItemsRequest {
            transactItems = listOf(
                TransactWriteItem {
                    put {
                        tableName = "table1"
                        item = mapOf("id" to AttributeValue.S("a"), "counter1" to AttributeValue.N("5"))
                    }
                },
                TransactWriteItem {
                    update {
                        tableName = "table1"
                        key = mapOf("id" to AttributeValue.S("b"))
                        updateExpression = "SET #foo = :bar"
                    }
                },
                TransactWriteItem {
                    delete {
                        tableName = "table1"
                        key = mapOf("id" to AttributeValue.S("c"))
                    }
                },
            )
        }

        val result = interceptor.modifyBeforeInvocation(
            lReqContext(highLevelRequest, lowLevelRequest),
        ) as LowLevelTransactWriteItemsRequest

        assertEquals("6", result.transactItems!![0].put!!.item["counter1"]!!.asN())
        assertNotNull(result.transactItems!![1].update) // update action untouched at low-level phase
        assertNotNull(result.transactItems!![2].delete)
    }

    @Test
    fun testAugmentsUpdateActions() {
        val interceptor = CounterInterceptor()

        val seedUpdate = UpdateItemRequest.PartitionKey<KeyType.Key1<String>> {
            update { set { attr["foo"] = "bar" } }
        }.update!!

        val highLevelRequest = TransactWriteItemsRequest {
            tables = listOf(
                TransactWriteItemsRequestTable(
                    conditionChecks = emptyList(),
                    deletes = emptyList(),
                    puts = emptyList(),
                    schema = schema,
                    tableName = "table1",
                    updates = listOf(TransactWriteItemsAction.Update(null, Key("pk"), null, seedUpdate)),
                ),
            )
        }

        val result = interceptor.modifyBeforeSerialization(hReqContext(highLevelRequest)).highLevelRequest as TransactWriteItemsRequest

        val table = result.tables.single() as TransactWriteItemsRequestTable.PartitionKey<*, *>
        val update = assertNotNull(table.updates.single().update)
        val updates = update.updates.associateBy { it.target.toString() }

        assertEquals(2, updates.size)
        assertEquals(UpdateClauseExpr(UpdateAction.SET, AttributePath("foo"), LiteralExpr("bar")), updates["foo"])
        assertEquals(
            UpdateClauseExpr(
                UpdateAction.SET,
                AttributePath("counter1"),
                AdditiveExpr(
                    AdditiveOperation.ADD,
                    ScalarFuncExpr(ScalarFunc.IF_NOT_EXISTS, AttributePath("counter1"), LiteralExpr(0)),
                    LiteralExpr(1),
                ),
            ),
            updates["counter1"],
        )
    }

    @Test
    fun testNoCounterFieldsLeavesUpdatesUnchanged() {
        val interceptor = CounterInterceptor()
        val schemaWithoutCounter = ItemSchema(testConverter(), KeySpec.string("id"))

        val seedUpdate = UpdateItemRequest.PartitionKey<KeyType.Key1<String>> {
            update { set { attr["foo"] = "bar" } }
        }.update!!

        val highLevelRequest = TransactWriteItemsRequest {
            tables = listOf(
                TransactWriteItemsRequestTable(
                    conditionChecks = emptyList(),
                    deletes = emptyList(),
                    puts = emptyList(),
                    schema = schemaWithoutCounter,
                    tableName = "table1",
                    updates = listOf(TransactWriteItemsAction.Update(null, Key("pk"), null, seedUpdate)),
                ),
            )
        }

        val result = interceptor.modifyBeforeSerialization(hReqContext(highLevelRequest)).highLevelRequest
        assertEquals(highLevelRequest, result)

        val table = (result as TransactWriteItemsRequest).tables.single() as TransactWriteItemsRequestTable.PartitionKey<*, *>
        assertNull(table.updates.single().update.updates.associateBy { it.target.toString() }["counter1"])
    }

    @Test
    fun testNoCounterFieldsIsNoOp() {
        val interceptor = CounterInterceptor()
        val schemaWithoutCounter = ItemSchema(testConverter(), KeySpec.string("id"))

        val highLevelRequest = TransactWriteItemsRequest {
            tables = listOf(
                TransactWriteItemsRequestTable(
                    conditionChecks = emptyList(),
                    deletes = emptyList(),
                    puts = emptyList(),
                    schema = schemaWithoutCounter,
                    tableName = "table1",
                    updates = emptyList(),
                ),
            )
        }

        val lowLevelRequest = LowLevelTransactWriteItemsRequest {
            transactItems = listOf(
                TransactWriteItem {
                    put {
                        tableName = "table1"
                        item = mapOf("id" to AttributeValue.S("a"))
                    }
                },
            )
        }

        val result = interceptor.modifyBeforeInvocation(lReqContext(highLevelRequest, lowLevelRequest))
        assertSame(lowLevelRequest, result)
    }

    private fun lReqContext(
        highLevelRequest: Any,
        lowLevelRequest: Any,
    ): LReqContext<Any, ItemSchema<Any>, Any, Any> = LReqContext(highLevelRequest, testSchema { }, testMapperContext(), lowLevelRequest)

    private fun hReqContext(highLevelRequest: Any): HReqContext<Any, ItemSchema<Any>, Any> = HReqContext(highLevelRequest, testSchema { }, testMapperContext())
}
