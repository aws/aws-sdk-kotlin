/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.collections

import aws.sdk.kotlin.hll.mapping.core.converters.*
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Namespace for containing various conversion utilities dealing with [List] mapping
 */
@ExperimentalApi
public object ListMappingConverters {
    /**
     * Creates a one-way converter for transforming [List] with elements of type [T] to [List] with elements of type [F]
     * @param F The type being converted to
     * @param T The type being converted from
     * @param elementConverter A one-way converter of [T] elements to [F] elements
     */
    public fun <F, T> of(elementConverter: ConvertsFrom<F, T>): ConvertsFrom<List<F>, List<T>> = ConvertsFrom { to: List<T> -> to.map(elementConverter::convertFrom) }

    /**
     * Chains this list converter with an element converter, yielding a new converter which performs a two-stage mapping
     * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical
     * steps in their actual implementation.)
     * @param F The source element type of this converter and the target type of the given [elementConverter]
     * @param F2 The source type of the given [elementConverter]
     * @param T The target type of this converter
     * @param elementConverter The element converter to chain together with this list converter. Note that the target
     * type of the given [elementConverter] must be the same as the source element type of this converter.
     */
    public fun <F, F2, T> ConvertsFrom<List<F>, T>.mapConvertsFrom(
        elementConverter: ConvertsFrom<F2, F>,
    ): ConvertsFrom<List<F2>, T> = this.andThenConvertsFrom(of(elementConverter))

    /**
     * Creates a one-way converter for transforming [List] with elements of type [F] to [List] with elements of type [T]
     * @param F The type being converted from
     * @param T The type being converted to
     * @param elementConverter A one-way converter of [F] elements to [T] elements
     */
    public fun <F, T> of(elementConverter: ConvertsTo<F, T>): ConvertsTo<List<F>, List<T>> = ConvertsTo { from: List<F> -> from.map(elementConverter::convertTo) }

    /**
     * Chains this list converter with an element converter, yielding a new converter which performs a two-stage mapping
     * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical
     * steps in their actual implementation.)
     * @param F The source type of this converter
     * @param T The target element type of this converter and the source type of the given [elementConverter]
     * @param T2 The target type of the given [elementConverter]
     * @param elementConverter The element converter to chain together with this list converter. Note that the source
     * type of the given [elementConverter] must be the same as the target element type of this converter.
     */
    public fun <F, T, T2> ConvertsTo<F, List<T>>.mapConvertsTo(
        elementConverter: ConvertsTo<T, T2>,
    ): ConvertsTo<F, List<T2>> = this.andThenConvertsTo(of(elementConverter))

    /**
     * Creates a two-way converter for transforming between a [List] with elements of type [F] and a [List] with
     * elements of type [T]
     * @param F The type being converted from
     * @param T The type being converted to
     * @param elementConverter A [Converter] for transforming between elements of type [F] and [T]
     */
    public fun <F, T> of(elementConverter: Converter<F, T>): Converter<List<F>, List<T>> = Converter(of(elementConverter as ConvertsTo<F, T>), of(elementConverter as ConvertsFrom<F, T>))
}

/**
 * Chains this list converter with an element converter, yielding a new converter which performs a two-stage mapping
 * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical steps
 * in their actual implementation.)
 * @param F The source element type of this converter and the target type of the given [elementConverter]
 * @param F2 The source type of the given [elementConverter]
 * @param T The target type of this converter
 * @param elementConverter The element converter to chain together with this list converter. Note that the target type
 * of the given [elementConverter] must be the same as the source element type of this converter.
 */
@ExperimentalApi
public fun <F, F2, T> Converter<List<F>, T>.mapFrom(elementConverter: Converter<F2, F>): Converter<List<F2>, T> = this.andThenFrom(ListMappingConverters.of(elementConverter))

/**
 * Chains this list converter with an element converter, yielding a new converter which performs a two-stage mapping
 * conversion. (Note that these two "stages" are conceptual. Each of these stages may consist of multiple logical steps
 * in their actual implementation.)
 * @param F The source type of this converter
 * @param T The target element type of this converter and the source type of the given [elementConverter]
 * @param T2 The target type of the given [elementConverter]
 * @param elementConverter The element converter to chain together with this list converter. Note that the source type
 * of the given [elementConverter] must be the same as the target element type of this converter.
 */
@ExperimentalApi
public fun <F, T, T2> Converter<F, List<T>>.mapTo(elementConverter: Converter<T, T2>): Converter<F, List<T2>> = this.andThenTo(ListMappingConverters.of(elementConverter))
