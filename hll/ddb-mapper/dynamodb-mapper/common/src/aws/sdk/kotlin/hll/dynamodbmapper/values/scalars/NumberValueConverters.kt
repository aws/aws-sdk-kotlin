/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.plus
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Namespace for containing various conversion utilities dealing with number conversion
 */
public object NumberValueConverters {
    /**
     * Converts between [Number] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number).
     * When converting attribute values into number values, the following concrete subclasses of [Number] will be returned:
     * * [kotlin.Double] — If the number contains any fractional component
     * * [kotlin.Int] — If the number is in the range of [kotlin.Int.MIN_VALUE] and [kotlin.Int.MAX_VALUE] (inclusive)
     * * [kotlin.Long] — Anything else
     */
    public val Auto: ValueConverter<Number> = NumberToStringConverters.Auto + NumericalStringValueConverter

    /**
     * Converts between [kotlin.Byte] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public val Byte: ValueConverter<Byte> = NumberToStringConverters.Byte + NumericalStringValueConverter

    /**
     * Converts between [kotlin.Double] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public val Double: ValueConverter<Double> = NumberToStringConverters.Double + NumericalStringValueConverter

    /**
     * Converts between [kotlin.Float] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public val Float: ValueConverter<Float> = NumberToStringConverters.Float + NumericalStringValueConverter

    /**
     * Converts between [kotlin.Int] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public val Int: ValueConverter<Int> = NumberToStringConverters.Int + NumericalStringValueConverter

    /**
     * Converts between [kotlin.Long] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public val Long: ValueConverter<Long> = NumberToStringConverters.Long + NumericalStringValueConverter

    /**
     * Converts between [kotlin.Short] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public val Short: ValueConverter<Short> = NumberToStringConverters.Short + NumericalStringValueConverter

    /**
     * Converts between [kotlin.UByte] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public val UByte: ValueConverter<UByte> = NumberToStringConverters.UByte + NumericalStringValueConverter

    /**
     * Converts between [kotlin.UInt] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public val UInt: ValueConverter<UInt> = NumberToStringConverters.UInt + NumericalStringValueConverter

    /**
     * Converts between [kotlin.ULong] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public val ULong: ValueConverter<ULong> = NumberToStringConverters.ULong + NumericalStringValueConverter

    /**
     * Converts between [kotlin.UShort] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public val UShort: ValueConverter<UShort> = NumberToStringConverters.UShort + NumericalStringValueConverter
}

/**
 * Converts between [String] instances which contains numbers and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
public val NumericalStringValueConverter: ValueConverter<String> = Converter(AttributeValue::N, AttributeValue::asN)
