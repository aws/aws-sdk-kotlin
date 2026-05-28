/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between [Item] and [AttributeValue]. This converter is typically chained following an [ItemConverter] using
 * the `+` operator.
 */
public class ItemValueConverter : ValueConverter<Item> by ItemValueConverter {
    public companion object : ValueConverter<Item> {
        override fun convertLeft(from: AttributeValue): Item = from.asM().toItem()
        override fun convertRight(from: Item): AttributeValue = AttributeValue.M(from)
    }
}
