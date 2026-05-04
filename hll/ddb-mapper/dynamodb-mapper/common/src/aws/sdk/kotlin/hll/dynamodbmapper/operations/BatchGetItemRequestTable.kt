/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.items.keysToItem
import aws.sdk.kotlin.hll.dynamodbmapper.operations.internal.BatchGetItemRequestTableCompositeKeyImpl
import aws.sdk.kotlin.hll.dynamodbmapper.operations.internal.BatchGetItemRequestTablePartitionKeyImpl
import aws.sdk.kotlin.services.dynamodb.model.KeysAndAttributes

public sealed interface BatchGetItemRequestTable<T> {
    public val consistentRead: Boolean?
    public val tableName: String

    public interface PartitionKey<T, PK : KeyType> : BatchGetItemRequestTable<T> {
        public val keys: List<PK>
        public val schema: ItemSchema.PartitionKey<T, PK>
    }

    public interface CompositeKey<T, PK : KeyType, SK : KeyType> : BatchGetItemRequestTable<T> {
        public val keys: List<Pair<PK, SK>>
        public val schema: ItemSchema.CompositeKey<T, PK, SK>
    }
}

@Suppress("ktlint:standard:function-naming")
public fun <T, PK : KeyType> BatchGetItemRequestTable(
    consistentRead: Boolean?,
    keys: List<PK>,
    schema: ItemSchema.PartitionKey<T, PK>,
    tableName: String,
): BatchGetItemRequestTable.PartitionKey<T, PK> = BatchGetItemRequestTablePartitionKeyImpl(
    consistentRead,
    keys,
    schema,
    tableName,
)

@Suppress("ktlint:standard:function-naming")
public fun <T, PK : KeyType, SK : KeyType> BatchGetItemRequestTable(
    consistentRead: Boolean?,
    keys: List<Pair<PK, SK>>,
    schema: ItemSchema.CompositeKey<T, PK, SK>,
    tableName: String,
): BatchGetItemRequestTable.CompositeKey<T, PK, SK> = BatchGetItemRequestTableCompositeKeyImpl(
    consistentRead,
    keys,
    schema,
    tableName,
)

internal fun List<BatchGetItemRequestTable<*>>.convert(): Map<String, KeysAndAttributes> {
    fun <PK : KeyType> pkMap(table: BatchGetItemRequestTable.PartitionKey<*, PK>) = table
        .keys
        .map { key -> keysToItem(table.schema, key) }

    fun <PK : KeyType, SK : KeyType> ckMap(table: BatchGetItemRequestTable.CompositeKey<*, PK, SK>) = table
        .keys
        .map { (pk, sk) -> keysToItem(table.schema, pk, sk) }

    return associate { table ->
        val ka = KeysAndAttributes {
            consistentRead = table.consistentRead

            keys = when (table) {
                is BatchGetItemRequestTable.PartitionKey<*, *> -> pkMap(table)
                is BatchGetItemRequestTable.CompositeKey<*, *, *> -> ckMap(table)
            }
        }
        table.tableName to ka
    }
}
