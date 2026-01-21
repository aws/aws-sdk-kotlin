/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.collections

import aws.sdk.kotlin.hll.mapping.core.converters.*
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Namespace for containing various conversion utilities dealing with [Map] mapping
 */
@ExperimentalApi
public object MapMappingConverters {
    /**
     * Creates a one-way converter for transforming [Map] with keys of type [TK] to [Map] with keys of type [FK]. The
     * values of the map are unchanged.
     * @param FK The type of keys being converted to
     * @param TK The type of keys being converted from
     * @param V The type of values
     * @param keyConverter A one-way converter of [TK] keys to [FK] keys
     */
    public fun <FK, TK, V> ofKeys(keyConverter: ConvertsFrom<FK, TK>): ConvertsFrom<Map<FK, V>, Map<TK, V>> = ConvertsFrom { to: Map<TK, V> ->
        to.mapKeys { e: Map.Entry<TK, V> -> keyConverter.convertFrom(e.key) }
    }

    /**
     * Chains this map converter with a key converter, yielding a new converter which performs a two-stage mapping
     * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical
     * steps in their actual implementation.)
     * @param FK The type of keys being converted from
     * @param FK2 The type of keys being converted to
     * @param V The type of values
     * @param T The target type of this converter
     * @param keyConverter The key converter to chain together with this map converter. Note that the target type of the
     * given [keyConverter] must be the same as the source key type of this converter.
     */
    public fun <FK, FK2, V, T> ConvertsFrom<Map<FK, V>, T>.mapConvertsKeysFrom(
        keyConverter: ConvertsFrom<FK2, FK>,
    ): ConvertsFrom<Map<FK2, V>, T> = this.andThenConvertsFrom(ofKeys(keyConverter))

    /**
     * Creates a one-way converter for transforming [Map] with values of type [TV] to [Map] with values of type [FV].
     * The keys of the map are unchanged.
     * @param K The type of keys
     * @param FV The type of values being converted to
     * @param TV The type of values being converted from
     * @param valueConverter A one-way converter of [TV] values to [FV] values
     */
    public fun <K, FV, TV> ofValues(
        valueConverter: ConvertsFrom<FV, TV>,
    ): ConvertsFrom<Map<K, FV>, Map<K, TV>> = ConvertsFrom { to: Map<K, TV> ->
        to.mapValues { e: Map.Entry<K, TV> -> valueConverter.convertFrom(e.value) }
    }

    /**
     * Chains this map converter with a value converter, yielding a new converter which performs a two-stage mapping
     * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical
     * steps in their actual implementation.)
     * @param K The type of keys
     * @param FV The type of values being converted to
     * @param FV2 The type of values being converted from
     * @param T The target type of this converter
     * @param valueConverter The value converter to chain together with this map converter. Note that the target type of
     * the given [valueConverter] must be the same as the source value type of this converter.
     */
    public fun <K, FV, FV2, T> ConvertsFrom<Map<K, FV>, T>.mapConvertsValuesFrom(
        valueConverter: ConvertsFrom<FV2, FV>,
    ): ConvertsFrom<Map<K, FV2>, T> = this.andThenConvertsFrom(ofValues(valueConverter))

    /**
     * Creates a one-way converter for transforming [Map] with keys of type [TK] and values of type [TV] to [Map] with
     * keys of type [FK] and values of type [FV]
     * @param FK The type of keys being converted to
     * @param FV The type of values being converted to
     * @param TK The type of keys being converted from
     * @param TV The type of values being converted from
     * @param entryConverter A one-way converter of [TK]/[TV] pairs to [FK]/[FV] pairs
     */
    public fun <FK, FV, TK, TV> of(
        entryConverter: ConvertsFrom<Pair<FK, FV>, Pair<TK, TV>>,
    ): ConvertsFrom<Map<FK, FV>, Map<TK, TV>> = ConvertsFrom { to: Map<TK, TV> ->
        to.entries.associate { e: Map.Entry<TK, TV> -> entryConverter.convertFrom(e.toPair()) }
    }

    /**
     * Chains this map converter with an entry converter, yielding a new converter which performs a two stage mapping
     * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical
     * steps in their actual implementation.)
     * @param FK The type of keys being converted to
     * @param FV The type of values being converted to
     * @param FK2 The type of keys being converted from
     * @param FV2 The type of values being converted from
     * @param entryConverter The entry converter to chain together with this map converter. Note that the target types
     * of the given [entryConverter] must be the same as the source types of this converter.
     */
    public fun <FK, FV, FK2, FV2, T> ConvertsFrom<Map<FK, FV>, T>.mapConvertsFrom(
        entryConverter: ConvertsFrom<Pair<FK2, FV2>, Pair<FK, FV>>,
    ): ConvertsFrom<Map<FK2, FV2>, T> = this.andThenConvertsFrom(of(entryConverter))

