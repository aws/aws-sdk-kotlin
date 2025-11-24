/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Defines the logic for converting individual values between a high-level type [V] (e.g., [String], [Boolean], [Map])
 * and
 * [DynamoDB data types](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes)
 * @param V The type of high-level values which will be converted to low-level DynamoDB attribute values
 */
public typealias ValueConverter<V> = Converter<V, AttributeValue>
