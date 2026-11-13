/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.keys

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbCounter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbTtlSeconds

// Invalid in three independent ways; every problem must be reported, not just the first
@DynamoDbItem
public data class MultipleProblems(
    var id: Int, // no @DynamoDbPartitionKey

    @DynamoDbTtlSeconds(-5)
    var expiresAt: Long,

    @DynamoDbCounter
    var accessCount: String,
)
