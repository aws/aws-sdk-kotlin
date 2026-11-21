/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.keys

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbItem
public data class UnsupportedMapKey(
    @DynamoDbPartitionKey var id: String,
    var tags: Map<List<String>, String>, // Invalid: list-typed map keys are unsupported
)
