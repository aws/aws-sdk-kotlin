/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.collections

import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.hll.mapping.core.converters.reversedBy

/**
 * Creates a map-mapping [MonoConverter] which turns values of type `Map<AK, AV>` into values of type `Map<BK, BV>`
 * @param AK The key type to convert from
 * @param AV The value type to convert from
 * @param BK The key type to convert to
 * @param BV The value type to convert to
 * @param delegate A [MonoConverter] from type `Pair<AK, AV>` to type `Pair<BK, BV>` to use for each map entry
 */
@Suppress("ktlint:standard:function-naming")
public fun <AK, AV, BK, BV> MapMappingMonoConverter(
    delegate: MonoConverter<Pair<AK, AV>, Pair<BK, BV>>,
): MonoConverter<Map<AK, AV>, Map<BK, BV>> = MonoConverter { from ->
    from.map { entry ->
        delegate.convert(entry.toPair())
    }.toMap()
}

/**
 * Creates a map-mapping [MonoConverter] which turns values of type `Map<AK, AV>` into values of type `Map<BK, BV>`
 * @param AK The key type to convert from
 * @param AV The value type to convert from
 * @param BK The key type to convert to
 * @param BV The value type to convert to
 * @param keyDelegate A [MonoConverter] from type [AK] to type [BK] to use for each map key
 * @param valueDelegate A [MonoConverter] from type [AV] to type [BV] to use for each map value
 */
@Suppress("ktlint:standard:function-naming")
public fun <AK, AV, BK, BV> MapMappingMonoConverter(
    keyDelegate: MonoConverter<AK, BK>,
    valueDelegate: MonoConverter<AV, BV>,
): MonoConverter<Map<AK, AV>, Map<BK, BV>> = MonoConverter { from ->
    from.map { (key, value) ->
        keyDelegate.convert(key) to valueDelegate.convert(value)
    }.toMap()
}

/**
 * Creates a map-mapping [Converter] which performs two-way conversions between values of type `Map<LK, LV>` and values
 * of type `Map<RK, RV>`
 * @param LK The **left** key type
 * @param LV The **left** value type
 * @param RK The **right** key type
 * @param RV The **right** value type
 * @param delegate A [Converter] between values of type `Pair<AK, AV>` and type `Pair<BK, BV>` to use for each map entry
 */
@Suppress("ktlint:standard:function-naming")
public fun <LK, LV, RK, RV> MapMappingConverter(
    delegate: Converter<Pair<LK, LV>, Pair<RK, RV>>,
): Converter<Map<LK, LV>, Map<RK, RV>> =
    MapMappingMonoConverter(delegate.right) reversedBy MapMappingMonoConverter(delegate.left)

/**
 * Creates a map-mapping [Converter] which performs two-way conversions between values of type `Map<LK, LV>` and values
 * of type `Map<RK, RV>`
 * @param LK The **left** key type
 * @param LV The **left** value type
 * @param RK The **right** key type
 * @param RV The **right** value type
 * @param keyDelegate A [Converter] between values of type [LK] and type [RK] to use for each map key
 * @param valueDelegate A [Converter] between values of type [LV] to type [RV] to use for each map value
 */
@Suppress("ktlint:standard:function-naming")
public fun <LK, LV, RK, RV> MapMappingConverter(
    keyDelegate: Converter<LK, RK>,
    valueDelegate: Converter<LV, RV>,
): Converter<Map<LK, LV>, Map<RK, RV>> =
    MapMappingMonoConverter(keyDelegate.right, valueDelegate.right) reversedBy
        MapMappingMonoConverter(keyDelegate.left, valueDelegate.left)
