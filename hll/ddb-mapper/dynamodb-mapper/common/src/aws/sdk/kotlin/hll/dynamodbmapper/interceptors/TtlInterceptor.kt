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
import aws.sdk.kotlin.hll.dynamodbmapper.util.av
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.time.Clock
import aws.sdk.kotlin.services.dynamodb.model.BatchWriteItemRequest as LowLevelBatchWriteItemRequest
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest as LowLevelPutItemRequest
import aws.sdk.kotlin.services.dynamodb.model.TransactWriteItemsRequest as LowLevelTransactWriteItemsRequest

/**
 * Interceptor that handles TTL fields defined on schema attributes and sets them to the current time plus their
 * specified lifetimes. This affects the following operations:
 * * `PutItem` is intercepted in [modifyBeforeInvocation] to update the low-level request item with the calculated
 *   field values
 * * `UpdateItem` is intercepted in [modifyBeforeSerialization] to augment the high-level request's update expression
 * * `BatchWriteItem` is intercepted in [modifyBeforeInvocation] to update the items of each low-level put request
 * * `TransactWriteItems` is intercepted in both [modifyBeforeSerialization] (to augment the update expression of each
 *   `Update` action) and [modifyBeforeInvocation] (to update the items of each low-level `Put` action)
 *
 * TTL fields are read from the schema associated with each operation. For the single-item operations this is the
 * operation's [serialize schema][HReqContext.serializeSchema]; for the multi-table operations (`BatchWriteItem` and
 * `TransactWriteItems`) the operation-level schema is a placeholder, so the per-table schemas carried by the request
 * are used instead.
 */
public class TtlInterceptor(private val clock: Clock = Clock.System) : InterceptorAny {
    override fun modifyBeforeInvocation(ctx: LReqContext<Any, ItemSchema<Any>, Any, Any>): Any = when (val originalReq = ctx.lowLevelRequest) {
        is LowLevelPutItemRequest -> {
            val ttlFields = ctx.serializeSchema.attributes.getOrNull(SchemaAttributes.TtlFields)
                ?: return originalReq
            handlePutItem(originalReq, ttlFields)
        }

        is LowLevelBatchWriteItemRequest ->
            handleBatchWriteItem(originalReq, ctx.highLevelRequest as BatchWriteItemRequest)

        is LowLevelTransactWriteItemsRequest ->
            handleTransactWriteItems(originalReq, ctx.highLevelRequest as TransactWriteItemsRequest)

        else -> originalReq
    }

    override fun modifyBeforeSerialization(ctx: HReqContext<Any, ItemSchema<Any>, Any>): SerializeInput<Any, ItemSchema<Any>, Any> = when (val originalReq = ctx.highLevelRequest) {
        is UpdateItemRequest -> {
            val ttlFields = ctx.serializeSchema.attributes.getOrNull(SchemaAttributes.TtlFields)
                ?: return ctx
            SerializeInput(handleUpdateItem(originalReq, ttlFields), ctx.serializeSchema)
        }

        is TransactWriteItemsRequest -> {
            if (originalReq.tables.none { it.itemSchema.ttlFields.isNotEmpty() }) return ctx
            SerializeInput(handleTransactWriteItemsUpdates(originalReq), ctx.serializeSchema)
        }

        else -> ctx
    }

    private fun handlePutItem(request: LowLevelPutItemRequest, ttlFields: Map<String, Long>): LowLevelPutItemRequest {
        val item = request.item ?: return request
        return request.copy { this.item = setTtls(item, ttlFields) }
    }

    private fun handleBatchWriteItem(
        request: LowLevelBatchWriteItemRequest,
        highLevelRequest: BatchWriteItemRequest,
    ): LowLevelBatchWriteItemRequest {
        val ttlFieldsByTable = highLevelRequest.tables
            .associate { it.tableName to it.itemSchema.ttlFields }
            .filterValues { it.isNotEmpty() }

        if (ttlFieldsByTable.isEmpty()) return request

        return request.copy {
            requestItems = request.requestItems?.mapValues { (tableName, writeRequests) ->
                val ttlFields = ttlFieldsByTable[tableName] ?: return@mapValues writeRequests
                writeRequests.map { writeRequest ->
                    val putRequest = writeRequest.putRequest ?: return@map writeRequest // leave delete requests untouched
                    writeRequest.copy {
                        this.putRequest = putRequest.copy { item = setTtls(putRequest.item, ttlFields) }
                    }
                }
            }
        }
    }

