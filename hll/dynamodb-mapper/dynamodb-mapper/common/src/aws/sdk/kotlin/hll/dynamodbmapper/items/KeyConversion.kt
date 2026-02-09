/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem

/**
 * Converts the given partition key to an [Item] with the given schema
 * @param schema A partition key schema
 * @param pk The partition key value to convert
 */
internal fun <PK : KeyType> keysToItem(schema: ItemSchema.PartitionKey<*, PK>, pk: PK?): Item? =
    pk?.let { schema.partitionKey.converter.convertRight(it) }?.takeIf { it.isNotEmpty() }

/**
 * Converts the given composite key to an [Item] with the given schema
 * @param schema The composite key schema
 * @param pk The parition key value to convert
 * @param sk The sort key value to convert
 */
internal fun <PK : KeyType, SK : KeyType> keysToItem(
    schema: ItemSchema.CompositeKey<*, PK, SK>,
    pk: PK?,
    sk: SK?,
): Item? {
    val pkItem = pk?.let { schema.partitionKey.converter.convertRight(it) }.orEmpty()
    val skItem = sk?.let { schema.sortKey.converter.convertRight(it) }.orEmpty()
    val fullItem = pkItem + skItem
    return fullItem.takeIf { it.isNotEmpty() }?.toItem()
}

/**
 * Converts the given [Item] into a partition key with the given schema
 * @param schema The partition key schema
 * @param item The item containing the key value(s)
 */
internal fun <PK : KeyType> itemToPk(schema: ItemSchema.PartitionKey<*, PK>, item: Item?): PK? =
    item?.let(schema.partitionKey.converter::convertLeft)

/**
 * Converts the given [Item] into a partition key with the given schema
 * @param schema The composite key schema
 * @param item The item containing the key value(s)
 */
internal fun <PK : KeyType> itemToPk(schema: ItemSchema.CompositeKey<*, PK, *>, item: Item?): PK? =
    item?.let(schema.partitionKey.converter::convertLeft)

/**
 * Converts the given [Item] into a sort key with the given schema
 * @param schema The composite key schema
 * @param item The item containing the key value(s)
 */
internal fun <SK : KeyType> itemToSk(schema: ItemSchema.CompositeKey<*, *, SK>, item: Item?): SK? =
    item?.let(schema.sortKey.converter::convertLeft)

/**
 * Extracts the partition key an entity of type [T] using the given schema
 * @param schema The partition key schema
 * @param entity The entity from which to extract the key
 */
internal fun <T, PK : KeyType> entityToPk(schema: ItemSchema.PartitionKey<T, PK>, entity: T): PK =
    // TODO make this more efficient
    schema.partitionKey.converter.convertLeft(schema.converter.convertRight(entity))

/**
 * Extracts the composite key an entity of type [T] using the given schema
 * @param schema The composite key schema
 * @param entity The entity from which to extract the keys
 */
internal fun <T, PK : KeyType, SK : KeyType> entityToCk(
    schema: ItemSchema.CompositeKey<T, PK, SK>,
    entity: T,
): Pair<PK, SK> {
    // TODO make this more efficient
    val fullItem = schema.converter.convertRight(entity)
    val pk = schema.partitionKey.converter.convertLeft(fullItem)
    val sk = schema.sortKey.converter.convertLeft(fullItem)
    return pk to sk
}
