/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.collections

import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.hll.mapping.core.converters.reversedBy
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Creates a set-mapping [MonoConverter] which turns values of type `Set<L>` into values of type `Set<R>`
 * @param A The element type to convert from
 * @param B The element type to convert to
 * @param delegate A [MonoConverter] from type [A] to type [B] to use for each set element
 */
@ExperimentalApi
@Suppress("ktlint:standard:function-naming")
public fun <A, B> SetMappingMonoConverter(delegate: MonoConverter<A, B>): MonoConverter<Set<A>, Set<B>> =
    MonoConverter { it.map(delegate::convert).toSet() }

/**
 * Creates a set-mapping [Converter] which performs two-way conversions between values of type `Set<L>` and values of
 * type `Set<R>`
 * @param L The **left** element type
 * @param R The **right** element type
 */
@ExperimentalApi
@Suppress("ktlint:standard:function-naming")
public fun <L, R> SetMappingConverter(delegate: Converter<L, R>): Converter<Set<L>, Set<R>> =
    SetMappingMonoConverter(delegate.right) reversedBy SetMappingMonoConverter(delegate.left)
