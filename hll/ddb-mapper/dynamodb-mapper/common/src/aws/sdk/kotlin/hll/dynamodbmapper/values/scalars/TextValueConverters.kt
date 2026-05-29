/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterChain
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Namespace for containing various conversion utilities dealing with text conversion
 */
public object TextConverters {
    /**
     * Converts between [kotlin.CharArray] and [kotlin.String]
     */
    public object CharArray : Converter<kotlin.CharArray, kotlin.String> {
        override fun convertRight(from: kotlin.CharArray): kotlin.String = from.concatToString()
        override fun convertLeft(from: kotlin.String): kotlin.CharArray = from.toCharArray()
    }

    /**
     * Converts between [kotlin.Char] and [kotlin.String]
     */
    public object Char : Converter<kotlin.Char, kotlin.String> {
        override fun convertRight(from: kotlin.Char): kotlin.String = from.toString()
        override fun convertLeft(from: kotlin.String): kotlin.Char = from.single()
    }

    /**
     * Converts between [kotlin.String] and [kotlin.String]
     */
    public object String : Converter<kotlin.String, kotlin.String> {
        override fun convertRight(from: kotlin.String): kotlin.String = from
        override fun convertLeft(from: kotlin.String): kotlin.String = from
    }
}

/**
 * Converts between [String] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
public object StringValueConverter : ValueConverter<String> {
    override fun convertLeft(from: AttributeValue): String = from.asS()
    override fun convertRight(from: String): AttributeValue = AttributeValue.S(from)
}

/**
 * Converts between [CharArray] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
public object CharArrayValueConverter : ValueConverter<CharArray> by ConverterChain(TextConverters.CharArray, StringValueConverter)

/**
 * Converts between [Char] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
public object CharValueConverter : ValueConverter<Char> by ConverterChain(TextConverters.Char, StringValueConverter)
