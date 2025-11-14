/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.plus
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Instantiates a new [ValueConverter] for enums of type [E]
 * @param E The [Enum] type for which to create a [ValueConverter]
 */
@ExperimentalApi
public inline fun <reified E : Enum<E>> EnumValueConverter(): ValueConverter<E> =
    Converter<E, String>({ it.name }, { enumValueOf(it) }) + StringValueConverter
