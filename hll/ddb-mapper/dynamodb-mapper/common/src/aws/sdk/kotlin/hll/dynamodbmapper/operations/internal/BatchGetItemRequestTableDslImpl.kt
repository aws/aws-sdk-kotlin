/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.model.Table
import aws.sdk.kotlin.hll.dynamodbmapper.operations.BatchGetItemRequestTable
import aws.sdk.kotlin.hll.dynamodbmapper.operations.BatchGetItemRequestTableDsl

internal class BatchGetItemRequestTableDslPartitionKeyImpl<T, PK : KeyType>(
    val existingTables: List<BatchGetItemRequestTable<*>>?,
    val table: Table.PartitionKey<T, PK>,
) : BatchGetItemRequestTableDsl.PartitionKey<T, PK> {
    override var consistentRead: Boolean? = null
    override var keys: List<PK> = mutableListOf()

    override fun key(key: PK) {
        keys += key
    }

    override fun keys(keys: Iterable<PK>) {
        this.keys += keys
    }

    private fun toTable() = BatchGetItemRequestTable(consistentRead, keys, table.schema, table.tableName)
    internal fun toTables(): List<BatchGetItemRequestTable<*>> = existingTables.orEmpty() + toTable()
}

internal class BatchGetItemRequestTableDslCompositeKeyImpl<T, PK : KeyType, SK : KeyType>(
    val existingTables: List<BatchGetItemRequestTable<*>>?,
    val table: Table.CompositeKey<T, PK, SK>,
) : BatchGetItemRequestTableDsl.CompositeKey<T, PK, SK> {
    override var consistentRead: Boolean? = null
    override var keys: List<Pair<PK, SK>> = mutableListOf()

    override fun key(partitionKey: PK, sortKey: SK) {
        keys += partitionKey to sortKey
    }

    override fun keys(keys: Iterable<Pair<PK, SK>>) {
        this.keys += keys
    }

    private fun toTable() = BatchGetItemRequestTable(consistentRead, keys, table.schema, table.tableName)
    internal fun toTables(): List<BatchGetItemRequestTable<*>> = existingTables.orEmpty() + toTable()
}
