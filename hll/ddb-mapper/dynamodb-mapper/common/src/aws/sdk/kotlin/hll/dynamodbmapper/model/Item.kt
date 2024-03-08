/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model

import aws.sdk.kotlin.hll.dynamodbmapper.model.internal.ItemImpl
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * An immutable representation of a low-level item in a DynamoDB table. Items consist of attributes, each of which have
 * a string name and a value.
 */
public interface Item : Map<String, AttributeValue>

/**
 * Builds a new immutable [Item] by populating a [MutableItem] using the provided [block] and returning a read-only copy
 * of it.
 * @param block The block to apply to the [MutableItem] builder
 */
public inline fun buildItem(block: MutableItem.() -> Unit): Item =
    mutableMapOf<String, AttributeValue>().toMutableItem().apply(block).toItem()

/**
 * Convert this [Item] into a [MutableItem]. Changes to the returned instance do not affect this instance.
 */
public fun Item.toMutableItem(): MutableItem = toMutableMap().toMutableItem()

/**
 * Converts this map to an immutable [Item]
 */
public fun Map<String, AttributeValue>.toItem(): Item = ItemImpl(this)

/**
 * Returns a new immutable [Item] with the specified attributes, given as name-value pairs
 * @param pairs A collection of [Pair]<[String], [AttributeValue]> where the first value is the attribute name and the
 * second is the attribute value.
 */
public fun itemOf(vararg pairs: Pair<String, AttributeValue>): Item = ItemImpl(mapOf(*pairs))
