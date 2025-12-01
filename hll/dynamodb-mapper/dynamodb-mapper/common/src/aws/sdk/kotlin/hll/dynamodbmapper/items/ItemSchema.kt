/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.ItemSchemaCompositeKeyImpl
import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.ItemSchemaPartitionKeyImpl

/**
 * Defines a schema for handling objects of a certain type, including an [ItemConverter] for converting between objects
 * items and a [KeySpec] for identifying primary keys.
 * @param T The type of objects described by this schema
 */
public interface ItemSchema<T> {
    /**
     * The [ItemConverter] used to convert between objects and items
     */
    public val converter: ItemConverter<T>

    /**
     * The name(s) of the attributes which form the primary key of this table
     */
    public val keyAttributeNames: Set<String>

    /**
     * Represents a schema with a primary key consisting of a single partition key
     * @param T The type of objects described by this schema
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     */
    public interface PartitionKey<T, in PK> : ItemSchema<T> {
        /**
         * The [KeySpec] for the partition key
         */
        public val partitionKey: KeySpec<PK>

        override val keyAttributeNames: Set<String>
            get() = setOf(partitionKey.name)
    }

    /**
     * Represents a schema with a primary key that is a composite of a partition key and a sort key
     * @param T The type of objects described by this schema
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     * @param SK The type of the sort key property, either [String], [Number], or [ByteArray]
     */
    public interface CompositeKey<T, PK, SK> : PartitionKey<T, PK> {
        /**
         * The [KeySpec] for the sort key
         */
        public val sortKey: KeySpec<SK>

        override val keyAttributeNames: Set<String>
            get() = setOf(partitionKey.name, sortKey.name)
    }
}

/**
 * Create a new item schema with a primary key consisting of a single partition key.
 * @param T The type of objects described by this schema
 * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
 * @param converter The [ItemConverter] used to convert between objects and items
 * @param partitionKey The [KeySpec] for the partition key
 */
@Suppress("FunctionName")
public fun <T, PK> ItemSchema(converter: ItemConverter<T>, partitionKey: KeySpec<PK>): ItemSchema.PartitionKey<T, PK> =
    ItemSchemaPartitionKeyImpl(converter, partitionKey)

/**
 * Create a new item schema with a primary key consisting of a single partition key.
 * @param T The type of objects described by this schema
 * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
 * @param SK The type of the sort key property, either [String], [Number], or [ByteArray]
 * @param converter The [ItemConverter] used to convert between objects and items
 * @param partitionKey The [KeySpec] for the partition key
 * @param sortKey The [KeySpec] for the sort key
 */
@Suppress("FunctionName")
public fun <T, PK, SK> ItemSchema(
    converter: ItemConverter<T>,
    partitionKey: KeySpec<PK>,
    sortKey: KeySpec<SK>,
): ItemSchema.CompositeKey<T, PK, SK> = ItemSchemaCompositeKeyImpl(converter, partitionKey, sortKey)

/**
 * Associate this [ItemConverter] with a [KeySpec] for a partition key to form a complete [ItemSchema]
 * @param T The type of objects described by this schema
 * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
 * @param partitionKey The [KeySpec] that describes the partition key
 */
public fun <T, PK> ItemConverter<T>.withKeySpec(partitionKey: KeySpec<PK>): ItemSchema.PartitionKey<T, PK> =
    ItemSchema(this, partitionKey)

/**
 * Associate this [ItemConverter] with [KeySpec] instances for a composite key to form a complete [ItemSchema]
 * @param T The type of objects described by this schema
 * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
 * @param SK The type of the sort key property, either [String], [Number], or [ByteArray]
 * @param partitionKey The [KeySpec] that describes the partition key
 * @param sortKey The [KeySpec] that describes the sort key
 */
public fun <T, PK, SK> ItemConverter<T>.withKeySpec(
    partitionKey: KeySpec<PK>,
    sortKey: KeySpec<SK>,
): ItemSchema.CompositeKey<T, PK, SK> = ItemSchema(this, partitionKey, sortKey)