    /**
     * Creates a one-way converter for transforming [Map] with keys of type [FK] to [Map] with keys of type [TK]. The
     * values of the map are unchanged.
     * @param FK The type of keys being converted from
     * @param TK The type of keys being converted to
     * @param V The type of values
     * @param keyConverter A one-way converter of [FK] keys to [TK] keys
     */
    public fun <FK, TK, V> ofKeys(
        keyConverter: ConvertsTo<FK, TK>,
    ): ConvertsTo<Map<FK, V>, Map<TK, V>> = ConvertsTo { from: Map<FK, V> ->
        from.mapKeys { e: Map.Entry<FK, V> -> keyConverter.convertTo(e.key) }
    }

    /**
     * Chains this map converter with a key converter, yielding a new converter which performs a two-stage mapping
     * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical
     * steps in their actual implementation.)
     * @param F The source type of this converter
     * @param TK The type of keys being converted from
     * @param TK2 The type of keys being converted to
     * @param V The type of values
     * @param keyConverter The key converter to chain together with this map converter. Note that the source type of the
     * given [keyConverter] must be the same as the target key type of this converter.
     */
    public fun <F, TK, TK2, V> ConvertsTo<F, Map<TK, V>>.mapConvertsKeysTo(
        keyConverter: ConvertsTo<TK, TK2>,
    ): ConvertsTo<F, Map<TK2, V>> = this.andThenConvertsTo(ofKeys(keyConverter))

    /**
     * Creates a one-way converter for transforming [Map] with values of type [FV] to [Map] with values of type [TV].
     * The keys of the map are unchanged.
     * @param K The type of keys
     * @param FV The type of values being converted from
     * @param TV The type of values being converted to
     * @param valueConverter A one-way converter of [FV] values to [TV] values
     */
    public fun <K, FV, TV> ofValues(
        valueConverter: ConvertsTo<FV, TV>,
    ): ConvertsTo<Map<K, FV>, Map<K, TV>> = ConvertsTo { from: Map<K, FV> ->
        from.mapValues { e: Map.Entry<K, FV> -> valueConverter.convertTo(e.value) }
    }

    /**
     * Chains this map converter with a value converter, yielding a new converter which performs a two-stage mapping
     * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical
     * steps in their actual implementation.)
     * @param F The source type of this converter
     * @param K The type of keys
     * @param TV The type of values being converted from
     * @param TV2 The type of values being converted to
     * @param valueConverter The value converter to chain together with this map converter. Note that the source type of
     * the given [valueConverter] must be the same as the target value type of this converter.
     */
    public fun <F, K, TV, TV2> ConvertsTo<F, Map<K, TV>>.mapConvertsValuesTo(
        valueConverter: ConvertsTo<TV, TV2>,
    ): ConvertsTo<F, Map<K, TV2>> = this.andThenConvertsTo(ofValues(valueConverter))

    /**
     * Creates a one-way converter for transforming [Map] with keys of type [FK] and values of type [FV] to [Map] with
     * keys of type [TK] and values of type [TV]
     * @param FK The type of keys being converted from
     * @param FV The type of values being converted from
     * @param TK The type of keys being converted to
     * @param TV The type of values being converted to
     * @param entryConverter A one-way converter of [FK]/[FV] pairs to [TK]/[TV] pairs
     */
    public fun <FK, FV, TK, TV> of(
        entryConverter: ConvertsTo<Pair<FK, FV>, Pair<TK, TV>>,
    ): ConvertsTo<Map<FK, FV>, Map<TK, TV>> = ConvertsTo { from: Map<FK, FV> ->
        from.entries.associate { e -> entryConverter.convertTo(e.toPair()) }
    }

    /**
     * Chains this map converter with an entry converter, yielding a new converter which performs a two-stage mapping
     * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical
     * steps in their actual implementation.)
     * @param F The source type of this converter
     * @param TK The type of keys being converted from
     * @param TV The type of values being converted from
     * @param TK2 The type of keys being converted to
     * @param TV2 The type of values being converted to
     * @param entryConverter The entry converter to chain together with this map converter. Note that the source types
     * of the given [entryConverter] must be the same as the target types of this converter.
     */
    public fun <F, TK, TV, TK2, TV2> ConvertsTo<F, Map<TK, TV>>.mapConvertsTo(
        entryConverter: ConvertsTo<Pair<TK, TV>, Pair<TK2, TV2>>,
    ): ConvertsTo<F, Map<TK2, TV2>> = this.andThenConvertsTo(of(entryConverter))

