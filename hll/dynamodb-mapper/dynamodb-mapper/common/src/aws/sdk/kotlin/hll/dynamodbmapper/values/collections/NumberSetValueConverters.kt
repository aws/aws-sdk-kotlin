/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.NumberSetValueConverters.Byte
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.NumberSetValueConverters.Double
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.NumberSetValueConverters.Float
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.NumberSetValueConverters.Int
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.NumberSetValueConverters.Long
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.NumberSetValueConverters.Short
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.NumberSetValueConverters.String
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.NumberSetValueConverters.UByte
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.NumberSetValueConverters.UInt
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.NumberSetValueConverters.ULong
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.NumberSetValueConverters.UShort
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberToStringConverters
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.collections.SetMappingConverter
import aws.sdk.kotlin.hll.mapping.core.converters.collections.SetToListConverter
import aws.sdk.kotlin.hll.mapping.core.converters.plus
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Namespace for containing various conversion utilities dealing with numerical set conversion
 */
@ExperimentalApi
public object NumberSetValueConverters {
    /**
     * Converts between a [List] of [String] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public val StringList: ValueConverter<List<String>> =
        Converter(AttributeValue::Ns, AttributeValue::asNs)

    /**
     * Converts between a [Set] of [String] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public val String: ValueConverter<Set<String>> = SetToListConverter<String>() + StringList

    /**
     * Creates a [ValueConverter] which converts between a [Set] of [N] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     * @param N The type of high-level values which will be converted
     */
    public fun <N> of(
        numberConverter: Converter<N, String>,
        stringSetValueConverter: ValueConverter<Set<String>> = String,
    ): ValueConverter<Set<N>> =
        SetMappingConverter(numberConverter) + stringSetValueConverter

    /**
     * Converts between a [Set] of [Byte] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    @ExperimentalApi
    public val Byte: ValueConverter<Set<Byte>> = of(NumberToStringConverters.Byte)

    /**
     * Converts between a [Set] of [Double] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    @ExperimentalApi
    public val Double: ValueConverter<Set<Double>> = of(NumberToStringConverters.Double)

    /**
     * Converts between a [Set] of [Float] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    @ExperimentalApi
    public val Float: ValueConverter<Set<Float>> = of(NumberToStringConverters.Float)

    /**
     * Converts between a [Set] of [Int] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    @ExperimentalApi
    public val Int: ValueConverter<Set<Int>> = of(NumberToStringConverters.Int)

    /**
     * Converts between a [Set] of [Long] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    @ExperimentalApi
    public val Long: ValueConverter<Set<Long>> = of(NumberToStringConverters.Long)

    /**
     * Converts between a [Set] of [Short] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    @ExperimentalApi
    public val Short: ValueConverter<Set<Short>> = of(NumberToStringConverters.Short)

    /**
     * Converts between a [Set] of [UByte] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    @ExperimentalApi
    public val UByte: ValueConverter<Set<UByte>> = of(NumberToStringConverters.UByte)

    /**
     * Converts between a [Set] of [UInt] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    @ExperimentalApi
    public val UInt: ValueConverter<Set<UInt>> = of(NumberToStringConverters.UInt)

    /**
     * Converts between a [Set] of [ULong] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    @ExperimentalApi
    public val ULong: ValueConverter<Set<ULong>> = of(NumberToStringConverters.ULong)

    /**
     * Converts between a [Set] of [UShort] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    @ExperimentalApi
    public val UShort: ValueConverter<Set<UShort>> = of(NumberToStringConverters.UShort)
}
