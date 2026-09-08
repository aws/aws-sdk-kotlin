/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.model.Table
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TransactGetItemsRequestTable
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TransactGetItemsRequestTableDsl

internal class TransactGetItemsRequestTableDslPartitionKeyImpl<T, PK : KeyType>(
    val existingTables: List<TransactGetItemsRequestTable<*>>?,
    val table: Table.PartitionKey<T, PK>,
) : TransactGetItemsRequestTableDsl.PartitionKey<T, PK> {
    override var keys: List<PK> = mutableListOf()

    override fun key(key: PK) {
        keys += key
    }

    override fun keys(keys: Iterable<PK>) {
        this.keys += keys
    }

    private fun toTable() = TransactGetItemsRequestTable(table.tableName, keys, table.schema)
    internal fun toTables(): List<TransactGetItemsRequestTable<*>> = existingTables.orEmpty() + toTable()
}

internal class TransactGetItemsRequestTableDslCompositeKeyImpl<T, PK : KeyType, SK : KeyType>(
    val existingTables: List<TransactGetItemsRequestTable<*>>?,
    val table: Table.CompositeKey<T, PK, SK>,
) : TransactGetItemsRequestTableDsl.CompositeKey<T, PK, SK> {
    override var keys: List<Pair<PK, SK>> = mutableListOf()

    override fun key(partitionKey: PK, sortKey: SK) {
        keys += partitionKey to sortKey
    }

    override fun keys(keys: Iterable<Pair<PK, SK>>) {
        this.keys += keys
    }

    private fun toTable() = TransactGetItemsRequestTable(table.tableName, keys, table.schema)
    internal fun toTables(): List<TransactGetItemsRequestTable<*>> = existingTables.orEmpty() + toTable()
}
