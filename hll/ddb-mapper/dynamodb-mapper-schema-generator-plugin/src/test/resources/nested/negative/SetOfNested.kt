/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbMappable
public data class Tag(
    var key: String,
    var value: String,
)

@DynamoDbItem
public data class Tagged(
    @DynamoDbPartitionKey var id: Int,
    var tags: Set<Tag>,
)
