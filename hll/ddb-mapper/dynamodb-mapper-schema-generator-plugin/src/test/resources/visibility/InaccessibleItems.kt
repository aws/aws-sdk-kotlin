/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.visibility

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbItem
private data class PrivateUser(
    @DynamoDbPartitionKey var id: Int,
    var name: String,
)

private object PrivateContainer {
    @DynamoDbItem
    data class NestedInPrivate(
        @DynamoDbPartitionKey var id: Int,
        var name: String,
    )
}

public open class ProtectedContainer {
    @DynamoDbItem
    protected data class ProtectedUser(
        @DynamoDbPartitionKey var id: Int,
        var name: String,
    )
}
