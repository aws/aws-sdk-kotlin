/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.collections

import aws.sdk.kotlin.hll.mapping.core.converters.Converter

/**
 * Creates a list-mapping [Converter] which performs two-way conversions between values of type `List<L>` and values of
 * type `List<R>`
 * @param L The **left** element type
 * @param R The **right** element type
 */
@Suppress("ktlint:standard:function-naming")
public class ListMappingConverter<L, R>(private val delegate: Converter<L, R>) : Converter<List<L>, List<R>> {
    override fun convertLeft(from: List<R>): List<L> = from.map { delegate.convertLeft(it) }
    override fun convertRight(from: List<L>): List<R> = from.map { delegate.convertRight(it) }
}
