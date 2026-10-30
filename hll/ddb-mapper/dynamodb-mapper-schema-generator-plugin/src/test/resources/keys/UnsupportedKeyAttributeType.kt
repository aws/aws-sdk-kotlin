/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.keys

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbItem
public data class UnsupportedKeyAttributeType(
    @DynamoDbPartitionKey var id: Boolean, // Invalid: Boolean is not a supported key attribute type
    var name: String,
)
