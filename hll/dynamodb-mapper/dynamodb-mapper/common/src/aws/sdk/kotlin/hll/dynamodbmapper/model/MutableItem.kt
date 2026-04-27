/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model

import aws.sdk.kotlin.hll.dynamodbmapper.model.internal.MutableItemImpl
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * A mutable representation of a low-level item in a DynamoDB table. Items consist of attributes, each of which have a
 * string name and a value.
 */
@ExperimentalApi
public interface MutableItem : MutableMap<String, AttributeValue>

/**
 * Convert this [MutableItem] to an immutable [Item]. Changes to this instance do not affect the returned instance.
 */
@ExperimentalApi
public fun MutableItem.toItem(): Item = toMap().toItem()

/**
 * Converts this map to a [MutableItem]
 */
@ExperimentalApi
public fun MutableMap<String, AttributeValue>.toMutableItem(): MutableItem = MutableItemImpl(this)

/**
 * Returns a new immutable [Item] with the specified attributes, given as name-value pairs
 * @param pairs A collection of [Pair]<[String], [AttributeValue]> where the first value is the attribute name and the
 * second is the attribute value.
 */
@ExperimentalApi
public fun mutableItemOf(vararg pairs: Pair<String, AttributeValue>): MutableItem = MutableItemImpl(mutableMapOf(*pairs))
