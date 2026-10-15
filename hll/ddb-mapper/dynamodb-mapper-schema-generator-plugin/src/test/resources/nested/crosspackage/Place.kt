/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import org.example.geo.Coordinates

@DynamoDbItem
public data class Place(
    @DynamoDbPartitionKey var id: Int,
    var name: String,
    var location: Coordinates,
)
