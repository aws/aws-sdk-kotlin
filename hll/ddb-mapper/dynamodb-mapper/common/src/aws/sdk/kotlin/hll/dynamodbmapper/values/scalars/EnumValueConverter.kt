/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterChain
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterImpl
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * A [ValueConverter] for enums of type [E]
 * @param E The [Enum] type for which to create a [ValueConverter]
 */
public class EnumValueConverter<E : Enum<E>>(
    enumToString: Converter<E, String>,
) : ConverterChain<E, String, AttributeValue>(enumToString, StringValueConverter)

/**
 * Instantiates a new [EnumValueConverter] for enums of type [E]
 * @param E The [Enum] type for which to create a [ValueConverter]
 */
@Suppress("ktlint:standard:function-naming")
public inline fun <reified E : Enum<E>> EnumValueConverter(): EnumValueConverter<E> =
    EnumValueConverter(ConverterImpl({ it.name }, { enumValueOf(it) }))