    /**
     * Creates a two-way converter for transforming between [Map] with keys of type [FK] and [Map] with keys of type
     * [TK]. The values of maps are unchanged.
     * @param FK The type of keys being converted from
     * @param TK The type of keys being converted to
     * @param V The type of values
     * @param keyConverter A converter for transforming between keys of type [FK] and [TK]
     */
    public fun <FK, TK, V> ofKeys(
        keyConverter: Converter<FK, TK>,
    ): Converter<Map<FK, V>, Map<TK, V>> = Converter(ofKeys(keyConverter as ConvertsTo<FK, TK>), ofKeys(keyConverter as ConvertsFrom<FK, TK>))

    /**
     * Creates a two-way converter for transforming between [Map] with values of type [FV] and [Map] with values of type
     * [TV]. The keys of the map are unchanged.
     * @param K The type of keys
     * @param FV The type of values being converted from
     * @param TV The type of values being converted to
     * @param valueConverter A converter for transforming between values of type [FV] and [TV]
     */
    public fun <K, FV, TV> ofValues(
        valueConverter: Converter<FV, TV>,
    ): Converter<Map<K, FV>, Map<K, TV>> = Converter(ofValues(valueConverter as ConvertsTo<FV, TV>), ofValues(valueConverter as ConvertsFrom<FV, TV>))

    /**
     * Creates a two-way converter for transforming between [Map] with keys of type [FK] and values of type [FV] to
     * [Map] with keys of type [TK] and values of type [TV]
     * @param FK The type of keys being converted from
     * @param FV The type of values being converted from
     * @param TK The type of keys being converted to
     * @param TV The type of values being converted to
     * @param entryConverter A converter for transforming between [FK]/[FV] pairs and [TK]/[TV] pairs
     */
    public fun <FK, FV, TK, TV> of(
        entryConverter: Converter<Pair<FK, FV>, Pair<TK, TV>>,
    ): Converter<Map<FK, FV>, Map<TK, TV>> = Converter(
        of(entryConverter as ConvertsTo<Pair<FK, FV>, Pair<TK, TV>>),
        of(entryConverter as ConvertsFrom<Pair<FK, FV>, Pair<TK, TV>>),
    )
}

/**
 * Chains this map converter with a key converter, yielding a new converter which performs a two-stage mapping
 * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical steps
 * in their actual implementation.)
 * @param FK The type of keys being converted to
 * @param FK2 The type of keys being converted from
 * @param V The type of values
 * @param T The target type of this converter
 * @param keyConverter The key converter to chain together with this map converter. Note that the target key type of the
 * given [keyConverter] must be the same as the source key type of this converter.
 */
@ExperimentalApi
public fun <FK, FK2, V, T> Converter<Map<FK, V>, T>.mapKeysFrom(
    keyConverter: Converter<FK2, FK>,
): Converter<Map<FK2, V>, T> = this.andThenFrom(MapMappingConverters.ofKeys(keyConverter))

/**
 * Chains this map converter with a key converter, yielding a new converter which performs a two-stage mapping
 * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical steps
 * in their actual implementation.)
 * @param F The source type of this converter
 * @param TK The type of keys being converted from
 * @param TK2 The type of keys being converted to
 * @param V The type of values
 * @param keyConverter The key converter to chain together with this map converter. Note that the source key type of the
 * given [keyConverter] must be the same as the target key type of this converter.
 */
@ExperimentalApi
public fun <F, TK, TK2, V> Converter<F, Map<TK, V>>.mapKeysTo(
    keyConverter: Converter<TK, TK2>,
): Converter<F, Map<TK2, V>> = this.andThenTo(MapMappingConverters.ofKeys(keyConverter))

/**
 * Chains this map converter with a value converter, yielding a new converter which performs a two-stage mapping
 * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical steps
 * in their actual implementation.)
 * @param K The type of keys
 * @param FV The type of values being converted to
 * @param FV2 The type of values being converted from
 * @param T The target type of this converter
 * @param valueConverter The value converter to chain together with this map converter. Note that the target value type
 * of the given [valueConverter] must be the same as the source value type of this converter.
 */
@ExperimentalApi
public fun <K, FV, FV2, T> Converter<Map<K, FV>, T>.mapValuesFrom(
    valueConverter: Converter<FV2, FV>,
): Converter<Map<K, FV2>, T> = this.andThenFrom(MapMappingConverters.ofValues(valueConverter))

/**
 * Chains this map converter with a value converter, yielding a new converter which performs a two-stage mapping
 * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical steps
 * in their actual implementation.)
 * @param F The source type of this converter
 * @param K The type of keys
 * @param TV The type of values being converted from
 * @param TV2 The type of values being converted to
 * @param valueConverter The value converter to chain together with this map converter. Note that the source value type
 * of the given [valueConverter] must be the same as the target value type of this converter.
 */
