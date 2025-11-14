/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.TextConverters
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.collections.SetMappingConverter
import aws.sdk.kotlin.hll.mapping.core.converters.collections.SetToListConverter
import aws.sdk.kotlin.hll.mapping.core.converters.plus
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Converts between a [Set] of [ByteArray] elements and
 * [DynamoDB `BS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
@ExperimentalApi
public val ByteArraySetConverter: ValueConverter<Set<ByteArray>> = Converter(
    right = { from -> AttributeValue.Bs(from.toList()) },
    left = { to -> to.asBs().toSet() },
)

/**
 * Converts between a [List] of [String] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
@ExperimentalApi
public val StringListValueConverter: ValueConverter<List<String>> =
    Converter(AttributeValue::Ss, AttributeValue::asSs)

/**
 * Converts between a [Set] of [String] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
@ExperimentalApi
public val StringSetValueConverter: ValueConverter<Set<String>> =
    SetToListConverter<String>() + StringListValueConverter

/**
 * Converts between a [Set] of [CharArray] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
@ExperimentalApi
public val CharArraySetValueConverter: ValueConverter<Set<CharArray>> =
    SetMappingConverter(TextConverters.CharArray) + StringSetValueConverter

/**
 * Converts between a [Set] of [Char] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
@ExperimentalApi
public val CharSetValueConverter: ValueConverter<Set<Char>> =
    SetMappingConverter(TextConverters.Char) + StringSetValueConverter
