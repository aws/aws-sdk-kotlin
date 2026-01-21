/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.collections

import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Namespace for containing various conversion utilities dealing with mapping between collection types (e.g., [Set] to
 * [List])
 */
@ExperimentalApi
public object CollectionTypeConverters {
    /**
     * Creates a [Converter] which transforms between [Set] and [List] instances (both of some type [T])
     * @param T The type of elements in the [Set]/[List]
     */
    @Suppress("ktlint:standard:function-naming")
    public inline fun <reified T> SetToListConverter(): Converter<Set<T>, List<T>> = Converter(Set<T>::toList, List<T>::toSet)
}
