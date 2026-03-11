/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters

/**
 * A type for one-way conversion from values of type [A] to values of type [B]. A symmetrical pair of one-way converters
 * form the two halves of a [Converter] which provides two-way conversion.
 * @param A the type to convert from
 * @param B the type to convert to
 */
public fun interface MonoConverter<A, B> {
    public companion object {
        public fun <T> identity(): MonoConverter<T, T> = MonoConverter { it }
    }

    /**
     * Convert a value from type [A] to type [B]
     */
    public fun convert(from: A): B
}

/**
 * Chains this converter with a subsequent converter, yielding a new converter which performs two-stage transformations.
 * @param A The "from" type of this converter
 * @param B The middle type, which is the "to" type of this converter and the "from" type of the next converter
 * @param C The "to" type of the next converter
 * @param next The subsequent converter to chain
 */
public operator fun <A, B, C> MonoConverter<A, B>.plus(next: MonoConverter<B, C>): MonoConverter<A, C> = MonoConverter { next.convert(this.convert(it)) }

/**
 * Pairs this one-way converter with a symmetrical one-way converter to form a two-way [Converter]
 * @param A The "from" type of this converter and the "to" type of the other converter
 * @param B The "from" type of the other converter and the "to" type of this converter
 * @param other The other converter to pair with
 */
public infix fun <A, B> MonoConverter<A, B>.reversedBy(other: MonoConverter<B, A>): Converter<A, B> = Converter(this, other)
