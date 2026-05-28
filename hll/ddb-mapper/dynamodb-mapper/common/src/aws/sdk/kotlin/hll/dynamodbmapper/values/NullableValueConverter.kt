/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.hll.dynamodbmapper.util.NULL_ATTR
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between `null` values and
 * [DynamoDB `NULL` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Null).
 * This converter is not generally useful on its own and is typically combined with a type-specific non-nullable
 * delegate in a [NullableValueConverter].
 */
public class NullValueConverter : ValueConverter<Nothing?> by NullValueConverter {
    public companion object : ValueConverter<Nothing?> {
        override fun convertLeft(from: AttributeValue): Nothing? {
            require(from is AttributeValue.Null)
            return null
        }
        override fun convertRight(from: Nothing?): AttributeValue = NULL_ATTR
    }
}

/**
 * Creates a converter between potentially-`null` values and
 * [DynamoDB `NULL` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Null).
 * @param V The non-nullable type
 * @param delegate The delegate converter for non-null values
 * @param nullValueConverter A [Converter] for `null` values. The default is [NullValueConverter].
 */
public class NullableValueConverter<V : Any>(
    private val delegate: ValueConverter<V>,
    private val nullValueConverter: ValueConverter<Nothing?> = NullValueConverter,
) : ValueConverter<V?> {
    override fun convertLeft(from: AttributeValue): V? = when (from) {
        is AttributeValue.Null -> nullValueConverter.convertLeft(from)
        else -> delegate.convertLeft(from)
    }

    override fun convertRight(from: V?): AttributeValue = when (from) {
        null -> nullValueConverter.convertRight(from)
        else -> delegate.convertRight(from)
    }
}

public fun <V : Any> ValueConverter<V>.asNullable(
    nullValueConverter: ValueConverter<Nothing?> = NullValueConverter,
): ValueConverter<V?> = NullableValueConverter(this, nullValueConverter)
