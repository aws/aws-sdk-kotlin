/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.counter

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbCounter

@DynamoDbItem
public data class UserWithCounter(
    @DynamoDbPartitionKey var id: Int,
    var name: String,
    
    @DynamoDbCounter
    var accessCount: Long,
    
    @DynamoDbCounter
    var updateCount: Long,
)
