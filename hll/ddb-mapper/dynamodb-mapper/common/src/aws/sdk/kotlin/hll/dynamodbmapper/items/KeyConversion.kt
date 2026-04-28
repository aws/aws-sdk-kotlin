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
@JvmName("keysToItemPkNonNull")
internal fun <PK : KeyType> keysToItem(
    schema: ItemSchema.PartitionKey<*, PK>,
    pk: PK,
): Item = schema.partitionKey.converter.convertRight(pk)

/**
 * Converts the given partition key to an [Item] with the given schema
 * @param schema A partition key schema
 * @param pk The partition key value to convert
 */
@JvmName("keysToItemPkNullable")
internal fun <PK : KeyType> keysToItem(
    schema: ItemSchema.PartitionKey<*, PK>,
    pk: PK?,
): Item? = pk?.let { keysToItem(schema, it) }

/**
 * Converts the given composite key to an [Item] with the given schema
 * @param schema The composite key schema
 * @param pk The partition key value to convert
 * @param sk The sort key value to convert
 */
@JvmName("keysToItemCkNonNull")
internal fun <PK : KeyType, SK : KeyType> keysToItem(
    schema: ItemSchema.CompositeKey<*, PK, SK>,
    pk: PK,
    sk: SK,
): Item {
    val pkItem = schema.partitionKey.converter.convertRight(pk)
    val skItem = schema.sortKey.converter.convertRight(sk)
    return (pkItem + skItem).toItem()
}

/**
 * Converts the given composite key to an [Item] with the given schema
 * @param schema The composite key schema
 * @param pk The partition key value to convert
 * @param sk The sort key value to convert
 */
@JvmName("keysToItemCkNullable")
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
@JvmName("itemToPkNonNull")
internal fun <PK : KeyType> itemToPk(
    schema: ItemSchema.PartitionKey<*, PK>,
    item: Item,
): PK = schema.partitionKey.converter.convertLeft(item)

/**
 * Converts the given [Item] into a partition key with the given schema
 * @param schema The partition key schema
 * @param item The item containing the key value(s)
 */
@JvmName("itemToPkNullable")
internal fun <PK : KeyType> itemToPk(
    schema: ItemSchema.PartitionKey<*, PK>,
    item: Item?,
): PK? = item?.let { itemToPk(schema, item) }

/**
 * Converts the given [Item] into a partition key with the given schema
 * @param schema The composite key schema
 * @param item The item containing the key value(s)
 */
@JvmName("itemToPkNonNull")
internal fun <PK : KeyType> itemToPk(
    schema: ItemSchema.CompositeKey<*, PK, *>,
    item: Item,
): PK = schema.partitionKey.converter.convertLeft(item)

/**
 * Converts the given [Item] into a partition key with the given schema
 * @param schema The composite key schema
 * @param item The item containing the key value(s)
 */
@JvmName("itemToPkNullable")
internal fun <PK : KeyType> itemToPk(
    schema: ItemSchema.CompositeKey<*, PK, *>,
    item: Item?,
): PK? = item?.let { itemToPk(schema, item) }

/**
 * Converts the given [Item] into a sort key with the given schema
 * @param schema The composite key schema
 * @param item The item containing the key value(s)
 */
@JvmName("itemToSkNonNull")
internal fun <SK : KeyType> itemToSk(
    schema: ItemSchema.CompositeKey<*, *, SK>,
    item: Item,
): SK = schema.sortKey.converter.convertLeft(item)

/**
 * Converts the given [Item] into a sort key with the given schema
 * @param schema The composite key schema
 * @param item The item containing the key value(s)
 */
@JvmName("itemToSkNullable")
internal fun <SK : KeyType> itemToSk(
    schema: ItemSchema.CompositeKey<*, *, SK>,
    item: Item?,
): SK? = item?.let(schema.sortKey.converter::convertLeft)

/**
 * Converts the given [Item] into a partition key and sort key with the given schema
 * @param schema The composite key schema
 * @param item The item containing the key value(s)
 */
@JvmName("itemToCkNonNull")
internal fun <PK : KeyType, SK : KeyType> itemToCk(
    schema: ItemSchema.CompositeKey<*, PK, SK>,
    item: Item,
): Pair<PK, SK> = itemToPk(schema, item) to itemToSk(schema, item)

/**
 * Converts the given [Item] into a partition key and sort key with the given schema
 * @param schema The composite key schema
 * @param item The item containing the key value(s)
 */
@JvmName("itemToCkNullable")
internal fun <PK : KeyType, SK : KeyType> itemToCk(
    schema: ItemSchema.CompositeKey<*, PK, SK>,
    item: Item?,
): Pair<PK, SK>? = item?.let { itemToPk(schema, item) to itemToSk(schema, item) }

/**
 * Extracts the partition key an entity of type [T] using the given schema
 * @param schema The partition key schema
 * @param entity The entity from which to extract the key
 */
internal fun <T, PK : KeyType> entityToPk(schema: ItemSchema.PartitionKey<T, PK>, entity: T): PK = // TODO make this more efficient
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
