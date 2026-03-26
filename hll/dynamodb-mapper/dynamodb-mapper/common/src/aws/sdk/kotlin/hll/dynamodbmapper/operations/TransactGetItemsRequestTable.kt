/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.items.keysToItem
import aws.sdk.kotlin.hll.dynamodbmapper.operations.internal.TransactGetItemsRequestTableCompositeKeyImpl
import aws.sdk.kotlin.hll.dynamodbmapper.operations.internal.TransactGetItemsRequestTablePartitionKeyImpl
import aws.sdk.kotlin.services.dynamodb.model.TransactGetItem

public sealed interface TransactGetItemsRequestTable<T> {
    public val tableName: String

    public interface PartitionKey<T, PK : KeyType> : TransactGetItemsRequestTable<T> {
        public val keys: List<PK>
        public val schema: ItemSchema.PartitionKey<T, PK>
    }

    public interface CompositeKey<T, PK : KeyType, SK : KeyType> : TransactGetItemsRequestTable<T> {
        public val keys: List<Pair<PK, SK>>
        public val schema: ItemSchema.CompositeKey<T, PK, SK>
    }
}

@Suppress("ktlint:standard:function-naming")
public fun <T, PK : KeyType> TransactGetItemsRequestTable(
    tableName: String,
    keys: List<PK>,
    schema: ItemSchema.PartitionKey<T, PK>,
): TransactGetItemsRequestTable.PartitionKey<T, PK> = TransactGetItemsRequestTablePartitionKeyImpl(
    tableName,
    keys,
    schema,
)

@Suppress("ktlint:standard:function-naming")
public fun <T, PK : KeyType, SK : KeyType> TransactGetItemsRequestTable(
    tableName: String,
    keys: List<Pair<PK, SK>>,
    schema: ItemSchema.CompositeKey<T, PK, SK>,
): TransactGetItemsRequestTable.CompositeKey<T, PK, SK> = TransactGetItemsRequestTableCompositeKeyImpl(
    tableName,
    keys,
    schema,
)

internal fun List<TransactGetItemsRequestTable<*>>.convert(): List<TransactGetItem> {
    fun <PK : KeyType> pkMap(table: TransactGetItemsRequestTable.PartitionKey<*, PK>) = table
        .keys
        .map { key -> keysToItem(table.schema, key) }

    fun <PK : KeyType, SK : KeyType> ckMap(table: TransactGetItemsRequestTable.CompositeKey<*, PK, SK>) = table
        .keys
        .map { (pk, sk) -> keysToItem(table.schema, pk, sk) }

    return flatMap { table ->
        val keys = when (table) {
            is TransactGetItemsRequestTable.PartitionKey<*, *> -> pkMap(table)
            is TransactGetItemsRequestTable.CompositeKey<*, *, *> -> ckMap(table)
        }

        keys.map { key ->
            TransactGetItem {
                get {
                    tableName = table.tableName
                    this.key = key
                }
            }
        }
    }
}
