/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters

import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Models two-way conversion between a type [T] and a type [F]
 * @param F The type being converted from
 * @param T The type being converted to
 */
@ExperimentalApi
public interface Converter<F, T> :
    ConvertsTo<F, T>,
    ConvertsFrom<F, T>

/**
 * Creates a new two-way converter from symmetrical one-way converters
 * @param F The type being converted from
 * @param T The type being converted to
 * @param convertTo A converter instance for converting one-way from [F] to [T]
 * @param convertFrom A converter instance for converting one-way from [T] to [F]
 */
@ExperimentalApi
public fun <F, T> Converter(convertTo: ConvertsTo<F, T>, convertFrom: ConvertsFrom<F, T>): Converter<F, T> = object : Converter<F, T>, ConvertsTo<F, T> by convertTo, ConvertsFrom<F, T> by convertFrom { }

/**
 * Chains this converter with another converter, yielding a new converter which performs a two-stage conversion. (Note
 * that these two "stages" are conceptual. Each of these stages may consist of multiple logical steps in their actual
 * implementation.)
 * @param F The source type of this converter
 * @param T The target type of this converter and the source type of the given [converter]
 * @param T2 The target type of the given [converter]
 * @param converter The converter to chain together with this converter. Note that the source type of the given
 * [converter] must be the same as the target type of this converter.
 */
@ExperimentalApi
public fun <F, T, T2> Converter<F, T>.andThenTo(converter: Converter<T, T2>): Converter<F, T2> = Converter(this.andThenConvertsTo(converter), converter.andThenConvertsFrom(this))

/**
 * Chains this converter with another converter, yielding a new converter which performs a two-stage conversion. (Note
 * that these two "stages" are conceptual. Each of these stages may consist of multiple logical steps in their actual
 * implementation.)
 * @param F The source type of this converter and the target type of the given [converter]
 * @param F2 The source type of the given [converter]
 * @param T The target type of this converter
 * @param converter The converter to chain together with this converter. Note that the target type of the given
 * [converter] must be the same as the source type of this converter.
 */
@ExperimentalApi
public fun <F, F2, T> Converter<F, T>.andThenFrom(converter: Converter<F2, F>): Converter<F2, T> = Converter(converter.andThenConvertsTo(this), this.andThenConvertsFrom(converter))

/**
 * Adds validation before conversions by running [validate] on [F] values before converting them to type [T]. Validators
 * are expected to throw an exception if the expected condition is not met.
 * @param F The type being converted from
 * @param T The type being converted to
 * @param validate A function which accepts an [F] value and throws an exception if the expected condition is not
 * met
 */
@ExperimentalApi
public fun <F, T> Converter<F, T>.validatingFrom(validate: (F) -> Unit): Converter<F, T> = Converter(this.firstValidatingFrom(validate), this)

/**
 * Adds validation before conversions by running [validate] on [T] values before converting them to type [F]. Validators
 * are expected to throw an exception if the expected condition is not met.
 * @param F The type being converted to
 * @param T The type being converted from
 * @param validate A function which accepts a [T] value and throws an exception if the expected condition is not
 * met
 */
@ExperimentalApi
public fun <F, T> Converter<F, T>.validatingTo(validate: (T) -> Unit): Converter<F, T> = Converter(this, this.firstValidatingTo(validate))
