/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters

import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Models one-way conversion from a type [F] to a type [T]
 * @param F The type being converted from
 * @param T The type being converted to
 */
@ExperimentalApi
public fun interface ConvertsTo<F, T> {
    /**
     * Converts a single value from type [F] to type [T]
     * @param from The value to convert
     */
    public fun convertTo(from: F): T
}

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
public fun <F, T, T2> ConvertsTo<F, T>.andThenConvertsTo(converter: ConvertsTo<T, T2>): ConvertsTo<F, T2> = ConvertsTo { from: F -> converter.convertTo(this.convertTo(from)) }

/**
 * Adds validation before a conversion by running [validate] on [F] values before converting them to type [T].
 * Validators are expected to throw an exception if the expected condition is not met.
 * @param F The type being converted from
 * @param T The type being converted to
 * @param validate A function which accepts an [F] value and throws an exception if the expected condition is not met
 */
@ExperimentalApi
public fun <F, T> ConvertsTo<F, T>.firstValidatingFrom(validate: (F) -> Unit): ConvertsTo<F, T> = ConvertsTo { from: F ->
    validate(from)
    this.convertTo(from)
}
