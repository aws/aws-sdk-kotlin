/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.keys

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem

@DynamoDbItem
public data class NoPartitionKey(
    var id: Int, // Invalid: no @DynamoDbPartitionKey
    var name: String,
)
