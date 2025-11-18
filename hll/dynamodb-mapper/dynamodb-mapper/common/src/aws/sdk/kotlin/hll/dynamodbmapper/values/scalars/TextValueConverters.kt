/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.plus
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Namespace for containing various conversion utilities dealing with text conversion
 */
@ExperimentalApi
public object TextConverters {
    /**
     * Converts between [kotlin.CharArray] and [kotlin.String]
     */
    public val CharArray: Converter<CharArray, String> =
        Converter(kotlin.CharArray::concatToString, kotlin.String::toCharArray)

    /**
     * Converts between [kotlin.Char] and [kotlin.String]
     */
    public val Char: Converter<Char, String> =
        Converter(kotlin.Char::toString, kotlin.String::single)

    /**
     * Converts between [kotlin.String] and [kotlin.String]
     */
    public val String: Converter<String, String> = Converter.identity()
}

/**
 * Converts between [String] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
@ExperimentalApi
public val StringValueConverter: ValueConverter<String> = Converter(AttributeValue::S, AttributeValue::asS)

/**
 * Converts between [CharArray] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
@ExperimentalApi
public val CharArrayValueConverter: ValueConverter<CharArray> = TextConverters.CharArray + StringValueConverter

/**
 * Converts between [Char] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
@ExperimentalApi
public val CharValueConverter: ValueConverter<Char> = TextConverters.Char + StringValueConverter
