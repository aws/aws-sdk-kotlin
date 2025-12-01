/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.collections

import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.hll.mapping.core.converters.reversedBy

/**
 * Creates a list-mapping [MonoConverter] which turns values of type `List<L>` into values of type `List<R>`
 * @param A The element type to convert from
 * @param B The element type to convert to
 * @param delegate A [MonoConverter] from type [A] to type [B] to use for each list element
 */
@Suppress("ktlint:standard:function-naming")
public fun <A, B> ListMappingMonoConverter(delegate: MonoConverter<A, B>): MonoConverter<List<A>, List<B>> =
    MonoConverter { it.map(delegate::convert) }

/**
 * Creates a list-mapping [Converter] which performs two-way conversions between values of type `List<L>` and values of
 * type `List<R>`
 * @param L The **left** element type
 * @param R The **right** element type
 */
@Suppress("ktlint:standard:function-naming")
public fun <L, R> ListMappingConverter(delegate: Converter<L, R>): Converter<List<L>, List<R>> =
    ListMappingMonoConverter(delegate.right) reversedBy ListMappingMonoConverter(delegate.left)
