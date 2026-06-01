/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberToStringConverters
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterChain
import aws.sdk.kotlin.hll.mapping.core.converters.collections.SetMappingConverter
import aws.sdk.kotlin.hll.mapping.core.converters.collections.SetToListConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Namespace for containing various conversion utilities dealing with numerical set conversion
 */
public object NumberSetValueConverters {
    /**
     * Converts between a [List] of [String] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public object StringList : ValueConverter<List<kotlin.String>> {
        override fun convertLeft(from: AttributeValue): List<kotlin.String> = from.asNs()
        override fun convertRight(from: List<kotlin.String>): AttributeValue = AttributeValue.Ns(from)
    }

    /**
     * Converts between a [Set] of [String] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public object String : ValueConverter<Set<kotlin.String>> by ConverterChain(SetToListConverter(), StringList)

    /**
     * Converts between a [Set] of [Byte] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public object Byte : ValueConverter<Set<kotlin.Byte>> by ConverterChain(SetMappingConverter(NumberToStringConverters.Byte), String)

    /**
     * Converts between a [Set] of [Double] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public object Double : ValueConverter<Set<kotlin.Double>> by ConverterChain(SetMappingConverter(NumberToStringConverters.Double), String)

    /**
     * Converts between a [Set] of [Float] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public object Float : ValueConverter<Set<kotlin.Float>> by ConverterChain(SetMappingConverter(NumberToStringConverters.Float), String)

    /**
     * Converts between a [Set] of [Int] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public object Int : ValueConverter<Set<kotlin.Int>> by ConverterChain(SetMappingConverter(NumberToStringConverters.Int), String)

    /**
     * Converts between a [Set] of [Long] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public object Long : ValueConverter<Set<kotlin.Long>> by ConverterChain(SetMappingConverter(NumberToStringConverters.Long), String)

    /**
     * Converts between a [Set] of [Short] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public object Short : ValueConverter<Set<kotlin.Short>> by ConverterChain(SetMappingConverter(NumberToStringConverters.Short), String)

    /**
     * Converts between a [Set] of [UByte] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public object UByte : ValueConverter<Set<kotlin.UByte>> by ConverterChain(SetMappingConverter(NumberToStringConverters.UByte), String)

    /**
     * Converts between a [Set] of [UInt] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public object UInt : ValueConverter<Set<kotlin.UInt>> by ConverterChain(SetMappingConverter(NumberToStringConverters.UInt), String)

    /**
     * Converts between a [Set] of [ULong] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public object ULong : ValueConverter<Set<kotlin.ULong>> by ConverterChain(SetMappingConverter(NumberToStringConverters.ULong), String)

    /**
     * Converts between a [Set] of [UShort] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public object UShort : ValueConverter<Set<kotlin.UShort>> by ConverterChain(SetMappingConverter(NumberToStringConverters.UShort), String)
}
