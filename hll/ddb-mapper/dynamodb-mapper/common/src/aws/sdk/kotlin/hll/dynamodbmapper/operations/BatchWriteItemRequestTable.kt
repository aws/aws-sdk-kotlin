/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.items.keysToItem
import aws.sdk.kotlin.hll.dynamodbmapper.operations.internal.BatchWriteItemRequestTableCompositeKeyImpl
import aws.sdk.kotlin.hll.dynamodbmapper.operations.internal.BatchWriteItemRequestTablePartitionKeyImpl
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.WriteRequest

public sealed interface BatchWriteItemRequestTable<T> {
    public val putItems: List<T>
    public val tableName: String

    public interface PartitionKey<T, PK : KeyType> : BatchWriteItemRequestTable<T> {
        public val deleteKeys: List<PK>
        public val schema: ItemSchema.PartitionKey<T, PK>
    }

    public interface CompositeKey<T, PK : KeyType, SK : KeyType> : BatchWriteItemRequestTable<T> {
        public val deleteKeys: List<Pair<PK, SK>>
        public val schema: ItemSchema.CompositeKey<T, PK, SK>
    }
}

@Suppress("ktlint:standard:function-naming")
public fun <T, PK : KeyType> BatchWriteItemRequestTable(
    putItems: List<T>,
    tableName: String,
    deleteKeys: List<PK>,
    schema: ItemSchema.PartitionKey<T, PK>,
): BatchWriteItemRequestTable.PartitionKey<T, PK> = BatchWriteItemRequestTablePartitionKeyImpl(
    putItems,
    tableName,
    deleteKeys,
    schema,
)

@Suppress("ktlint:standard:function-naming")
public fun <T, PK : KeyType, SK : KeyType> BatchWriteItemRequestTable(
    putItems: List<T>,
    tableName: String,
    deleteKeys: List<Pair<PK, SK>>,
    schema: ItemSchema.CompositeKey<T, PK, SK>,
): BatchWriteItemRequestTable.CompositeKey<T, PK, SK> = BatchWriteItemRequestTableCompositeKeyImpl(
    putItems,
    tableName,
    deleteKeys,
    schema,
)

internal fun List<BatchWriteItemRequestTable<*>>.convert(): Map<String, List<WriteRequest>> {
    fun <T, PK : KeyType> pkMap(
        table: BatchWriteItemRequestTable.PartitionKey<T, PK>,
    ) = table.deleteKeys.map { key -> deleteRequest(keysToItem(table.schema, key)) } +
        table.putItems.map { item -> putRequest(table.schema.converter.convertRight(item)) }

    fun <T, PK : KeyType, SK : KeyType> ckMap(
        table: BatchWriteItemRequestTable.CompositeKey<T, PK, SK>,
    ) = table.deleteKeys.map { (pk, sk) -> deleteRequest(keysToItem(table.schema, pk, sk)) } +
        table.putItems.map { item -> putRequest(table.schema.converter.convertRight(item)) }

    return associate { table ->
        val writes = when (table) {
            is BatchWriteItemRequestTable.PartitionKey<*, *> -> pkMap(table)
            is BatchWriteItemRequestTable.CompositeKey<*, *, *> -> ckMap(table)
        }

        table.tableName to writes
    }
}

private fun deleteRequest(key: Map<String, AttributeValue>) = WriteRequest { deleteRequest { this.key = key } }
private fun putRequest(item: Map<String, AttributeValue>) = WriteRequest { putRequest { this.item = item } }
