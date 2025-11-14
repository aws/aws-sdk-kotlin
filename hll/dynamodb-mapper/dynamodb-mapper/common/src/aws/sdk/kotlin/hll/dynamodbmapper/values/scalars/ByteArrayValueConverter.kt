/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Converts between [ByteArray] and
 * [DynamoDB `B` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Binary)
 */
@ExperimentalApi
public val ByteArrayValueConverter: ValueConverter<ByteArray> = Converter(AttributeValue::B, AttributeValue::asB)
