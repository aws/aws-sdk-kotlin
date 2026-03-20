/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.items.itemToCk
import aws.sdk.kotlin.hll.dynamodbmapper.items.itemToPk
import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem
import aws.sdk.kotlin.hll.dynamodbmapper.operations.internal.BatchWriteItemResponseTableCompositeKeyImpl
import aws.sdk.kotlin.hll.dynamodbmapper.operations.internal.BatchWriteItemResponseTablePartitionKeyImpl
import aws.sdk.kotlin.services.dynamodb.model.WriteRequest

public sealed interface BatchWriteItemResponseTable<T> {
    public val tableName: String
    public val unprocessedItems: List<T>

    public interface PartitionKey<T, PK : KeyType> : BatchWriteItemResponseTable<T> {
        public val schema: ItemSchema.PartitionKey<T, PK>
        public val unprocessedKeys: List<PK>
    }

    public interface CompositeKey<T, PK : KeyType, SK : KeyType> : BatchWriteItemResponseTable<T> {
        public val schema: ItemSchema.CompositeKey<T, PK, SK>
        public val unprocessedKeys: List<Pair<PK, SK>>
    }
}

@Suppress("ktlint:standard:function-naming")
public fun <T, PK : KeyType> BatchWriteItemResponseTable(
    tableName: String,
    unprocessedItems: List<T>,
    schema: ItemSchema.PartitionKey<T, PK>,
    unprocessedKeys: List<PK>,
): BatchWriteItemResponseTable.PartitionKey<T, PK> = BatchWriteItemResponseTablePartitionKeyImpl(
    tableName,
    unprocessedItems,
    schema,
    unprocessedKeys,
)

@Suppress("ktlint:standard:function-naming")
public fun <T, PK : KeyType, SK : KeyType> BatchWriteItemResponseTable(
    tableName: String,
    unprocessedItems: List<T>,
    schema: ItemSchema.CompositeKey<T, PK, SK>,
    unprocessedKeys: List<Pair<PK, SK>>,
): BatchWriteItemResponseTable.CompositeKey<T, PK, SK> = BatchWriteItemResponseTableCompositeKeyImpl(
    tableName,
    unprocessedItems,
    schema,
    unprocessedKeys,
)

@Suppress("ktlint:standard:function-naming")
internal fun BatchWriteItemResponseTables(
    unprocessedItems: Map<String, List<WriteRequest>>?,
    requestTables: List<BatchWriteItemRequestTable<*>>,
): List<BatchWriteItemResponseTable<*>> = requestTables
    .map { requestTable ->
        val tableItems = unprocessedItems?.get(requestTable.tableName).orEmpty()
        val keyValues = tableItems.mapNotNull { it.deleteRequest?.key }
        val itemValues = tableItems.mapNotNull { it.putRequest?.item?.toItem() }

        fun <T, PK : KeyType> responseTable(requestTable: BatchWriteItemRequestTable.PartitionKey<T, PK>) = run {
            val items = itemValues.map { item -> requestTable.schema.converter.convertLeft(item) }
            val keys = keyValues.map { key -> itemToPk(requestTable.schema, key.toItem()) }
            BatchWriteItemResponseTable(requestTable.tableName, items, requestTable.schema, keys)
        }

        fun <T, PK : KeyType, SK : KeyType> responseTable(requestTable: BatchWriteItemRequestTable.CompositeKey<T, PK, SK>) = run {
            val items = itemValues.map { item -> requestTable.schema.converter.convertLeft(item) }
            val keys = keyValues.map { key -> itemToCk(requestTable.schema, key.toItem()) }
            BatchWriteItemResponseTable(requestTable.tableName, items, requestTable.schema, keys)
        }

        when (requestTable) {
            is BatchWriteItemRequestTable.PartitionKey<*, *> -> responseTable(requestTable)
            is BatchWriteItemRequestTable.CompositeKey<*, *, *> -> responseTable(requestTable)
        }
    }
