/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbAttributeConverter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter

@DynamoDbItem
public data class User(
    @DynamoDbPartitionKey var id: Int,
    @DynamoDbAttributeConverter(StringValueConverter::class) var givenName: String,
    var surname: String,
    @DynamoDbAttributeConverter(NumberValueConverters.Int::class) var age: Int,
)
