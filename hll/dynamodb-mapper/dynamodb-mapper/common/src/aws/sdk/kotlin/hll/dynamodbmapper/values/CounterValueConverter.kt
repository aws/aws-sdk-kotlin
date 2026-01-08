/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * A ValueConverter that automatically increments numeric values
 */
public class CounterValueConverter<T : Number>(
    private val baseConverter: ValueConverter<T>,
    private val incrementer: (T) -> T,
) : ValueConverter<T> {

    override val right: MonoConverter<T, AttributeValue> = MonoConverter { value ->
        baseConverter.convertRight(incrementer(value))
    }

    override val left: MonoConverter<AttributeValue, T> = baseConverter.left

    public companion object {
        public val Long: CounterValueConverter<Long> = CounterValueConverter(
            NumberValueConverters.Long,
        ) { it + 1 }

        public val Int: CounterValueConverter<Int> = CounterValueConverter(
            NumberValueConverters.Int,
        ) { it + 1 }
    }
}
