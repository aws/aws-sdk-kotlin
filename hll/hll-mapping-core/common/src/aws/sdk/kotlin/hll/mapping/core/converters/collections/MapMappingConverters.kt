/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.collections

import aws.sdk.kotlin.hll.mapping.core.converters.Converter

/**
 * A map-mapping [Converter] which performs two-way conversions between values of type `Map<LK, LV>` and
 * values of type `Map<RK, RV>`
 * @param LK The **left** key type
 * @param LV The **left** value type
 * @param RK The **right** key type
 * @param RV The **right** value type
 */
public class MapMappingConverter<LK, LV, RK, RV> private constructor(
    private val convertPairRight: (Pair<LK, LV>) -> Pair<RK, RV>,
    private val convertPairLeft: (Pair<RK, RV>) -> Pair<LK, LV>,
) : Converter<Map<LK, LV>, Map<RK, RV>> {
    /**
     * Create a map-mapping [Converter] which performs two-way conversions between values of type `Map<LK, LV>` and
     * values of type `Map<RK, RV>`
     * @param LK The **left** key type
     * @param LV The **left** value type
     * @param RK The **right** key type
     * @param RV The **right** value type
     * @param delegate A [Converter] between values of type `Pair<AK, AV>` and type `Pair<BK, BV>` to use for each map
     * entry
     */
    public constructor(delegate: Converter<Pair<LK, LV>, Pair<RK, RV>>) : this(
        { entry -> delegate.convertRight(entry) },
        { entry -> delegate.convertLeft(entry) },
    )

    /**
     * Create a map-mapping [Converter] which performs two-way conversions between values of type `Map<LK, LV>` and
     * values of type `Map<RK, RV>`
     * @param LK The **left** key type
     * @param LV The **left** value type
     * @param RK The **right** key type
     * @param RV The **right** value type
     * @param keyDelegate A [Converter] between values of type [LK] and type [RK] to use for each map key
     * @param valueDelegate A [Converter] between values of type [LV] to type [RV] to use for each map value
     */
    public constructor(keyDelegate: Converter<LK, RK>, valueDelegate: Converter<LV, RV>) : this(
        { (key, value) -> keyDelegate.convertRight(key) to valueDelegate.convertRight(value) },
        { (key, value) -> keyDelegate.convertLeft(key) to valueDelegate.convertLeft(value) },
    )

    override fun convertLeft(from: Map<RK, RV>): Map<LK, LV> = from.map { convertPairLeft(it.toPair()) }.toMap()
    override fun convertRight(from: Map<LK, LV>): Map<RK, RV> = from.map { convertPairRight(it.toPair()) }.toMap()
}
