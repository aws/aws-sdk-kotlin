/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.collections.ListMappingConverter
import aws.sdk.kotlin.hll.mapping.core.converters.plus
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Converts between [List] and
 * [DynamoDB `L` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Document.List).
 * Note that the lists must contain already-converted [AttributeValue] elements. This converter is typically chained
 * with another converter which handles mapping elements to [AttributeValue], such as by using the factory function
 * [ListValueConverter].
 */
@ExperimentalApi
public val AttributeValueListValueConverter: ValueConverter<List<AttributeValue>> =
    Converter(AttributeValue::L, AttributeValue::asL)

/**
 * Creates a new list converter using the given [delegate] as a delegate
 * @param E The type of elements in the list
 * @param delegate A converter for transforming between values of [E] and [AttributeValue]
 */
@ExperimentalApi
@Suppress("ktlint:standard:function-naming")
public fun <E> ListValueConverter(
    delegate: ValueConverter<E>,
    attributeValueListValueConverter: ValueConverter<List<AttributeValue>> = AttributeValueListValueConverter,
): ValueConverter<List<E>> = ListMappingConverter(delegate) + attributeValueListValueConverter
