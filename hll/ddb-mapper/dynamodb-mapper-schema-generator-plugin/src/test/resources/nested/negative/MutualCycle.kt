/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbItem
public data class Node(
    @DynamoDbPartitionKey var id: Int,
    var edge: Edge,
)

@DynamoDbMappable
public data class Edge(
    var label: String,
    var target: Node,
)
