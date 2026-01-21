/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.andThenTo
import aws.sdk.kotlin.hll.mapping.core.converters.validatingFrom
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Namespace for containing various conversion utilities dealing with number conversion
 */
@ExperimentalApi
public object NumberConverters {
    /**
     * Converts between [String] instances which contains numbers and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public val StringToAttributeValueNumberConverter: ValueConverter<String> =
        Converter(AttributeValue::N, AttributeValue::asN)

    /**
     * Creates a [ValueConverter] which converts between number type [N] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     */
    public fun <N> of(numberToStringConverter: Converter<N, String>): ValueConverter<N> = numberToStringConverter.andThenTo(StringToAttributeValueNumberConverter)

    /**
     * Converts between [Number] and [String] values
     */
    public val AutoNumberToStringConverter: Converter<Number, String> = Converter(
        convertTo = Number::toString,
        convertFrom = { to: String ->
            when {
                '.' in to -> to.toDouble()
                else -> when (val longNumber = to.toLong()) {
                    in Int.MIN_VALUE..Int.MAX_VALUE -> longNumber.toInt()
                    else -> longNumber
                }
            }
        },
    )

    /**
     * Converts between [Byte] and [String] values
     */
    public val ByteToStringConverter: Converter<Byte, String> = Converter(Byte::toString, String::toByte)

    /**
     * Converts between [Double] and [String] values. Because
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     * do not support them, this converter throws exceptions for non-finite numbers such as [Double.NEGATIVE_INFINITY],
     * [Double.POSITIVE_INFINITY], and [Double.NaN].
     */
    public val DoubleToStringConverter: Converter<Double, String> =
        Converter(Double::toString, String::toDouble).validatingFrom { from: Double ->
            require(from.isFinite()) { "Cannot convert $from: only finite numbers are supported" }
        }

    /**
     * Converts between [Float] and [String] values. Because
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     * do not support them, this converter throws exceptions for non-finite numbers such as [Float.NEGATIVE_INFINITY],
     * [Float.POSITIVE_INFINITY], and [Float.NaN].
     */
    public val FloatToStringConverter: Converter<Float, String> =
        Converter(Float::toString, String::toFloat).validatingFrom { from: Float ->
            require(from.isFinite()) { "Cannot convert $from: only finite numbers are supported" }
        }

    /**
     * Converts between [Int] and [String] values
     */
    public val IntToStringConverter: Converter<Int, String> = Converter(Int::toString, String::toInt)

    /**
     * Converts between [Long] and [String] values
     */
    public val LongToStringConverter: Converter<Long, String> = Converter(Long::toString, String::toLong)

    /**
     * Converts between [Short] and [String] values
     */
    public val ShortToStringConverter: Converter<Short, String> = Converter(Short::toString, String::toShort)

    /**
     * Converts between [UByte] and [String] values
     */
    public val UByteToStringConverter: Converter<UByte, String> = Converter(UByte::toString, String::toUByte)

    /**
     * Converts between [UInt] and [String] values
     */
    public val UIntToStringConverter: Converter<UInt, String> = Converter(UInt::toString, String::toUInt)

    /**
     * Converts between [ULong] and [String] values
     */
    public val ULongToStringConverter: Converter<ULong, String> = Converter(ULong::toString, String::toULong)

    /**
     * Converts between [UShort] and [String] values
     */
    public val UShortToStringConverter: Converter<UShort, String> = Converter(UShort::toString, String::toUShort)
}

/**
 * Converts between [Number] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number).
 * When converting attribute values into number values, the following concrete subclasses of [Number] will be returned:
 * * [Double] — If the number contains any fractional component
 * * [Int] — If the number is in the range of [Int.MIN_VALUE] and [Int.MAX_VALUE] (inclusive)
 * * [Long] — Anything else
 */
@ExperimentalApi
public val AutoNumberConverter: ValueConverter<Number> =
    NumberConverters.of(NumberConverters.AutoNumberToStringConverter)

/**
 * Converts between [Byte] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
@ExperimentalApi
public val ByteConverter: ValueConverter<Byte> = NumberConverters.of(NumberConverters.ByteToStringConverter)

/**
 * Converts between [Double] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
@ExperimentalApi
public val DoubleConverter: ValueConverter<Double> = NumberConverters.of(NumberConverters.DoubleToStringConverter)

/**
 * Converts between [Float] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
@ExperimentalApi
public val FloatConverter: ValueConverter<Float> = NumberConverters.of(NumberConverters.FloatToStringConverter)

/**
 * Converts between [Int] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
@ExperimentalApi
public val IntConverter: ValueConverter<Int> = NumberConverters.of(NumberConverters.IntToStringConverter)

/**
 * Converts between [Long] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
@ExperimentalApi
public val LongConverter: ValueConverter<Long> = NumberConverters.of(NumberConverters.LongToStringConverter)

/**
 * Converts between [Short] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
@ExperimentalApi
public val ShortConverter: ValueConverter<Short> = NumberConverters.of(NumberConverters.ShortToStringConverter)

/**
 * Converts between [UByte] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
@ExperimentalApi
public val UByteConverter: ValueConverter<UByte> = NumberConverters.of(NumberConverters.UByteToStringConverter)

/**
 * Converts between [UInt] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
@ExperimentalApi
public val UIntConverter: ValueConverter<UInt> = NumberConverters.of(NumberConverters.UIntToStringConverter)

/**
 * Converts between [ULong] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
@ExperimentalApi
public val ULongConverter: ValueConverter<ULong> = NumberConverters.of(NumberConverters.ULongToStringConverter)

/**
 * Converts between [UShort] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
@ExperimentalApi
public val UShortConverter: ValueConverter<UShort> = NumberConverters.of(NumberConverters.UShortToStringConverter)
