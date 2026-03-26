/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.model.Table
import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem
import aws.sdk.kotlin.hll.dynamodbmapper.operations.internal.TransactGetItemsResponseTableCompositeKeyImpl
import aws.sdk.kotlin.hll.dynamodbmapper.operations.internal.TransactGetItemsResponseTablePartitionKeyImpl
import aws.sdk.kotlin.services.dynamodb.model.ItemResponse

public sealed interface TransactGetItemsResponseTable<T> {
    public val items: List<T?>
    public val tableName: String

    public interface PartitionKey<T, PK : KeyType> : TransactGetItemsResponseTable<T> {
        public val schema: ItemSchema.PartitionKey<T, PK>
    }

    public interface CompositeKey<T, PK : KeyType, SK : KeyType> : TransactGetItemsResponseTable<T> {
        public val schema: ItemSchema.CompositeKey<T, PK, SK>
    }
}

@Suppress("ktlint:standard:function-naming")
public fun <T, PK : KeyType> TransactGetItemsResponseTable(
    items: List<T?>,
    tableName: String,
    schema: ItemSchema.PartitionKey<T, PK>,
): TransactGetItemsResponseTable.PartitionKey<T, PK> = TransactGetItemsResponseTablePartitionKeyImpl(
    items,
    tableName,
    schema,
)

@Suppress("ktlint:standard:function-naming")
public fun <T, PK : KeyType, SK : KeyType> TransactGetItemsResponseTable(
    items: List<T?>,
    tableName: String,
    schema: ItemSchema.CompositeKey<T, PK, SK>,
): TransactGetItemsResponseTable.CompositeKey<T, PK, SK> = TransactGetItemsResponseTableCompositeKeyImpl(
    items,
    tableName,
    schema,
)

@Suppress("ktlint:standard:function-naming")
internal fun TransactGetItemsResponseTables(
    responses: List<ItemResponse>?,
    requestTables: List<TransactGetItemsRequestTable<*>>,
): List<TransactGetItemsResponseTable<*>> {
    val responseIterator = responses.orEmpty().iterator()

    return requestTables
        .map { requestTable ->
            fun <T, PK : KeyType> responseTable(
                requestTable: TransactGetItemsRequestTable.PartitionKey<T, PK>,
            ): TransactGetItemsResponseTable.PartitionKey<T, PK> {
                val items = responseIterator
                    .take(requestTable.keys.size)
                    .map { response ->
                        response.item?.let { item ->
                            requestTable.schema.converter.convertLeft(item.toItem())
                        }
                    }
                return TransactGetItemsResponseTable(items, requestTable.tableName, requestTable.schema)
            }

            fun <T, PK : KeyType, SK : KeyType> responseTable(
                requestTable: TransactGetItemsRequestTable.CompositeKey<T, PK, SK>,
            ): TransactGetItemsResponseTable.CompositeKey<T, PK, SK> {
                val items = responseIterator
                    .take(requestTable.keys.size)
                    .map { response ->
                        response.item?.let { item ->
                            requestTable.schema.converter.convertLeft(item.toItem())
                        }
                    }
                return TransactGetItemsResponseTable(items, requestTable.tableName, requestTable.schema)
            }

            when (requestTable) {
                is TransactGetItemsRequestTable.PartitionKey<*, *> -> responseTable(requestTable)
                is TransactGetItemsRequestTable.CompositeKey<*, *, *> -> responseTable(requestTable)
            }
        }
}

private fun <T> Iterator<T>.take(count: Int): List<T> = List(count) { index ->
    check(hasNext()) { "Cannot take $count elements from iterator: out of bounds at index $index" }
    next()
}

public fun <T, PK : KeyType> TransactGetItemsResponse.table(
    table: Table.PartitionKey<T, PK>,
): TransactGetItemsResponseTable.PartitionKey<T, PK> = tables
    .filterIsInstance<TransactGetItemsResponseTable.PartitionKey<T, PK>>()
    .find { it.tableName == table.tableName && it.schema == table.schema }
    ?: error("""Table name "${table.tableName}" not found in results""")

public fun <T, PK : KeyType, SK : KeyType> TransactGetItemsResponse.table(
    table: Table.CompositeKey<T, PK, SK>,
): TransactGetItemsResponseTable.CompositeKey<T, PK, SK> = tables
    .filterIsInstance<TransactGetItemsResponseTable.CompositeKey<T, PK, SK>>()
    .find { it.tableName == table.tableName && it.schema == table.schema }
    ?: error("""Table name "${table.tableName}" not found in results""")
