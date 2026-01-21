/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.collections

import aws.sdk.kotlin.hll.mapping.core.converters.*
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Namespace for containing various conversion utilities dealing with [Set] mapping
 */
@ExperimentalApi
public object SetMappingConverters {
    /**
     * Creates a one-way converter for transforming [Set] with elements of type [T] to [Set] with elements of type [F]
     * @param F The type being converted to
     * @param T The type being converted from
     * @param elementConverter A one-way converter of [T] values to [F] values
     */
    public fun <F, T> of(elementConverter: ConvertsFrom<F, T>): ConvertsFrom<Set<F>, Set<T>> = ConvertsFrom { to: Set<T> -> to.map(elementConverter::convertFrom).toSet() }

    /**
     * Chains this set converter with an element converter, yielding a new converter which performs a two-stage mapping
     * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical steps
     * in their actual implementation.)
     * @param F The source element type of this converter and the target type of the given [elementConverter]
     * @param F2 The source type of the given [elementConverter]
     * @param T The target type of this converter
     * @param elementConverter The element converter to chain together with this set converter. Note that the target type
     * of the given [elementConverter] must be the same as the source element type of this converter.
     */
    public fun <F, F2, T> ConvertsFrom<Set<F>, T>.mapConvertsFrom(
        elementConverter: ConvertsFrom<F2, F>,
    ): ConvertsFrom<Set<F2>, T> = this.andThenConvertsFrom(of(elementConverter))

    /**
     * Creates a one-way converter for transforming [Set] with elements of type [F] to [Set] with elements of type [T]
     * @param F The type being converted from
     * @param T The type being converted to
     * @param elementConverter A one-way converter of [F] values to [T] values
     */
    public fun <F, T> of(elementConverter: ConvertsTo<F, T>): ConvertsTo<Set<F>, Set<T>> = ConvertsTo { from: Set<F> -> from.map(elementConverter::convertTo).toSet() }

    /**
     * Chains this set converter with an element converter, yielding a new converter which performs a two-stage mapping
     * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical steps
     * in their actual implementation.)
     * @param F The source type of this converter
     * @param T The target element type of this converter and the source type of the given [elementConverter]
     * @param T2 The target type of the given [elementConverter]
     * @param elementConverter The element converter to chain together with this set converter. Note that the source type
     * of the given [elementConverter] must be the same as the target element type of this converter.
     */
    public fun <F, T, T2> ConvertsTo<F, Set<T>>.mapConvertsTo(
        elementConverter: ConvertsTo<T, T2>,
    ): ConvertsTo<F, Set<T2>> = this.andThenConvertsTo(of(elementConverter))

    /**
     * Creates a two-way converter for transforming between a [Set] with elements of type [F] and a [Set] with elements
     * of type [T]
     * @param F The type being converted from
     * @param T The type being converted to
     * @param elementConverter A [Converter] for transforming between values of type [F] and [T]
     */
    public fun <F, T> of(elementConverter: Converter<F, T>): Converter<Set<F>, Set<T>> = Converter(of(elementConverter as ConvertsTo<F, T>), of(elementConverter as ConvertsFrom<F, T>))
}

/**
 * Chains this set converter with an element converter, yielding a new converter which performs a two-stage mapping
 * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical steps
 * in their actual implementation.)
 * @param F The source element type of this converter and the target type of the given [elementConverter]
 * @param F2 The source type of the given [elementConverter]
 * @param T The target type of this converter
 * @param elementConverter The element converter to chain together with this set converter. Note that the target type
 * of the given [elementConverter] must be the same as the source element type of this converter.
 */
@ExperimentalApi
public fun <F, F2, T> Converter<Set<F>, T>.mapFrom(elementConverter: Converter<F2, F>): Converter<Set<F2>, T> = this.andThenFrom(SetMappingConverters.of(elementConverter))

/**
 * Chains this set converter with an element converter, yielding a new converter which performs a two-stage mapping
 * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical steps
 * in their actual implementation.)
 * @param F The source type of this converter
 * @param T The target element type of this converter and the source type of the given [elementConverter]
 * @param T2 The target type of the given [elementConverter]
 * @param elementConverter The element converter to chain together with this set converter. Note that the source type
 * of the given [elementConverter] must be the same as the target element type of this converter.
 */
@ExperimentalApi
public fun <F, T, T2> Converter<F, Set<T>>.mapTo(elementConverter: Converter<T, T2>): Converter<F, Set<T2>> = this.andThenTo(SetMappingConverters.of(elementConverter))
