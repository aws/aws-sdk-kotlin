/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.keys

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSortKey

@DynamoDbItem
public data class TooManySortKeys(
    @DynamoDbPartitionKey var id: String,
    @DynamoDbSortKey var a: String,
    @DynamoDbSortKey var b: String,
    @DynamoDbSortKey var c: String,
    @DynamoDbSortKey var d: String,
    @DynamoDbSortKey var e: String, // Invalid: at most 4 sort keys
)
