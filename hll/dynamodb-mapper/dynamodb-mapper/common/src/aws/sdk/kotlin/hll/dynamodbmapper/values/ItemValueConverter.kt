/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Converts between [Item] and [AttributeValue]. This converter is typically chained following an [ItemConverter] using
 * the `+` operator.
 */
@ExperimentalApi
public val ItemValueConverter : ValueConverter<Item> = Converter(AttributeValue::M) { it.asM().toItem() }
