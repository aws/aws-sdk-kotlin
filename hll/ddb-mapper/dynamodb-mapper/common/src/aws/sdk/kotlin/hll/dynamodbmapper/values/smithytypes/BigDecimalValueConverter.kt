/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.NumberSetValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumericalStringValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterChain
import aws.sdk.kotlin.hll.mapping.core.converters.collections.SetMappingConverter
import aws.smithy.kotlin.runtime.content.BigDecimal

/**
 * Converts between [BigDecimal] and [String] values
 */
public object BigDecimalToStringConverter : Converter<BigDecimal, String> {
    override fun convertRight(from: BigDecimal): String = from.toPlainString()
    override fun convertLeft(from: String): BigDecimal = BigDecimal(from)
}

/**
 * Converts between [BigDecimal] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number).
 * Because `N` values are stored as decimal strings, this converter preserves the full precision and range that DynamoDB
 * supports, unlike the [kotlin.Double] and [kotlin.Float] converters.
 */
public object BigDecimalValueConverter :
    ValueConverter<BigDecimal> by ConverterChain(BigDecimalToStringConverter, NumericalStringValueConverter)

/**
 * Converts between a [Set] of [BigDecimal] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public object BigDecimalSetValueConverter :
    ValueConverter<Set<BigDecimal>> by ConverterChain(SetMappingConverter(BigDecimalToStringConverter), NumberSetValueConverters.String)
