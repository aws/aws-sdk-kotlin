/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.hll.dynamodbmapper.util.NULL_ATTR
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.hll.mapping.core.converters.reversedBy
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between `null` values and
 * [DynamoDB `NULL` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Null).
 * This converter is not generally useful on its own and is typically combined with a type-specific non-nullable
 * delegate in a [NullableValueConverter].
 */
public val NullValueConverter: ValueConverter<Nothing?> =
    MonoConverter<Nothing?, AttributeValue> { NULL_ATTR } reversedBy MonoConverter {
        require(it is AttributeValue.Null)
        null
    }

/**
 * Creates a converter between potentially-`null` values and
 * [DynamoDB `NULL` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Null).
 * @param V The non-nullable type
 * @param delegate The delegate converter for non-null values
 * @param nullValueConverter A [Converter] for `null` values. The default is [NullValueConverter].
 */
@Suppress("ktlint:standard:function-naming")
public fun <V : Any> NullableValueConverter(
    delegate: ValueConverter<V>,
    nullValueConverter: ValueConverter<Nothing?> = NullValueConverter,
): ValueConverter<V?> {
    val right = MonoConverter<V?, AttributeValue> { from ->
        when (from) {
            null -> nullValueConverter.convertRight(from)
            else -> delegate.convertRight(from)
        }
    }

    val left = MonoConverter<AttributeValue, V?> { from ->
        when (from) {
            is AttributeValue.Null -> nullValueConverter.convertLeft(from)
            else -> delegate.convertLeft(from)
        }
    }

    return Converter(right, left)
}

public fun <V : Any> ValueConverter<V>.asNullable(
    nullValueConverter: ValueConverter<Nothing?> = NullValueConverter,
): ValueConverter<V?> = NullableValueConverter(this, nullValueConverter)
