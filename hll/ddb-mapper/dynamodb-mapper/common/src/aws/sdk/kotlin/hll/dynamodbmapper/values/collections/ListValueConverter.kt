/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterChain
import aws.sdk.kotlin.hll.mapping.core.converters.collections.ListMappingConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between [List] and
 * [DynamoDB `L` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Document.List).
 * Note that the lists must contain already-converted [AttributeValue] elements. This converter is typically chained
 * with another converter which handles mapping elements to [AttributeValue], such as by using the factory function
 * [ListValueConverter].
 */
public object AttributeValueListValueConverter : ValueConverter<List<AttributeValue>> {
    override fun convertLeft(from: AttributeValue): List<AttributeValue> = from.asL()
    override fun convertRight(from: List<AttributeValue>): AttributeValue = AttributeValue.L(from)
}

/**
 * Converts between a [List] of [E] elements and
 * [DynamoDB `L` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Document.List)
 * @param E The type of elements in the list
 * @param delegate A converter for transforming between values of [E] and [AttributeValue]
 */
public class ListValueConverter<E>(
    delegate: ValueConverter<E>,
    attributeValueListValueConverter: ValueConverter<List<AttributeValue>> = AttributeValueListValueConverter,
) : ValueConverter<List<E>> by ConverterChain(ListMappingConverter(delegate), attributeValueListValueConverter)
