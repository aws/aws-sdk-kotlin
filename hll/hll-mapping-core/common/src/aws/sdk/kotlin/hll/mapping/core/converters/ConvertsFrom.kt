/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters

import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Models one-way conversion from a type [T] to a type [F]. This type is similar to [ConvertsTo] but models conversion
 * in the opposite direction.
 * @param F The type being converted to
 * @param T The type being converted from
 */
@ExperimentalApi
public fun interface ConvertsFrom<F, T> {
    /**
     * Converts a single value from type [T] to type [F]
     * @param to The value to convert
     */
    public fun convertFrom(to: T): F
}

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
public fun <F, F2, T> ConvertsFrom<F, T>.andThenConvertsFrom(converter: ConvertsFrom<F2, F>): ConvertsFrom<F2, T> = ConvertsFrom { to: T -> converter.convertFrom(this.convertFrom(to)) }

/**
 * Adds validation before a conversion by running [validate] on [T] values before converting them to type [F].
 * Validators are expected to throw an exception if the expected condition is not met.
 * @param F The type being converted to
 * @param T The type being converted from
 * @param validate A function which accepts a [T] value and throws an exception if the expected condition is not met
 */
@ExperimentalApi
public fun <F, T> ConvertsFrom<F, T>.firstValidatingTo(validate: (T) -> Unit): ConvertsFrom<F, T> = ConvertsFrom { to: T ->
    validate(to)
    this.convertFrom(to)
}
