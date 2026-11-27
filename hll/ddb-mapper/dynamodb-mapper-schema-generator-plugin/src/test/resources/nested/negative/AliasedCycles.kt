/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

typealias Tree = TreeNode
typealias FirstAlias = First
typealias SecondAlias = Second

@DynamoDbItem
data class TreeNode(
    @DynamoDbPartitionKey val id: String,
    val children: List<Tree>,
)

@DynamoDbItem
data class First(
    @DynamoDbPartitionKey val id: String,
    val second: SecondAlias?,
)

@DynamoDbItem
data class Second(
    @DynamoDbPartitionKey val id: String,
    val first: FirstAlias?,
)