@ExperimentalApi
public fun <F, K, TV, TV2> Converter<F, Map<K, TV>>.mapValuesTo(
    valueConverter: Converter<TV, TV2>,
): Converter<F, Map<K, TV2>> = this.andThenTo(MapMappingConverters.ofValues(valueConverter))

/**
 * Convenience function for combining independent converters into `Converter<Pair<F1, F2>, Pair<T1, T2>>`, suitable for
 * use as a map entry converter
 * @param F1 The first type of value being converted from
 * @param T1 The first type of value being converted to
 * @param F2 The second type of value being converted from
 * @param T2 The second type of value being converted to
 * @param other The converter to zip with this one
 */
private fun <F1, T1, F2, T2> Converter<F1, T1>.zip(other: Converter<F2, T2>) = Converter(
    convertTo = { from: Pair<F1, F2> -> this.convertTo(from.first) to other.convertTo(from.second) },
    convertFrom = { to: Pair<T1, T2> -> this.convertFrom(to.first) to other.convertFrom(to.second) },
)

/**
 * Chains this map converter with an entry converter, yielding a new converter which performs a two-stage mapping
 * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical steps
 * in their actual implementation.)
 * @param FK The type of keys being converted to
 * @param FV The type of values being converted to
 * @param FK2 The type of keys being converted from
 * @param FV2 The type of values being converted from
 * @param T The target type of this converter
 * @param entryConverter The entry converter to chain together with this map converter. Note that the target types of
 * the given [entryConverter] must be the same as the source types of this converter.
 */
@ExperimentalApi
public fun <FK, FV, FK2, FV2, T> Converter<Map<FK, FV>, T>.mapFrom(
    entryConverter: Converter<Pair<FK2, FV2>, Pair<FK, FV>>,
): Converter<Map<FK2, FV2>, T> = this.andThenFrom(MapMappingConverters.of(entryConverter))

/**
 * Chains this map converter with a key converter and a value converter, yielding a new converter which performs a
 * two-stage mapping conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of
 * multiple logical steps in their actual implementation.)
 * @param FK The type of keys being converted to
 * @param FV The type of values being converted to
 * @param FK2 The type of keys being converted from
 * @param FV2 The type of values being converted from
 * @param T The target type of this converter
 * @param keyConverter The key converter to chain together with this map converter. Note that the target key type of the
 * given [keyConverter] must be the same as the source key type of this converter.
 * @param valueConverter The value converter to chain together with this map converter. Note that the target value type
 * of the given [valueConverter] must be the same as the source value type of this converter.
 */
@ExperimentalApi
public fun <FK, FV, FK2, FV2, T> Converter<Map<FK, FV>, T>.mapFrom(
    keyConverter: Converter<FK2, FK>,
    valueConverter: Converter<FV2, FV>,
): Converter<Map<FK2, FV2>, T> = mapFrom(keyConverter.zip(valueConverter))

/**
 * Chains this map converter with an entry converter, yielding a new converter which performs a two-stage mapping
 * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical steps
 * in their actual implementation.)
 * @param F The source type of this converter
 * @param TK The type of keys being converted from
 * @param TV The type of values being converted from
 * @param TK2 The type of keys being converted to
 * @param TV2 The type of values being converted to
 * @param entryConverter The entry converter to chain together with this map converter. Note that the source types of
 * the given [entryConverter] must be the same as the target types of this converter.
 */
@ExperimentalApi
public fun <F, TK, TV, TK2, TV2> Converter<F, Map<TK, TV>>.mapTo(
    entryConverter: Converter<Pair<TK, TV>, Pair<TK2, TV2>>,
): Converter<F, Map<TK2, TV2>> = this.andThenTo(MapMappingConverters.of(entryConverter))

/**
 * Chains this map converter with a key converter and a value converter, yielding a new converter which performs a
 * two-stage mapping conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of
 * multiple logical steps in their actual implementation.)
 * @param F The source type of this converter
 * @param TK The type of keys being converted from
 * @param TV The type of values being converted from
 * @param TK2 The type of keys being converted to
 * @param TV2 The type of values being converted to
 * @param keyConverter The key converter to chain together with this map converter. Note that the source key type of the
 * given [keyConverter] must be the same as the target key type of this converter.
 * @param valueConverter The value converter to chain together with this map converter. Note that the source value type
 * of the given [valueConverter] must be the same as the target value type of this converter.
 */
@ExperimentalApi
public fun <F, TK, TV, TK2, TV2> Converter<F, Map<TK, TV>>.mapTo(
    keyConverter: Converter<TK, TK2>,
    valueConverter: Converter<TV, TV2>,
): Converter<F, Map<TK2, TV2>> = mapTo(keyConverter.zip(valueConverter))
