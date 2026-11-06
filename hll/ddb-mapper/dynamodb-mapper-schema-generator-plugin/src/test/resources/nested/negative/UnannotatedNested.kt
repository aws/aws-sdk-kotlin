/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

public data class PlainThing(
    var x: Int,
    var y: Int,
)

@DynamoDbItem
public data class HasPlain(
    @DynamoDbPartitionKey var id: Int,
    var thing: PlainThing,
)
