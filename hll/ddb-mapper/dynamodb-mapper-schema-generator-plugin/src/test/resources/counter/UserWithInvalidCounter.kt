/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.counter

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbCounter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbItem
public data class UserWithInvalidCounter(
    @DynamoDbPartitionKey var id: Int,
    var name: String,

    @DynamoDbCounter
    var accessCount: String, // Invalid: String type
)
