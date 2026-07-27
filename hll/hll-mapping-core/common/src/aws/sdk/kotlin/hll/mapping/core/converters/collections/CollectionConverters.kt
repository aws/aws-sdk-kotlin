/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.collections

import aws.sdk.kotlin.hll.mapping.core.converters.Converter

@Suppress("ktlint:standard:function-naming")
public class SetToListConverter<E> : Converter<Set<E>, List<E>> {
    override fun convertLeft(from: List<E>): Set<E> = from.toSet()
    override fun convertRight(from: Set<E>): List<E> = from.toList()
}
