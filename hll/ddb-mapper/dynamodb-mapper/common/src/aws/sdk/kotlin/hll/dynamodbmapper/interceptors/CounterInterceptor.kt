/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.interceptors

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.UpdateExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.UpdateDslImpl
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.model.SchemaAttributes
import aws.sdk.kotlin.hll.dynamodbmapper.operations.BatchWriteItemRequest
import aws.sdk.kotlin.hll.dynamodbmapper.operations.BatchWriteItemRequestTable
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TransactWriteItemsAction
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TransactWriteItemsRequest
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TransactWriteItemsRequestTable
import aws.sdk.kotlin.hll.dynamodbmapper.operations.Update
import aws.sdk.kotlin.hll.dynamodbmapper.operations.UpdateItemRequest
import aws.sdk.kotlin.hll.dynamodbmapper.operations.copy
import aws.sdk.kotlin.hll.dynamodbmapper.operations.toBuilder
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.HReqContext
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.InterceptorAny
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LReqContext
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.SerializeInput
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.BatchWriteItemRequest as LowLevelBatchWriteItemRequest
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest as LowLevelPutItemRequest
import aws.sdk.kotlin.services.dynamodb.model.TransactWriteItemsRequest as LowLevelTransactWriteItemsRequest

/**
 * Interceptor that handles counter fields defined on schema attributes by incrementing them on each mutating operation.
 * This affects the following operations:
 * * `PutItem` is intercepted in [modifyBeforeInvocation] to update the low-level request item with the incremented
 *   field values
 * * `UpdateItem` is intercepted in [modifyBeforeSerialization] to augment the high-level request's update expression
 * * `BatchWriteItem` is intercepted in [modifyBeforeInvocation] to update the items of each low-level put request
 * * `TransactWriteItems` is intercepted in both [modifyBeforeSerialization] (to augment the update expression of each
 *   `Update` action) and [modifyBeforeInvocation] (to update the items of each low-level `Put` action)
 *
 * Counter fields are read from the schema associated with each operation. For the single-item operations this is the
 * operation's [serialize schema][HReqContext.serializeSchema]; for the multi-table operations (`BatchWriteItem` and
 * `TransactWriteItems`) the operation-level schema is a placeholder, so the per-table schemas carried by the request
 * are used instead.
 */
public class CounterInterceptor : InterceptorAny {
    override fun modifyBeforeInvocation(ctx: LReqContext<Any, ItemSchema<Any>, Any, Any>): Any = when (val originalReq = ctx.lowLevelRequest) {
        is LowLevelPutItemRequest -> {
            val counterFields = ctx.serializeSchema.attributes.getOrNull(SchemaAttributes.CounterFields)
                ?: return originalReq
            handlePutItem(originalReq, counterFields)
        }

        is LowLevelBatchWriteItemRequest ->
            handleBatchWriteItem(originalReq, ctx.highLevelRequest as BatchWriteItemRequest)

        is LowLevelTransactWriteItemsRequest ->
            handleTransactWriteItems(originalReq, ctx.highLevelRequest as TransactWriteItemsRequest)

        else -> originalReq
    }

    override fun modifyBeforeSerialization(ctx: HReqContext<Any, ItemSchema<Any>, Any>): SerializeInput<Any, ItemSchema<Any>, Any> = when (val originalReq = ctx.highLevelRequest) {
        is UpdateItemRequest -> {
            val counterFields = ctx.serializeSchema.attributes.getOrNull(SchemaAttributes.CounterFields)
                ?: return ctx
            SerializeInput(handleUpdateItem(originalReq, counterFields), ctx.serializeSchema)
        }

        is TransactWriteItemsRequest -> {
            if (originalReq.tables.none { it.itemSchema.counterFields.isNotEmpty() }) return ctx
            SerializeInput(handleTransactWriteItemsUpdates(originalReq), ctx.serializeSchema)
        }

        else -> ctx
    }

    private fun handlePutItem(request: LowLevelPutItemRequest, counterFields: Set<String>): LowLevelPutItemRequest {
        val item = request.item ?: return request
        return request.copy { this.item = incrementCounters(item, counterFields) }
    }

    private fun handleBatchWriteItem(
        request: LowLevelBatchWriteItemRequest,
        highLevelRequest: BatchWriteItemRequest,
    ): LowLevelBatchWriteItemRequest {
        val counterFieldsByTable = highLevelRequest.tables
            .associate { it.tableName to it.itemSchema.counterFields }
            .filterValues { it.isNotEmpty() }

        if (counterFieldsByTable.isEmpty()) return request

        return request.copy {
            requestItems = request.requestItems?.mapValues { (tableName, writeRequests) ->
                val counterFields = counterFieldsByTable[tableName] ?: return@mapValues writeRequests
                writeRequests.map { writeRequest ->
                    val putRequest = writeRequest.putRequest ?: return@map writeRequest // leave delete requests untouched
                    writeRequest.copy {
                        this.putRequest = putRequest.copy { item = incrementCounters(putRequest.item, counterFields) }
                    }
                }
            }
        }
    }

