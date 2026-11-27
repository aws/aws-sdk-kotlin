/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbMappable
public data class SetLeaf(
    var label: String,
)

// The innermost Set<SetLeaf> is a set of maps, which DynamoDB cannot represent. Codegen must fail even when the set
// is buried deep within other collections.
@DynamoDbItem
public data class DeepSetHolder(
    @DynamoDbPartitionKey var id: Int,
    var data: List<Map<String, Set<SetLeaf>>>,
)
