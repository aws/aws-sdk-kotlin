/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.TextConverters
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterChain
import aws.sdk.kotlin.hll.mapping.core.converters.collections.SetMappingConverter
import aws.sdk.kotlin.hll.mapping.core.converters.collections.SetToListConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between a [Set] of [ByteArray] elements and
 * [DynamoDB `BS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public object ByteArraySetConverter : ValueConverter<Set<ByteArray>> {
    override fun convertLeft(from: AttributeValue): Set<ByteArray> = from.asBs().toSet()
    override fun convertRight(from: Set<ByteArray>): AttributeValue = AttributeValue.Bs(from.toList())
}

/**
 * Converts between a [List] of [String] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public object StringListValueConverter : ValueConverter<List<String>> {
    override fun convertLeft(from: AttributeValue): List<String> = from.asSs()
    override fun convertRight(from: List<String>): AttributeValue = AttributeValue.Ss(from)
}

/**
 * Converts between a [Set] of [String] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public object StringSetValueConverter : ValueConverter<Set<String>> by ConverterChain(SetToListConverter(), StringListValueConverter)

/**
 * Converts between a [Set] of [CharArray] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public object CharArraySetValueConverter : ValueConverter<Set<CharArray>> by ConverterChain(SetMappingConverter(TextConverters.CharArray), StringSetValueConverter)

/**
 * Converts between a [Set] of [Char] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public object CharSetValueConverter : ValueConverter<Set<Char>> by ConverterChain(SetMappingConverter(TextConverters.Char), StringSetValueConverter)
