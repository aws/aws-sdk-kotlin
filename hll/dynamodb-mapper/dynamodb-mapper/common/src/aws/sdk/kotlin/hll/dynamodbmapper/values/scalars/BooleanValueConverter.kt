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
public val BooleanValueConverter: ValueConverter<Boolean> = Converter(AttributeValue::Bool, AttributeValue::asBool)

/**
 * Converts between [Boolean] and [String]
 */
public val BooleanToStringConverter: Converter<Boolean, String> = Converter({ it.toString() }, { it.toBoolean() })
