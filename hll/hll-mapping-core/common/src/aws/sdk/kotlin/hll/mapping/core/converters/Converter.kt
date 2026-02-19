/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters

import aws.sdk.kotlin.hll.mapping.core.converters.internal.ConverterImpl

/**
 * A type for two-way conversion between a **left** type [L] and **right** type [R]. As a general convention, **left**
 * is for types which are closer to user code and business logic. Conversely, **right** is for types which are farther
 * from user code and business logic. Alternatively, one may think of **left** types as "my" types and **right** types
 * as "their" types. Note that these distinctions are often subjective so consult documentation in the library which
 * uses this converter for more details and context.
 * @param L The **left** type
 * @param R The **right** type
 */
public interface Converter<L, R> {
    public companion object {
        public fun <T> identity(): Converter<T, T> = Converter({ it }, { it })
    }

    /**
     * Gets a [MonoConverter] that converts from **left** type [L] to **right** type [R]
     */
    public val right: MonoConverter<L, R>

    /**
     * Gets a [MonoConverter] that converts from **right** type [R] to **left** type [L]
     */
    public val left: MonoConverter<R, L>

    /**
     * Converts a **left** value into a **right** value
     */
    public fun convertRight(from: L): R = right.convert(from)

    /**
     * Converts a **right** value into a **left** value
     */
    public fun convertLeft(from: R): L = left.convert(from)
}

/**
 * Creates a new two-way converter from symmetrical one-way converters
 * @param L The type being converted from
 * @param R The type being converted to
 * @param right A converter instance for converting one-way from [L] to [R]
 * @param left A converter instance for converting one-way from [R] to [L]
 */
public fun <L, R> Converter(right: MonoConverter<L, R>, left: MonoConverter<R, L>): Converter<L, R> = ConverterImpl(right, left)

/**
 * Chains this converter with a subsequent converter, yielding a new converter which performs two-stage transformations.
 * @param L The **left** type of this converter
 * @param M The **middle** type, which is the **right** type of this converter and the **left** type of the next
 * converter
 * @param R The **right** type of the next converter
 * @param next The subsequent converter to chain
 */
public operator fun <L, M, R> Converter<L, M>.plus(next: Converter<M, R>): Converter<L, R> = Converter(this.right + next.right, next.left + this.left)
