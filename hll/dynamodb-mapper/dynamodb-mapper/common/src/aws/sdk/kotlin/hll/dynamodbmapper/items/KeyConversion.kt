/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem

internal fun <PK : KeyType> keysToItem(schema: ItemSchema.PartitionKey<*, PK>, pk: PK?): Item? =
    pk?.let { schema.partitionKey.converter.convertRight(it) }?.takeIf { it.isNotEmpty() }

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

internal fun <PK : KeyType> itemToPk(schema: ItemSchema.PartitionKey<*, PK>, item: Item?): PK? =
    item?.let(schema.partitionKey.converter::convertLeft)

internal fun <PK : KeyType> itemToPk(schema: ItemSchema.CompositeKey<*, PK, *>, item: Item?): PK? =
    item?.let(schema.partitionKey.converter::convertLeft)

internal fun <SK : KeyType> itemToSk(schema: ItemSchema.CompositeKey<*, *, SK>, item: Item?): SK? =
    item?.let(schema.sortKey.converter::convertLeft)

internal fun <T, PK : KeyType> entityToPk(schema: ItemSchema.PartitionKey<T, PK>, entity: T): PK =
    // FIXME make this more efficient
    schema.partitionKey.converter.convertLeft(schema.converter.convertRight(entity))

internal fun <T, PK : KeyType, SK : KeyType> entityToCk(
    schema: ItemSchema.CompositeKey<T, PK, SK>,
    entity: T,
): Pair<PK, SK> {
    // FIXME make this more efficient
    val fullItem = schema.converter.convertRight(entity)
    val pk = schema.partitionKey.converter.convertLeft(fullItem)
    val sk = schema.sortKey.converter.convertLeft(fullItem)
    return pk to sk
}
