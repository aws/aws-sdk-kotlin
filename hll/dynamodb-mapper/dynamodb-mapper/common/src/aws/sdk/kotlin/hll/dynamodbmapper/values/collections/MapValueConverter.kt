/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.collections.MapMappingConverter
import aws.sdk.kotlin.hll.mapping.core.converters.plus
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between [Map] and
 * [DynamoDB `M` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Document.Map).
 * Note that the maps must contain [String] keys and already-converted [AttributeValue] values. This converter is
 * typically chained with another converter which handles converting values to [AttributeValue] either by using the
 * factory function [MapValueConverter] or by using the [mapFrom]/[mapValuesFrom]/[mapKeysFrom] extension methods.
 *
 * ```kotlin
 * val instantMapConv = MapValueConverter(InstantConverter.Default) // ValueConverter<Map<String, Instant>>
 * val instantMapConv2 = MapValueConverter.mapValuesFrom(InstantConverter.Default) // same as above
 * ```
 */
public val AttributeValueMapValueConverter: ValueConverter<Map<String, AttributeValue>> =
    Converter(AttributeValue::M, AttributeValue::asM)

/**
 * Creates a new map converter using the given [keyConverter] and [valueConverter] as delegates
 * @param K The type of keys in the map
 * @param V The type of values in the map
 * @param entryConverter A converter for transforming between entries of type `Pair<K, V>` and type
 * `Pair<String, AttributeValue>`
 */
@JvmName("MapValueConverterByEntryConverter")
@Suppress("ktlint:standard:function-naming")
public fun <K, V> MapValueConverter(
    entryConverter: Converter<Pair<K, V>, Pair<String, AttributeValue>>,
    attributeValueMapValueConverter: ValueConverter<Map<String, AttributeValue>> = AttributeValueMapValueConverter,
): ValueConverter<Map<K, V>> = MapMappingConverter(entryConverter) + attributeValueMapValueConverter

/**
 * Creates a new map converter using the given [keyConverter] and [valueConverter] as delegates
 * @param K The type of keys in the map
 * @param V The type of values in the map
 * @param keyConverter A converter for transforming between [K] keys and [String] keys
 * @param valueConverter A converter for transforming between [V] values and [AttributeValue]
 */
@JvmName("MapValueConverterByKeyAndValueConverter")
@Suppress("ktlint:standard:function-naming")
public fun <K, V> MapValueConverter(
    keyConverter: Converter<K, String>,
    valueConverter: ValueConverter<V>,
    attributeValueMapValueConverter: ValueConverter<Map<String, AttributeValue>> = AttributeValueMapValueConverter,
): ValueConverter<Map<K, V>> = MapMappingConverter(keyConverter, valueConverter) + attributeValueMapValueConverter

/**
 * Creates a new string-keyed map converter using the given [valueConverter] as a delegate
 * @param V The type of values in the map
 * @param valueConverter A converter for transforming between [V] values and [AttributeValue]
 */
@JvmName("MapValueConverterByValueConverter")
@Suppress("ktlint:standard:function-naming")
public fun <V> MapValueConverter(
    valueConverter: ValueConverter<V>,
    attributeValueMapValueConverter: ValueConverter<Map<String, AttributeValue>> = AttributeValueMapValueConverter,
): ValueConverter<Map<String, V>> = MapMappingConverter(Converter.identity<String>(), valueConverter) + attributeValueMapValueConverter
