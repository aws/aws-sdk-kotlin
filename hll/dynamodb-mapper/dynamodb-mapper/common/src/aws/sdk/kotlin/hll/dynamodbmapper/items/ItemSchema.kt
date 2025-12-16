/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.ItemSchemaCompositeKeyImpl
import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.ItemSchemaPartitionKeyImpl
import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.attrs

/**
 * Defines a schema for handling objects of type [T], including an [ItemConverter] for converting between objects and
 * items and a [KeySpec] for identifying primary keys.
 * @param T The type of objects described by this schema
 */
public sealed interface ItemSchema<T> {
    /**
     * The [ItemConverter] used to convert between objects and items
     */
    public val converter: ItemConverter<T>

    /**
     * The names of the attributes which form the primary key of this table
     */
    public val keyAttributeNames: List<String>

    /**
     * Represents a schema with a primary key consisting of a single partition key
     * @param T The type of objects described by this schema
     * @param PK The type of the partition key property, either [KeyType] or one of its specific derivations
     */
    public interface PartitionKey<T, PK : KeyType> : ItemSchema<T> {
        /**
         * The [KeySpec] for the partition key
         */
        public val partitionKey: KeySpec<PK>

        override val keyAttributeNames: List<String>
            get() = partitionKey.attrs.map { it.name }
    }

    /**
     * Represents a schema with a primary key that is a composite of a partition key and a sort key
     * @param T The type of objects described by this schema
     * @param PK The type of the partition key property, either [KeyType] or one of its specific derivations
     * @param SK The type of the sort key property, either [KeyType] or one of its specific derivations
     */
    public interface CompositeKey<T, PK : KeyType, SK : KeyType> : ItemSchema<T> {
        /**
         * The [KeySpec] for the partition key
         */
        public val partitionKey: KeySpec<PK>

        /**
         * The [KeySpec] for the sort key
         */
        public val sortKey: KeySpec<SK>

        override val keyAttributeNames: List<String>
            get() = partitionKey.attrs.map { it.name } + sortKey.attrs.map { it.name }
    }
}

/**
 * Create a new item schema with a primary key consisting of a single partition key.
 * @param T The type of objects described by this schema
 * @param PK The type of the partition key property, either [KeyType] or one of its specific derivations
 * @param converter The [ItemConverter] used to convert between objects and items
 * @param partitionKey The [KeySpec] for the partition key
 */
@Suppress("FunctionName")
public fun <T, PK : KeyType> ItemSchema(converter: ItemConverter<T>, partitionKey: KeySpec<PK>): ItemSchema.PartitionKey<T, PK> =
    ItemSchemaPartitionKeyImpl(converter, partitionKey)

/**
 * Create a new item schema with a primary key consisting of a single partition key.
 * @param T The type of objects described by this schema
 * @param PK The type of the partition key property, either [KeyType] or one of its specific derivations
 * @param SK The type of the sort key property, either [KeyType] or one of its specific derivations
 * @param converter The [ItemConverter] used to convert between objects and items
 * @param partitionKey The [KeySpec] for the partition key
 * @param sortKey The [KeySpec] for the sort key
 */
@Suppress("FunctionName")
public fun <T, PK : KeyType, SK : KeyType> ItemSchema(
    converter: ItemConverter<T>,
    partitionKey: KeySpec<PK>,
    sortKey: KeySpec<SK>,
): ItemSchema.CompositeKey<T, PK, SK> = ItemSchemaCompositeKeyImpl(converter, partitionKey, sortKey)

/**
 * Associate this [ItemConverter] with a [KeySpec] for a partition key to form a complete [ItemSchema]
 * @param T The type of objects described by this schema
 * @param PK The type of the partition key property, either [KeyType] or one of its specific derivations
 * @param partitionKey The [KeySpec] that describes the partition key
 */
public fun <T, PK : KeyType> ItemConverter<T>.withKeySpec(partitionKey: KeySpec<PK>): ItemSchema.PartitionKey<T, PK> =
    ItemSchema(this, partitionKey)

/**
 * Associate this [ItemConverter] with [KeySpec] instances for a composite key to form a complete [ItemSchema]
 * @param T The type of objects described by this schema
 * @param PK The type of the partition key property, either [KeyType] or one of its specific derivations
 * @param SK The type of the sort key property, either [KeyType] or one of its specific derivations
 * @param partitionKey The [KeySpec] that describes the partition key
 * @param sortKey The [KeySpec] that describes the sort key
 */
public fun <T, PK : KeyType, SK : KeyType> ItemConverter<T>.withKeySpec(
    partitionKey: KeySpec<PK>,
    sortKey: KeySpec<SK>,
): ItemSchema.CompositeKey<T, PK, SK> = ItemSchema(this, partitionKey, sortKey)
