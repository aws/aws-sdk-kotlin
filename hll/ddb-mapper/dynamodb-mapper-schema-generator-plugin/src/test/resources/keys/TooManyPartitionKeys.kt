/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.keys

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbItem
public data class TooManyPartitionKeys(
    @DynamoDbPartitionKey var a: String,
    @DynamoDbPartitionKey var b: String,
    @DynamoDbPartitionKey var c: String,
    @DynamoDbPartitionKey var d: String,
    @DynamoDbPartitionKey var e: String, // Invalid: at most 4 partition keys
)
