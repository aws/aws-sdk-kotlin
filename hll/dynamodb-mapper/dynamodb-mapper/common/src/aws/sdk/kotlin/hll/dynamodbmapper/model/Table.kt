/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TableOperations

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
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     */
    public interface PartitionKey<T, PK> :
        Table<T>,
        ItemSource.PartitionKey<T, PK> {
        // TODO reimplement operations to use pipeline, extension functions where appropriate, docs, etc.
        public suspend fun getItem(partitionKey: PK): T?
    }

    /**
     * Represents a table whose primary key is a composite of a partition key and a sort key
     * @param T The type of objects which will be read from and/or written to this table
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     * @param SK The type of the sort key property, either [String], [Number], or [ByteArray]
     */
    public interface CompositeKey<T, PK, SK> :
        Table<T>,
        ItemSource.CompositeKey<T, PK, SK> {
        // TODO reimplement operations to use pipeline, extension functions where appropriate, docs, etc.
        public suspend fun getItem(partitionKey: PK, sortKey: SK): T?
    }

    /**
     * Get an [Index] reference for performing secondary index operations
     * @param T The type of objects which will be read from to this index
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     * @param name The name of the index
     * @param schema The [ItemSchema] which describes the index, its keys, and how items are converted
     */
    public fun <T, PK> getIndex(
        name: String,
        schema: ItemSchema.PartitionKey<T, PK>,
    ): Index.PartitionKey<T, PK>

    /**
     * Get an [Index] reference for performing secondary index operations
     * @param T The type of objects which will be read from this index
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     * @param SK The type of the sort key property, either [String], [Number], or [ByteArray]
     * @param name The name of the index
     * @param schema The [ItemSchema] which describes the index, its keys, and how items are converted
     */
    public fun <T, PK, SK> getIndex(
        name: String,
        schema: ItemSchema.CompositeKey<T, PK, SK>,
    ): Index.CompositeKey<T, PK, SK>
}
