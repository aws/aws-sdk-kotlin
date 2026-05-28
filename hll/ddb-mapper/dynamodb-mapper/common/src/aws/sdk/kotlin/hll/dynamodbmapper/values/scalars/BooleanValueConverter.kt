/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between [Boolean] and
 * [DynamoDB `BOOL` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Boolean)
 */
public class BooleanValueConverter : ValueConverter<Boolean> by BooleanValueConverter {
    public companion object : ValueConverter<Boolean> {
        override fun convertLeft(from: AttributeValue): Boolean = from.asBool()
        override fun convertRight(from: Boolean): AttributeValue = AttributeValue.Bool(from)
    }
}

/**
 * Converts between [Boolean] and [String]
 */
public class BooleanToStringConverter : Converter<Boolean, String> by BooleanToStringConverter {
    public companion object : Converter<Boolean, String> {
        override fun convertLeft(from: String): Boolean = from.toBoolean()
        override fun convertRight(from: Boolean): String = from.toString()
    }
}
