/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters

/**
 * A simple [Converter] implementation backed by conversion functions.
 * @param L The **left** type
 * @param R The **right** type
 * @param convertRight A function for converting from [L] to [R]
 * @param convertLeft A function for converting from [R] to [L]
 */
public class ConverterImpl<L, R>(
    private val convertRight: (L) -> R,
    private val convertLeft: (R) -> L,
) : Converter<L, R> {
    override fun convertRight(from: L): R = convertRight.invoke(from)
    override fun convertLeft(from: R): L = convertLeft.invoke(from)
}
