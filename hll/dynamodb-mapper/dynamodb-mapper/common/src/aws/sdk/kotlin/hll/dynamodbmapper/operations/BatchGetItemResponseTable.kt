/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.items.itemToCk
import aws.sdk.kotlin.hll.dynamodbmapper.items.itemToPk
import aws.sdk.kotlin.hll.dynamodbmapper.model.Table
import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem
import aws.sdk.kotlin.hll.dynamodbmapper.operations.internal.BatchGetItemResponseTableCompositeKeyImpl
import aws.sdk.kotlin.hll.dynamodbmapper.operations.internal.BatchGetItemResponseTablePartitionKeyImpl
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.KeysAndAttributes

public sealed interface BatchGetItemResponseTable<T> {
    public companion object { }

    public val items: List<T>
    public val tableName: String

    public interface PartitionKey<T, PK : KeyType> : BatchGetItemResponseTable<T> {
        public val schema: ItemSchema.PartitionKey<T, PK>
        public val unprocessedKeys: List<PK>
    }

    public interface CompositeKey<T, PK : KeyType, SK : KeyType> : BatchGetItemResponseTable<T> {
        public val schema: ItemSchema.CompositeKey<T, PK, SK>
        public val unprocessedKeys: List<Pair<PK, SK>>
    }
}

@Suppress("ktlint:standard:function-naming")
public fun <T, PK : KeyType> BatchGetItemResponseTable(
    items: List<T>,
    tableName: String,
    schema: ItemSchema.PartitionKey<T, PK>,
    unprocessedKeys: List<PK>,
): BatchGetItemResponseTable.PartitionKey<T, PK> = BatchGetItemResponseTablePartitionKeyImpl(
    items,
    tableName,
    schema,
    unprocessedKeys,
)

@Suppress("ktlint:standard:function-naming")
public fun <T, PK : KeyType, SK : KeyType> BatchGetItemResponseTable(
    items: List<T>,
    tableName: String,
    schema: ItemSchema.CompositeKey<T, PK, SK>,
    unprocessedKeys: List<Pair<PK, SK>>,
): BatchGetItemResponseTable.CompositeKey<T, PK, SK> = BatchGetItemResponseTableCompositeKeyImpl(
    items,
    tableName,
    schema,
    unprocessedKeys,
)

@Suppress("ktlint:standard:function-naming")
internal fun BatchGetItemResponseTables(
    responses: Map<String, List<Map<String, AttributeValue>>>?,
    unprocessedKeys: Map<String, KeysAndAttributes>?,
    requestTables: List<BatchGetItemRequestTable<*>>,
): List<BatchGetItemResponseTable<*>> = requestTables
    .map { requestTable ->
        val itemValues = responses?.get(requestTable.tableName)?.map { it.toItem() }.orEmpty()
        val keyValues = unprocessedKeys?.get(requestTable.tableName)?.keys.orEmpty()

        fun <T, PK : KeyType> responseTable(requestTable: BatchGetItemRequestTable.PartitionKey<T, PK>) = run {
            val items = itemValues.map { item -> requestTable.schema.converter.convertLeft(item) }
            val keys = keyValues.map { key -> itemToPk(requestTable.schema, key.toItem()) }
            BatchGetItemResponseTable(items, requestTable.tableName, requestTable.schema, keys)
        }

        fun <T, PK : KeyType, SK : KeyType> responseTable(requestTable: BatchGetItemRequestTable.CompositeKey<T, PK, SK>) = run {
            val items = itemValues.map { item -> requestTable.schema.converter.convertLeft(item) }
            val keys = keyValues.map { key -> itemToCk(requestTable.schema, key.toItem()) }
            BatchGetItemResponseTable(items, requestTable.tableName, requestTable.schema, keys)
        }

        when (requestTable) {
            is BatchGetItemRequestTable.PartitionKey<*, *> -> responseTable(requestTable)
            is BatchGetItemRequestTable.CompositeKey<*, *, *> -> responseTable(requestTable)
        }
    }

public fun <T, PK : KeyType> BatchGetItemResponse.table(
    table: Table.PartitionKey<T, PK>,
): BatchGetItemResponseTable.PartitionKey<T, PK> = tables
    .filterIsInstance<BatchGetItemResponseTable.PartitionKey<T, PK>>()
    .find { it.tableName == table.tableName && it.schema == table.schema }
    ?: error("""Table name "${table.tableName}" not found in results""")

public fun <T, PK : KeyType, SK : KeyType> BatchGetItemResponse.table(
    table: Table.CompositeKey<T, PK, SK>,
): BatchGetItemResponseTable.CompositeKey<T, PK, SK> = tables
    .filterIsInstance<BatchGetItemResponseTable.CompositeKey<T, PK, SK>>()
    .find { it.tableName == table.tableName && it.schema == table.schema }
    ?: error("""Table name "${table.tableName}" not found in results""")
