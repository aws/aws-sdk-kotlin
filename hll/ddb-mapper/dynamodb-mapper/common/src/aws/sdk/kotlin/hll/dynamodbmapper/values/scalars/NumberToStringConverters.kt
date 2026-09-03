/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.mapping.core.converters.Converter

public object NumberToStringConverters {
    /**
     * Converts between [Number] and [String] values
     */
    public object Auto : Converter<Number, String> {
        override fun convertRight(from: Number): String = from.toString()
        override fun convertLeft(from: String): Number = when {
            '.' in from -> from.toDouble()
            else -> when (val longNumber = from.toLong()) {
                in kotlin.Int.MIN_VALUE..kotlin.Int.MAX_VALUE -> longNumber.toInt()
                else -> longNumber
            }
        }
    }

    /**
     * Converts between [kotlin.Byte] and [String] values
     */
    public object Byte : Converter<kotlin.Byte, String> {
        override fun convertRight(from: kotlin.Byte): String = from.toString()
        override fun convertLeft(from: String): kotlin.Byte = from.toByte()
    }

    /**
     * Converts between [kotlin.Double] and [String] values. Because
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     * do not support them, this converter throws exceptions for non-finite numbers such as [Double.NEGATIVE_INFINITY],
     * [Double.POSITIVE_INFINITY], and [Double.NaN].
     */
    public object Double : Converter<kotlin.Double, String> {
        override fun convertRight(from: kotlin.Double): String {
            require(from.isFinite()) { "Cannot convert $from: only finite numbers are supported" }
            return from.toString()
        }
        override fun convertLeft(from: String): kotlin.Double = from.toDouble()
    }

    /**
     * Converts between [kotlin.Float] and [String] values. Because
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     * do not support them, this converter throws exceptions for non-finite numbers such as [Float.NEGATIVE_INFINITY],
     * [Float.POSITIVE_INFINITY], and [Float.NaN].
     */
    public object Float : Converter<kotlin.Float, String> {
        override fun convertRight(from: kotlin.Float): String {
            require(from.isFinite()) { "Cannot convert $from: only finite numbers are supported" }
            return from.toString()
        }
        override fun convertLeft(from: String): kotlin.Float = from.toFloat()
    }

    /**
     * Converts between [kotlin.Int] and [String] values
     */
    public object Int : Converter<kotlin.Int, String> {
        override fun convertRight(from: kotlin.Int): String = from.toString()
        override fun convertLeft(from: String): kotlin.Int = from.toInt()
    }

    /**
     * Converts between [kotlin.Long] and [String] values
     */
    public object Long : Converter<kotlin.Long, String> {
        override fun convertRight(from: kotlin.Long): String = from.toString()
        override fun convertLeft(from: String): kotlin.Long = from.toLong()
    }

    /**
     * Converts between [kotlin.Short] and [String] values
     */
    public object Short : Converter<kotlin.Short, String> {
        override fun convertRight(from: kotlin.Short): String = from.toString()
        override fun convertLeft(from: String): kotlin.Short = from.toShort()
    }

    /**
     * Converts between [kotlin.UByte] and [String] values
     */
    public object UByte : Converter<kotlin.UByte, String> {
        override fun convertRight(from: kotlin.UByte): String = from.toString()
        override fun convertLeft(from: String): kotlin.UByte = from.toUByte()
    }

    /**
     * Converts between [kotlin.UInt] and [String] values
     */
    public object UInt : Converter<kotlin.UInt, String> {
        override fun convertRight(from: kotlin.UInt): String = from.toString()
        override fun convertLeft(from: String): kotlin.UInt = from.toUInt()
    }

    /**
     * Converts between [kotlin.ULong] and [String] values
     */
    public object ULong : Converter<kotlin.ULong, String> {
        override fun convertRight(from: kotlin.ULong): String = from.toString()
        override fun convertLeft(from: String): kotlin.ULong = from.toULong()
    }

    /**
     * Converts between [kotlin.UShort] and [String] values
     */
    public object UShort : Converter<kotlin.UShort, String> {
        override fun convertRight(from: kotlin.UShort): String = from.toString()
        override fun convertLeft(from: String): kotlin.UShort = from.toUShort()
    }
}
