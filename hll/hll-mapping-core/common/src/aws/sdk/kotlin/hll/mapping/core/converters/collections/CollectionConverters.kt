/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.collections

import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.smithy.kotlin.runtime.ExperimentalApi

@ExperimentalApi
@Suppress("ktlint:standard:function-naming")
public fun <E> SetToListConverter(): Converter<Set<E>, List<E>> = Converter({ it.toList() }, { it.toSet() })
