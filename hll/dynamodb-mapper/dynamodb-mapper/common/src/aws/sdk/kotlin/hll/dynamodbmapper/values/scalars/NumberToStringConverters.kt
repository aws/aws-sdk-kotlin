/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter

public object NumberToStringConverters {
    /**
     * Converts between [Number] and [String] values
     */
    public val Auto: Converter<Number, String> =
        Converter(Number::toString) {
            when {
                '.' in it -> it.toDouble()
                else -> when (val longNumber = it.toLong()) {
                    in kotlin.Int.MIN_VALUE..kotlin.Int.MAX_VALUE -> longNumber.toInt()
                    else -> longNumber
                }
            }
        }

    /**
     * Converts between [kotlin.Byte] and [String] values
     */
    public val Byte: Converter<Byte, String> = Converter(kotlin.Byte::toString, String::toByte)

    /**
     * Converts between [kotlin.Double] and [String] values. Because
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     * do not support them, this converter throws exceptions for non-finite numbers such as [Double.NEGATIVE_INFINITY],
     * [Double.POSITIVE_INFINITY], and [Double.NaN].
     */
    public val Double: Converter<Double, String> = run {
        val doubleToString = MonoConverter<Double, String> {
            require(it.isFinite()) { "Cannot convert $it: only finite numbers are supported" }
            it.toString()
        }
        Converter(doubleToString, String::toDouble)
    }

    /**
     * Converts between [kotlin.Float] and [String] values. Because
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     * do not support them, this converter throws exceptions for non-finite numbers such as [Float.NEGATIVE_INFINITY],
     * [Float.POSITIVE_INFINITY], and [Float.NaN].
     */
    public val Float: Converter<Float, String> = run {
        val floatToString = MonoConverter<Float, String> {
            require(it.isFinite()) { "Cannot convert $it: only finite numbers are supported" }
            it.toString()
        }
        Converter(floatToString, String::toFloat)
    }

    /**
     * Converts between [kotlin.Int] and [String] values
     */
    public val Int: Converter<Int, String> = Converter(kotlin.Int::toString, String::toInt)

    /**
     * Converts between [kotlin.Long] and [String] values
     */
    public val Long: Converter<Long, String> = Converter(kotlin.Long::toString, String::toLong)

    /**
     * Converts between [kotlin.Short] and [String] values
     */
    public val Short: Converter<Short, String> = Converter(kotlin.Short::toString, String::toShort)

    /**
     * Converts between [kotlin.UByte] and [String] values
     */
    public val UByte: Converter<UByte, String> = Converter(kotlin.UByte::toString, String::toUByte)

    /**
     * Converts between [kotlin.UInt] and [String] values
     */
    public val UInt: Converter<UInt, String> = Converter(kotlin.UInt::toString, String::toUInt)

    /**
     * Converts between [kotlin.ULong] and [String] values
     */
    public val ULong: Converter<ULong, String> = Converter(kotlin.ULong::toString, String::toULong)

    /**
     * Converts between [kotlin.UShort] and [String] values
     */
    public val UShort: Converter<UShort, String> = Converter(kotlin.UShort::toString, String::toUShort)
}
