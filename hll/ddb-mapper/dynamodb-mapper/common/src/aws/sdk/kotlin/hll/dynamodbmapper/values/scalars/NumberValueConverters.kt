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
    public class Auto : ValueConverter<Number> by Auto {
        public companion object : ConverterChain<Number, String, AttributeValue>(NumberToStringConverters.Auto, NumericalStringValueConverter)
    }

    /**
     * Converts between [kotlin.Byte] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public class Byte : ValueConverter<kotlin.Byte> by Byte {
        public companion object : ConverterChain<kotlin.Byte, String, AttributeValue>(NumberToStringConverters.Byte, NumericalStringValueConverter)
    }

    /**
     * Converts between [kotlin.Double] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public class Double : ValueConverter<kotlin.Double> by Double {
        public companion object : ConverterChain<kotlin.Double, String, AttributeValue>(NumberToStringConverters.Double, NumericalStringValueConverter)
    }

    /**
     * Converts between [kotlin.Float] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public class Float : ValueConverter<kotlin.Float> by Float {
        public companion object : ConverterChain<kotlin.Float, String, AttributeValue>(NumberToStringConverters.Float, NumericalStringValueConverter)
    }

    /**
     * Converts between [kotlin.Int] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public class Int : ValueConverter<kotlin.Int> by Int {
        public companion object : ConverterChain<kotlin.Int, String, AttributeValue>(NumberToStringConverters.Int, NumericalStringValueConverter)
    }

    /**
     * Converts between [kotlin.Long] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public class Long : ValueConverter<kotlin.Long> by Long {
        public companion object : ConverterChain<kotlin.Long, String, AttributeValue>(NumberToStringConverters.Long, NumericalStringValueConverter)
    }

    /**
     * Converts between [kotlin.Short] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public class Short : ValueConverter<kotlin.Short> by Short {
        public companion object : ConverterChain<kotlin.Short, String, AttributeValue>(NumberToStringConverters.Short, NumericalStringValueConverter)
    }

    /**
     * Converts between [kotlin.UByte] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public class UByte : ValueConverter<kotlin.UByte> by UByte {
        public companion object : ConverterChain<kotlin.UByte, String, AttributeValue>(NumberToStringConverters.UByte, NumericalStringValueConverter)
    }

    /**
     * Converts between [kotlin.UInt] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public class UInt : ValueConverter<kotlin.UInt> by UInt {
        public companion object : ConverterChain<kotlin.UInt, String, AttributeValue>(NumberToStringConverters.UInt, NumericalStringValueConverter)
    }

    /**
     * Converts between [kotlin.ULong] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public class ULong : ValueConverter<kotlin.ULong> by ULong {
        public companion object : ConverterChain<kotlin.ULong, String, AttributeValue>(NumberToStringConverters.ULong, NumericalStringValueConverter)
    }

    /**
     * Converts between [kotlin.UShort] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public class UShort : ValueConverter<kotlin.UShort> by UShort {
        public companion object : ConverterChain<kotlin.UShort, String, AttributeValue>(NumberToStringConverters.UShort, NumericalStringValueConverter)
    }
}

/**
 * Converts between [String] instances which contains numbers and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
public class NumericalStringValueConverter : ValueConverter<String> by NumericalStringValueConverter {
    public companion object : ValueConverter<String> {
        override fun convertLeft(from: AttributeValue): String = from.asN()
        override fun convertRight(from: String): AttributeValue = AttributeValue.N(from)
    }
}
