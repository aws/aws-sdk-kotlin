/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.items.entityToCk
import aws.sdk.kotlin.hll.dynamodbmapper.items.entityToPk
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TableOperations
import aws.sdk.kotlin.hll.dynamodbmapper.operations.deleteItem
import aws.sdk.kotlin.hll.dynamodbmapper.operations.getItem
import aws.sdk.kotlin.hll.dynamodbmapper.operations.putItem

/**
 * Represents a table in DynamoDB and an associated item schema. Operations on this table will invoke low-level
 * operations after mapping objects to items and vice versa.
 * @param T The type of objects which will be read from and/or written to this table
 */
public interface Table<T> :
    TableSpec<T>,
    TableOperations<T>,
    ItemSource<T> {

    /**
     * Represents a table whose primary key is a single partition key
     * @param T The type of objects which will be read from and/or written to this table
     * @param PK The type of the partition key property, either [KeyType] or one of its specific derivations
     */
    public interface PartitionKey<T, PK : KeyType> :
        Table<T>,
        TableSpec.PartitionKey<T, PK>,
        TableOperations.PartitionKey<T, PK>,
        ItemSource.PartitionKey<T, PK>

    /**
     * Represents a table whose primary key is a composite of a partition key and a sort key
     * @param T The type of objects which will be read from and/or written to this table
     * @param PK The type of the partition key property, either [KeyType] or one of its specific derivations
     * @param SK The type of the sort key property, either [KeyType] or one of its specific derivations
     */
    public interface CompositeKey<T, PK : KeyType, SK : KeyType> :
        Table<T>,
        TableSpec.CompositeKey<T, PK, SK>,
        TableOperations.CompositeKey<T, PK, SK>,
        ItemSource.CompositeKey<T, PK, SK>

    /**
     * Get an [Index] reference for performing secondary index operations
     * @param T The type of objects which will be read from to this index
     * @param PK The type of the partition key property, either [KeyType] or one of its specific derivations
     * @param name The name of the index
     * @param schema The [ItemSchema] which describes the index, its keys, and how items are converted
     */
    public fun <T, PK : KeyType> getIndex(
        name: String,
        schema: ItemSchema.PartitionKey<T, PK>,
    ): Index.PartitionKey<T, PK>

    /**
     * Get an [Index] reference for performing secondary index operations
     * @param T The type of objects which will be read from this index
     * @param PK The type of the partition key property, either [KeyType] or one of its specific derivations
     * @param SK The type of the sort key property, either [KeyType] or one of its specific derivations
     * @param name The name of the index
     * @param schema The [ItemSchema] which describes the index, its keys, and how items are converted
     */
    public fun <T, PK : KeyType, SK : KeyType> getIndex(
        name: String,
        schema: ItemSchema.CompositeKey<T, PK, SK>,
    ): Index.CompositeKey<T, PK, SK>
}

public suspend fun <T, PK : KeyType> Table.PartitionKey<T, PK>.deleteItem(item: T) {
    this.deleteItem { partitionKey = entityToPk(schema, item) }
}

public suspend fun <T, PK : KeyType> Table.PartitionKey<T, PK>.deleteItem(partitionKey: PK) {
    this.deleteItem { this.partitionKey = partitionKey }
}

public suspend fun <T, PK : KeyType, SK : KeyType> Table.CompositeKey<T, PK, SK>.deleteItem(item: T) {
    val (pk, sk) = entityToCk(schema, item)
    this.deleteItem {
        partitionKey = pk
        sortKey = sk
    }
}

public suspend fun <T, PK : KeyType, SK : KeyType> Table.CompositeKey<T, PK, SK>.deleteItem(
    partitionKey: PK,
    sortKey: SK,
) {
    this.deleteItem {
        this.partitionKey = partitionKey
        this.sortKey = sortKey
    }
}

public suspend fun <T, PK : KeyType> Table.PartitionKey<T, PK>.getItem(item: T): T? =
    this.getItem { partitionKey = entityToPk(schema, item) }.item

public suspend fun <T, PK : KeyType> Table.PartitionKey<T, PK>.getItem(partitionKey: PK): T? =
    this.getItem { this.partitionKey = partitionKey }.item

public suspend fun <T, PK : KeyType, SK : KeyType> Table.CompositeKey<T, PK, SK>.getItem(item: T): T? {
    val (pk, sk) = entityToCk(schema, item)
    return this.getItem {
        partitionKey = pk
        sortKey = sk
    }.item
}

public suspend fun <T, PK : KeyType, SK : KeyType> Table.CompositeKey<T, PK, SK>.getItem(
    partitionKey: PK,
    sortKey: SK,
): T? =
    this.getItem {
        this.partitionKey = partitionKey
        this.sortKey = sortKey
    }.item

public suspend fun <T> Table<T>.putItem(item: T) {
    this.putItem { this.item = item }
}
