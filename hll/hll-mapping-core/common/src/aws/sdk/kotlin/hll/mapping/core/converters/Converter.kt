/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters

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
        public fun <T> identity(): Converter<T, T> = ConverterImpl({ it }, { it })
    }

    /**
     * Converts a **left** value into a **right** value
     */
    public fun convertRight(from: L): R

    /**
     * Converts a **right** value into a **left** value
     */
    public fun convertLeft(from: R): L
}
