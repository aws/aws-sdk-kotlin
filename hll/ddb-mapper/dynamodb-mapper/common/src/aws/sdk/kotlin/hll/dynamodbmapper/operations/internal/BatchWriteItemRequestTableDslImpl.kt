/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.model.Table
import aws.sdk.kotlin.hll.dynamodbmapper.operations.BatchWriteItemRequestTable
import aws.sdk.kotlin.hll.dynamodbmapper.operations.BatchWriteItemRequestTableDsl

internal class BatchWriteItemRequestTableDslPartitionKeyImpl<T, PK : KeyType>(
    val existingTables: List<BatchWriteItemRequestTable<*>>?,
    val table: Table.PartitionKey<T, PK>,
) : BatchWriteItemRequestTableDsl.PartitionKey<T, PK> {
    override var deleteKeys: List<PK> = mutableListOf()
    override var putItems: List<T> = mutableListOf()

    override fun deleteKey(key: PK) {
        deleteKeys += key
    }

    override fun deleteKeys(keys: Iterable<PK>) {
        deleteKeys += keys
    }

    override fun putItem(item: T) {
        putItems += item
    }

    override fun putItems(items: Iterable<T>) {
        putItems += items
    }

    private fun toTable() = BatchWriteItemRequestTable(putItems, table.tableName, deleteKeys, table.schema)
    internal fun toTables(): List<BatchWriteItemRequestTable<*>> = existingTables.orEmpty() + toTable()
}

internal class BatchWriteItemRequestTableDslCompositeKeyImpl<T, PK : KeyType, SK : KeyType>(
    val existingTables: List<BatchWriteItemRequestTable<*>>?,
    val table: Table.CompositeKey<T, PK, SK>,
) : BatchWriteItemRequestTableDsl.CompositeKey<T, PK, SK> {
    override var deleteKeys: List<Pair<PK, SK>> = mutableListOf()
    override var putItems: List<T> = mutableListOf()

    override fun deleteKey(partitionKey: PK, sortKey: SK) {
        deleteKeys += partitionKey to sortKey
    }

    override fun deleteKeys(keys: Iterable<Pair<PK, SK>>) {
        deleteKeys += keys
    }

    override fun putItem(item: T) {
        putItems += item
    }

    override fun putItems(items: Iterable<T>) {
        putItems += items
    }

    private fun toTable() = BatchWriteItemRequestTable(putItems, table.tableName, deleteKeys, table.schema)
    internal fun toTables(): List<BatchWriteItemRequestTable<*>> = existingTables.orEmpty() + toTable()
}