    private fun handleTransactWriteItems(
        request: LowLevelTransactWriteItemsRequest,
        highLevelRequest: TransactWriteItemsRequest,
    ): LowLevelTransactWriteItemsRequest {
        val ttlFieldsByTable = highLevelRequest.tables
            .associate { it.tableName to it.itemSchema.ttlFields }
            .filterValues { it.isNotEmpty() }

        if (ttlFieldsByTable.isEmpty()) return request

        return request.copy {
            transactItems = request.transactItems?.map { transactItem ->
                val put = transactItem.put ?: return@map transactItem // only `Put` actions; `Update` is handled high-level
                val ttlFields = ttlFieldsByTable[put.tableName] ?: return@map transactItem
                transactItem.copy { this.put = put.copy { item = setTtls(put.item, ttlFields) } }
            }
        }
    }

    private fun handleUpdateItem(
        request: UpdateItemRequest,
        ttlFields: Map<String, Long>,
    ): UpdateItemRequest = when (request) {
        is UpdateItemRequest.PartitionKey<*> ->
            request.toBuilder().apply { update = augmentUpdateExpr(update, ttlFields) }.build()

        is UpdateItemRequest.CompositeKey<*, *> ->
            request.toBuilder().apply { update = augmentUpdateExpr(update, ttlFields) }.build()
    }

    private fun handleTransactWriteItemsUpdates(request: TransactWriteItemsRequest): TransactWriteItemsRequest {
        val newTables = request.tables.map { table ->
            val ttlFields = table.itemSchema.ttlFields
            if (ttlFields.isEmpty()) table else augmentTableUpdates(table, ttlFields)
        }
        return request.copy { tables = newTables }
    }

    private fun augmentTableUpdates(
        table: TransactWriteItemsRequestTable<*>,
        ttlFields: Map<String, Long>,
    ): TransactWriteItemsRequestTable<*> = when (table) {
        is TransactWriteItemsRequestTable.PartitionKey<*, *> -> augmentPartitionKeyTable(table, ttlFields)
        is TransactWriteItemsRequestTable.CompositeKey<*, *, *> -> augmentCompositeKeyTable(table, ttlFields)
    }

    private fun <T, PK : KeyType> augmentPartitionKeyTable(
        table: TransactWriteItemsRequestTable.PartitionKey<T, PK>,
        ttlFields: Map<String, Long>,
    ): TransactWriteItemsRequestTable.PartitionKey<T, PK> = if (table.updates.isEmpty()) {
        table
    } else {
        TransactWriteItemsRequestTable(
            conditionChecks = table.conditionChecks,
            deletes = table.deletes,
            puts = table.puts,
            schema = table.schema,
            tableName = table.tableName,
            updates = table.updates.map { it.augment(ttlFields) },
        )
    }

    private fun <T, PK : KeyType, SK : KeyType> augmentCompositeKeyTable(
        table: TransactWriteItemsRequestTable.CompositeKey<T, PK, SK>,
        ttlFields: Map<String, Long>,
    ): TransactWriteItemsRequestTable.CompositeKey<T, PK, SK> = if (table.updates.isEmpty()) {
        table
    } else {
        TransactWriteItemsRequestTable(
            conditionChecks = table.conditionChecks,
            deletes = table.deletes,
            puts = table.puts,
            schema = table.schema,
            tableName = table.tableName,
            updates = table.updates.map { it.augment(ttlFields) },
        )
    }

    private fun <E> TransactWriteItemsAction.Update<E>.augment(
        ttlFields: Map<String, Long>,
    ): TransactWriteItemsAction.Update<E> = TransactWriteItemsAction.Update(
        condition = condition,
        entity = entity,
        returnValuesOnConditionCheckFailure = returnValuesOnConditionCheckFailure,
        update = augmentUpdateExpr(update, ttlFields),
    )

    /**
     * Sets each TTL field in [item] to the current time plus its configured lifetime.
     */
    private fun setTtls(
        item: Map<String, AttributeValue>,
        ttlFields: Map<String, Long>,
    ): Map<String, AttributeValue> {
        val now = clock.now().epochSeconds
        val newItem = item.toMutableMap()
        ttlFields.forEach { (fieldName, lifetime) ->
            newItem[fieldName] = av(now + lifetime)
        }
        return newItem
    }

    /**
     * Returns a new [UpdateExpr] which augments [existing] with a `SET` clause setting each TTL field to the current
     * time plus its configured lifetime.
     */
    private fun augmentUpdateExpr(existing: UpdateExpr?, ttlFields: Map<String, Long>): UpdateExpr {
        val now = clock.now().epochSeconds
        return UpdateDslImpl(existing).apply {
            set {
                ttlFields.forEach { (fieldName, lifetime) ->
                    attr[fieldName] = now + lifetime
                }
            }
        }.toExpression()
    }
}

private val ItemSchema<*>.ttlFields: Map<String, Long>
    get() = attributes.getOrNull(SchemaAttributes.TtlFields).orEmpty()

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
