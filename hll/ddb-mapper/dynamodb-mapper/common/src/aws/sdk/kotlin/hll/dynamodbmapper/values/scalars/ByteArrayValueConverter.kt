/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between [ByteArray] and
 * [DynamoDB `B` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Binary)
 */
public object ByteArrayValueConverter : ValueConverter<ByteArray> {
    override fun convertLeft(from: AttributeValue): ByteArray = from.asB()
    override fun convertRight(from: ByteArray): AttributeValue = AttributeValue.B(from)
}
