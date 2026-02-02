/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.collections.mapFrom
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Converts between [List] and
 * [DynamoDB `L` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Document.List).
 * Note that the lists must contain already-converted [AttributeValue] elements. This converter is typically chained
 * with another converter which handles converting elements to [AttributeValue] either by using the factory function
 * [ListConverter] or using the [mapFrom] extension method.
 *
 * For example:
 *
 * ```kotlin
 * val intListConv = ListConverter(IntConverter) // ValueConverter<List<Int>>
 * val intListConv2 = ListConverter.mapFrom(IntConverter) // same as above
 * ```
 */
@ExperimentalApi
public val ListConverter: ValueConverter<List<AttributeValue>> = Converter(AttributeValue::L, AttributeValue::asL)

/**
 * Creates a new list converter using the given [elementConverter] as a delegate
 * @param F The type of elements in the list
 * @param elementConverter A converter for transforming between values of [F] and [AttributeValue]
 */
@ExperimentalApi
@Suppress("ktlint:standard:function-naming")
public fun <F> ListConverter(elementConverter: Converter<F, AttributeValue>): ValueConverter<List<F>> = ListConverter.mapFrom(elementConverter)
