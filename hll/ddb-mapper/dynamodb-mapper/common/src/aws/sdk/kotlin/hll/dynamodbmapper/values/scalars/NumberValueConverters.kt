/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterChain
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
    public object Auto : ValueConverter<Number> by ConverterChain(NumberToStringConverters.Auto, NumericalStringValueConverter)

    /**
     * Converts between [kotlin.Byte] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public object Byte : ValueConverter<kotlin.Byte> by ConverterChain(NumberToStringConverters.Byte, NumericalStringValueConverter)

    /**
     * Converts between [kotlin.Double] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public object Double : ValueConverter<kotlin.Double> by ConverterChain(NumberToStringConverters.Double, NumericalStringValueConverter)

    /**
     * Converts between [kotlin.Float] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public object Float : ValueConverter<kotlin.Float> by ConverterChain(NumberToStringConverters.Float, NumericalStringValueConverter)

    /**
     * Converts between [kotlin.Int] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public object Int : ValueConverter<kotlin.Int> by ConverterChain(NumberToStringConverters.Int, NumericalStringValueConverter)

    /**
     * Converts between [kotlin.Long] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public object Long : ValueConverter<kotlin.Long> by ConverterChain(NumberToStringConverters.Long, NumericalStringValueConverter)

    /**
     * Converts between [kotlin.Short] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public object Short : ValueConverter<kotlin.Short> by ConverterChain(NumberToStringConverters.Short, NumericalStringValueConverter)

    /**
     * Converts between [kotlin.UByte] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public object UByte : ValueConverter<kotlin.UByte> by ConverterChain(NumberToStringConverters.UByte, NumericalStringValueConverter)

    /**
     * Converts between [kotlin.UInt] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public object UInt : ValueConverter<kotlin.UInt> by ConverterChain(NumberToStringConverters.UInt, NumericalStringValueConverter)

    /**
     * Converts between [kotlin.ULong] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public object ULong : ValueConverter<kotlin.ULong> by ConverterChain(NumberToStringConverters.ULong, NumericalStringValueConverter)

    /**
     * Converts between [kotlin.UShort] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public object UShort : ValueConverter<kotlin.UShort> by ConverterChain(NumberToStringConverters.UShort, NumericalStringValueConverter)
}

/**
 * Converts between [String] instances which contains numbers and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
public object NumericalStringValueConverter : ValueConverter<String> {
    override fun convertLeft(from: AttributeValue): String = from.asN()
    override fun convertRight(from: String): AttributeValue = AttributeValue.N(from)
}