    private fun handleTransactWriteItems(
        request: LowLevelTransactWriteItemsRequest,
        highLevelRequest: TransactWriteItemsRequest,
    ): LowLevelTransactWriteItemsRequest {
        val counterFieldsByTable = highLevelRequest.tables
            .associate { it.tableName to it.itemSchema.counterFields }
            .filterValues { it.isNotEmpty() }

        if (counterFieldsByTable.isEmpty()) return request

        return request.copy {
            transactItems = request.transactItems?.map { transactItem ->
                val put = transactItem.put ?: return@map transactItem // only `Put` actions; `Update` is handled high-level
                val counterFields = counterFieldsByTable[put.tableName] ?: return@map transactItem
                transactItem.copy { this.put = put.copy { item = incrementCounters(put.item, counterFields) } }
            }
        }
    }

    private fun handleUpdateItem(
        request: UpdateItemRequest,
        counterFields: Set<String>,
    ): UpdateItemRequest = when (request) {
        is UpdateItemRequest.PartitionKey<*> ->
            request.toBuilder().apply { update = augmentUpdateExpr(update, counterFields) }.build()

        is UpdateItemRequest.CompositeKey<*, *> ->
            request.toBuilder().apply { update = augmentUpdateExpr(update, counterFields) }.build()
    }

    private fun handleTransactWriteItemsUpdates(request: TransactWriteItemsRequest): TransactWriteItemsRequest {
        val newTables = request.tables.map { table ->
            val counterFields = table.itemSchema.counterFields
            if (counterFields.isEmpty()) table else augmentTableUpdates(table, counterFields)
        }
        return request.copy { tables = newTables }
    }

    private fun augmentTableUpdates(
        table: TransactWriteItemsRequestTable<*>,
        counterFields: Set<String>,
    ): TransactWriteItemsRequestTable<*> = when (table) {
        is TransactWriteItemsRequestTable.PartitionKey<*, *> -> augmentPartitionKeyTable(table, counterFields)
        is TransactWriteItemsRequestTable.CompositeKey<*, *, *> -> augmentCompositeKeyTable(table, counterFields)
    }

    private fun <T, PK : KeyType> augmentPartitionKeyTable(
        table: TransactWriteItemsRequestTable.PartitionKey<T, PK>,
        counterFields: Set<String>,
    ): TransactWriteItemsRequestTable.PartitionKey<T, PK> = if (table.updates.isEmpty()) {
        table
    } else {
        TransactWriteItemsRequestTable(
            conditionChecks = table.conditionChecks,
            deletes = table.deletes,
            puts = table.puts,
            schema = table.schema,
            tableName = table.tableName,
            updates = table.updates.map { it.augment(counterFields) },
        )
    }

    private fun <T, PK : KeyType, SK : KeyType> augmentCompositeKeyTable(
        table: TransactWriteItemsRequestTable.CompositeKey<T, PK, SK>,
        counterFields: Set<String>,
    ): TransactWriteItemsRequestTable.CompositeKey<T, PK, SK> = if (table.updates.isEmpty()) {
        table
    } else {
        TransactWriteItemsRequestTable(
            conditionChecks = table.conditionChecks,
            deletes = table.deletes,
            puts = table.puts,
            schema = table.schema,
            tableName = table.tableName,
            updates = table.updates.map { it.augment(counterFields) },
        )
    }

    private fun <E> TransactWriteItemsAction.Update<E>.augment(
        counterFields: Set<String>,
    ): TransactWriteItemsAction.Update<E> = TransactWriteItemsAction.Update(
        condition = condition,
        entity = entity,
        returnValuesOnConditionCheckFailure = returnValuesOnConditionCheckFailure,
        update = augmentUpdateExpr(update, counterFields),
    )

    /**
     * Increments each counter field in [item], defaulting any missing or null field to zero before incrementing.
     */
    private fun incrementCounters(
        item: Map<String, AttributeValue>,
        counterFields: Set<String>,
    ): Map<String, AttributeValue> {
        val newItem = item.toMutableMap()
        counterFields.forEach { fieldName ->
            val currentValue = newItem[fieldName]?.asN()?.toLong() ?: 0L
            newItem[fieldName] = AttributeValue.N((currentValue + 1).toString())
        }
        return newItem
    }

    /**
     * Returns a new [UpdateExpr] which augments [existing] with a `SET` clause incrementing each counter field via
     * `if_not_exists(field, 0) + 1`.
     */
    private fun augmentUpdateExpr(existing: UpdateExpr?, counterFields: Set<String>): UpdateExpr = UpdateDslImpl(existing).apply {
        set {
            counterFields.forEach { fieldName ->
                attr[fieldName] = attr[fieldName].orElse(0) + 1
            }
        }
    }.toExpression()
}

private val ItemSchema<*>.counterFields: Set<String>
    get() = attributes.getOrNull(SchemaAttributes.CounterFields).orEmpty()

private val BatchWriteItemRequestTable<*>.itemSchema: ItemSchema<*>
    get() = when (this) {
        is BatchWriteItemRequestTable.PartitionKey<*, *> -> schema
        is BatchWriteItemRequestTable.CompositeKey<*, *, *> -> schema
    }

private val TransactWriteItemsRequestTable<*>.itemSchema: ItemSchema<*>
    get() = when (this) {
        is TransactWriteItemsRequestTable.PartitionKey<*, *> -> schema
        is TransactWriteItemsRequestTable.CompositeKey<*, *, *> -> schema
    }
