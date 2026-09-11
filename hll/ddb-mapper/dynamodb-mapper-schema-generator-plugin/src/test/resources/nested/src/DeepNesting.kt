/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbMappable
public data class Leaf(
    var label: String,
    var value: Int,
)

@DynamoDbItem
public data class DeepListHolder(
    @DynamoDbPartitionKey var id: Int,
    var data: List<Map<String, List<Leaf>>>,
)
