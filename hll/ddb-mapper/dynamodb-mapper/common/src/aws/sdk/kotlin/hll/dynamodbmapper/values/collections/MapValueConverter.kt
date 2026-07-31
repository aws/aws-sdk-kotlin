/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterChain
import aws.sdk.kotlin.hll.mapping.core.converters.collections.MapMappingConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import kotlin.jvm.JvmName

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
public object AttributeValueMapValueConverter : ValueConverter<Map<String, AttributeValue>> {
    override fun convertLeft(from: AttributeValue): Map<String, AttributeValue> = from.asM()
    override fun convertRight(from: Map<String, AttributeValue>): AttributeValue = AttributeValue.M(from)
}

/**
 * Converts between a [Map] with keys of type [K] and values of type [V] and
 * [DynamoDB `M` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Document.Map)
 * @param K The type of keys in the map
 * @param V The type of values in the map
 */
public class MapValueConverter<K, V> private constructor(
    composed: ValueConverter<Map<K, V>>,
) : ValueConverter<Map<K, V>> by composed {
    /**
     * Creates a new map converter using the given [entryConverter] as a delegate
     * @param entryConverter A converter for transforming between entries of type `Pair<K, V>` and type
     * `Pair<String, AttributeValue>`
     */
    public constructor(
        entryConverter: Converter<Pair<K, V>, Pair<String, AttributeValue>>,
        attributeValueMapValueConverter: ValueConverter<Map<String, AttributeValue>> = AttributeValueMapValueConverter,
    ) : this(ConverterChain(MapMappingConverter(entryConverter), attributeValueMapValueConverter))

    /**
     * Creates a new map converter using the given [keyConverter] and [valueConverter] as delegates
     * @param keyConverter A converter for transforming between [K] keys and [String] keys
     * @param valueConverter A converter for transforming between [V] values and [AttributeValue]
     */
    public constructor(
        keyConverter: Converter<K, String>,
        valueConverter: ValueConverter<V>,
        attributeValueMapValueConverter: ValueConverter<Map<String, AttributeValue>> = AttributeValueMapValueConverter,
    ) : this(ConverterChain(MapMappingConverter(keyConverter, valueConverter), attributeValueMapValueConverter))
}

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
): MapValueConverter<String, V> = MapValueConverter(Converter.identity(), valueConverter, attributeValueMapValueConverter)
