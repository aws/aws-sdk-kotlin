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
    public class StringList : ValueConverter<List<kotlin.String>> by StringList {
        public companion object : ValueConverter<List<kotlin.String>> {
            override fun convertLeft(from: AttributeValue): List<kotlin.String> = from.asNs()
            override fun convertRight(from: List<kotlin.String>): AttributeValue = AttributeValue.Ns(from)
        }
    }

    /**
     * Converts between a [Set] of [String] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public class String : ValueConverter<Set<kotlin.String>> by String {
        public companion object : ConverterChain<Set<kotlin.String>, List<kotlin.String>, AttributeValue>(SetToListConverter(), StringList)
    }

    /**
     * Converts between a [Set] of [Byte] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public class Byte : ValueConverter<Set<kotlin.Byte>> by Byte {
        public companion object : ConverterChain<Set<kotlin.Byte>, Set<kotlin.String>, AttributeValue>(SetMappingConverter(NumberToStringConverters.Byte), String)
    }

    /**
     * Converts between a [Set] of [Double] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public class Double : ValueConverter<Set<kotlin.Double>> by Double {
        public companion object : ConverterChain<Set<kotlin.Double>, Set<kotlin.String>, AttributeValue>(SetMappingConverter(NumberToStringConverters.Double), String)
    }

    /**
     * Converts between a [Set] of [Float] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public class Float : ValueConverter<Set<kotlin.Float>> by Float {
        public companion object : ConverterChain<Set<kotlin.Float>, Set<kotlin.String>, AttributeValue>(SetMappingConverter(NumberToStringConverters.Float), String)
    }

    /**
     * Converts between a [Set] of [Int] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public class Int : ValueConverter<Set<kotlin.Int>> by Int {
        public companion object : ConverterChain<Set<kotlin.Int>, Set<kotlin.String>, AttributeValue>(SetMappingConverter(NumberToStringConverters.Int), String)
    }

    /**
     * Converts between a [Set] of [Long] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public class Long : ValueConverter<Set<kotlin.Long>> by Long {
        public companion object : ConverterChain<Set<kotlin.Long>, Set<kotlin.String>, AttributeValue>(SetMappingConverter(NumberToStringConverters.Long), String)
    }

    /**
     * Converts between a [Set] of [Short] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public class Short : ValueConverter<Set<kotlin.Short>> by Short {
        public companion object : ConverterChain<Set<kotlin.Short>, Set<kotlin.String>, AttributeValue>(SetMappingConverter(NumberToStringConverters.Short), String)
    }

    /**
     * Converts between a [Set] of [UByte] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public class UByte : ValueConverter<Set<kotlin.UByte>> by UByte {
        public companion object : ConverterChain<Set<kotlin.UByte>, Set<kotlin.String>, AttributeValue>(SetMappingConverter(NumberToStringConverters.UByte), String)
    }

    /**
     * Converts between a [Set] of [UInt] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public class UInt : ValueConverter<Set<kotlin.UInt>> by UInt {
        public companion object : ConverterChain<Set<kotlin.UInt>, Set<kotlin.String>, AttributeValue>(SetMappingConverter(NumberToStringConverters.UInt), String)
    }

    /**
     * Converts between a [Set] of [ULong] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public class ULong : ValueConverter<Set<kotlin.ULong>> by ULong {
        public companion object : ConverterChain<Set<kotlin.ULong>, Set<kotlin.String>, AttributeValue>(SetMappingConverter(NumberToStringConverters.ULong), String)
    }

    /**
     * Converts between a [Set] of [UShort] elements and
     * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
     */
    public class UShort : ValueConverter<Set<kotlin.UShort>> by UShort {
        public companion object : ConverterChain<Set<kotlin.UShort>, Set<kotlin.String>, AttributeValue>(SetMappingConverter(NumberToStringConverters.UShort), String)
    }
}
