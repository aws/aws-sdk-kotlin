/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.collections

import aws.sdk.kotlin.hll.mapping.core.converters.Converter

/**
 * A set-mapping [Converter] which performs two-way conversions between values of type `Set<L>` and values of type
 * `Set<R>`
 * @param L The **left** element type
 * @param R The **right** element type
 */
public class SetMappingConverter<L, R>(private val delegate: Converter<L, R>): Converter<Set<L>, Set<R>> {
    override fun convertLeft(from: Set<R>): Set<L> = from.map { delegate.convertLeft(it) }.toSet()
    override fun convertRight(from: Set<L>): Set<R> = from.map { delegate.convertRight(it) }.toSet()
}
