/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model

import aws.sdk.kotlin.hll.dynamodbmapper.items.Key
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType

/**
 * Gets an item from this table with the given partition key value
 * @param T The type of objects which will be read from and/or written to this table
 * @param partitionKey The partition key value of the item
 */
public suspend fun <T> Table.PartitionKey<T, KeyType.Key1<ByteArray>>.getItem(partitionKey: ByteArray): T? = getItem(Key(partitionKey))

/**
 * Gets an item from this table with the given partition key value
 * @param T The type of objects which will be read from and/or written to this table
 * @param N The type of [Number] of the partition key
 * @param partitionKey The partition key value of the item
 */
public suspend fun <T, N : Number> Table.PartitionKey<T, KeyType.Key1<N>>.getItem(partitionKey: N): T? = getItem(Key(partitionKey))

/**
 * Gets an item from this table with the given partition key value
 * @param T The type of objects which will be read from and/or written to this table
 * @param partitionKey The partition key value of the item
 */
public suspend fun <T> Table.PartitionKey<T, KeyType.Key1<String>>.getItem(partitionKey: String): T? = getItem(Key(partitionKey))

/**
 * Gets an item from this table with the given partition key and sort key values
 * @param T The type of objects which will be read from and/or written to this table
 * @param partitionKey The partition key value of the item
 * @param sortKey The sort key value of the item
 */
public suspend fun <T> Table.CompositeKey<T, KeyType.Key1<ByteArray>, KeyType.Key1<ByteArray>>.getItem(
    partitionKey: ByteArray,
    sortKey: ByteArray,
): T? = getItem(Key(partitionKey), Key(sortKey))

/**
 * Gets an item from this table with the given partition key and sort key values
 * @param T The type of objects which will be read from and/or written to this table
 * @param N2 The type of [Number] of the sort key
 * @param partitionKey The partition key value of the item
 * @param sortKey The sort key value of the item
 */
public suspend fun <T, N2 : Number> Table.CompositeKey<T, KeyType.Key1<ByteArray>, KeyType.Key1<N2>>.getItem(
    partitionKey: ByteArray,
    sortKey: N2,
): T? = getItem(Key(partitionKey), Key(sortKey))

/**
 * Gets an item from this table with the given partition key and sort key values
 * @param T The type of objects which will be read from and/or written to this table
 * @param partitionKey The partition key value of the item
 * @param sortKey The sort key value of the item
 */
public suspend fun <T> Table.CompositeKey<T, KeyType.Key1<ByteArray>, KeyType.Key1<String>>.getItem(
    partitionKey: ByteArray,
    sortKey: String,
): T? = getItem(Key(partitionKey), Key(sortKey))

/**
 * Gets an item from this table with the given partition key and sort key values
 * @param T The type of objects which will be read from and/or written to this table
 * @param N1 The type of [Number] of the partition key
 * @param partitionKey The partition key value of the item
 * @param sortKey The sort key value of the item
 */
public suspend fun <T, N1 : Number> Table.CompositeKey<T, KeyType.Key1<N1>, KeyType.Key1<ByteArray>>.getItem(
    partitionKey: N1,
    sortKey: ByteArray,
): T? = getItem(Key(partitionKey), Key(sortKey))

/**
 * Gets an item from this table with the given partition key and sort key values
 * @param T The type of objects which will be read from and/or written to this table
 * @param N1 The type of [Number] of the partition key
 * @param N2 The type of [Number] of the sort key
 * @param partitionKey The partition key value of the item
 * @param sortKey The sort key value of the item
 */
public suspend fun <T, N1 : Number, N2 : Number> Table.CompositeKey<T, KeyType.Key1<N1>, KeyType.Key1<N2>>.getItem(
    partitionKey: N1,
    sortKey: N2,
): T? = getItem(Key(partitionKey), Key(sortKey))

/**
 * Gets an item from this table with the given partition key and sort key values
 * @param T The type of objects which will be read from and/or written to this table
 * @param N1 The type of [Number] of the partition key
 * @param partitionKey The partition key value of the item
 * @param sortKey The sort key value of the item
 */
public suspend fun <T, N1 : Number> Table.CompositeKey<T, KeyType.Key1<N1>, KeyType.Key1<String>>.getItem(
    partitionKey: N1,
    sortKey: String,
): T? = getItem(Key(partitionKey), Key(sortKey))

/**
 * Gets an item from this table with the given partition key and sort key values
 * @param T The type of objects which will be read from and/or written to this table
 * @param partitionKey The partition key value of the item
 * @param sortKey The sort key value of the item
 */
public suspend fun <T> Table.CompositeKey<T, KeyType.Key1<String>, KeyType.Key1<ByteArray>>.getItem(
    partitionKey: String,
    sortKey: ByteArray,
): T? = getItem(Key(partitionKey), Key(sortKey))

/**
 * Gets an item from this table with the given partition key and sort key values
 * @param T The type of objects which will be read from and/or written to this table
 * @param N2 The type of [Number] of the sort key
 * @param partitionKey The partition key value of the item
 * @param sortKey The sort key value of the item
 */
public suspend fun <T, N2 : Number> Table.CompositeKey<T, KeyType.Key1<String>, KeyType.Key1<N2>>.getItem(
    partitionKey: String,
    sortKey: N2,
): T? = getItem(Key(partitionKey), Key(sortKey))

/**
 * Gets an item from this table with the given partition key and sort key values
 * @param T The type of objects which will be read from and/or written to this table
 * @param partitionKey The partition key value of the item
 * @param sortKey The sort key value of the item
 */
public suspend fun <T> Table.CompositeKey<T, KeyType.Key1<String>, KeyType.Key1<String>>.getItem(
    partitionKey: String,
    sortKey: String,
): T? = getItem(Key(partitionKey), Key(sortKey))
