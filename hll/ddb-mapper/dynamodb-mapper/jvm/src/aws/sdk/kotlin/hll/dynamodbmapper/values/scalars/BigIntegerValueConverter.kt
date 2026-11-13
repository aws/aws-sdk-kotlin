/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.NumberSetValueConverters
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterChain
import aws.sdk.kotlin.hll.mapping.core.converters.collections.SetMappingConverter
import java.math.BigInteger

/**
 * Converts between [java.math.BigInteger] and [String] values.
 *
 * This converter is only available on the JVM. Multiplatform code should use the
 * [aws.smithy.kotlin.runtime.content.BigInteger] converter instead.
 */
public object BigIntegerToStringConverter : Converter<BigInteger, String> {
    override fun convertRight(from: BigInteger): String = from.toString()
    override fun convertLeft(from: String): BigInteger = BigInteger(from)
}

/**
 * Converts between [java.math.BigInteger] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number).
 * Because `N` values are stored as decimal strings, this converter preserves the full range of whole numbers that
 * DynamoDB supports.
 *
 * This converter is only available on the JVM. Multiplatform code should use the
 * [aws.smithy.kotlin.runtime.content.BigInteger] converter instead.
 */
public object BigIntegerValueConverter :
    ValueConverter<BigInteger> by ConverterChain(BigIntegerToStringConverter, NumericalStringValueConverter)

/**
 * Converts between a [Set] of [java.math.BigInteger] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes).
 *
 * This converter is only available on the JVM. Multiplatform code should use the
 * [aws.smithy.kotlin.runtime.content.BigInteger] converter instead.
 */
public object BigIntegerSetValueConverter :
    ValueConverter<Set<BigInteger>> by ConverterChain(SetMappingConverter(BigIntegerToStringConverter), NumberSetValueConverters.String)
