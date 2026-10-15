/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbMappable
public data class NestedWithKey(
    @DynamoDbPartitionKey var id: Int,
    var name: String,
)
