/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.collections.mapFrom
import aws.sdk.kotlin.hll.mapping.core.converters.collections.mapKeysFrom
import aws.sdk.kotlin.hll.mapping.core.converters.collections.mapValuesFrom
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Converts between [Map] and
 * [DynamoDB `M` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Document.Map).
 * Note that the maps must contain [String] keys and already-converted [AttributeValue] values. This converter is
 * typically chained with another converter which handles converting values to [AttributeValue] either by using the
 * factory function [MapConverter] or by using the [mapFrom]/[mapValuesFrom]/[mapKeysFrom] extension methods.
 *
 * ```kotlin
 * val instantMapConv = MapConverter(InstantConverter.Default) // ValueConverter<Map<String, Instant>>
 * val instantMapConv2 = MapConverter.mapValuesFrom(InstantConverter.Default) // same as above
 * ```
 */
@ExperimentalApi
public val MapConverter: ValueConverter<Map<String, AttributeValue>> = Converter(AttributeValue::M, AttributeValue::asM)

/**
 * Creates a new map converter using the given [keyConverter] and [valueConverter] as delegates
 * @param K The type of keys in the map
 * @param V The type of values in the map
 * @param keyConverter A converter for transforming between [K] keys and [String] keys
 * @param valueConverter A converter for transforming between [V] values and [AttributeValue]
 */
@ExperimentalApi
@Suppress("ktlint:standard:function-naming")
public fun <K, V> MapConverter(
    keyConverter: Converter<K, String>,
    valueConverter: ValueConverter<V>,
): ValueConverter<Map<K, V>> = MapConverter.mapFrom(keyConverter, valueConverter)

/**
 * Creates a new string-keyed map converter using the given [valueConverter] as a delegate
 * @param V The type of values in the map
 * @param valueConverter A converter for transforming between [V] values and [AttributeValue]
 */
@ExperimentalApi
@Suppress("ktlint:standard:function-naming")
public fun <V> MapConverter(valueConverter: ValueConverter<V>): ValueConverter<Map<String, V>> = MapConverter.mapValuesFrom(valueConverter)
