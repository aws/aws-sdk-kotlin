/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbCounter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbTtlSeconds

// A @DynamoDbItem carrying TTL and counter annotations that is also nested inside another @DynamoDbItem. When nested,
// only its ItemConverter participates; its schema attributes (TtlFields/CounterFields) are NOT propagated to the
// parent, so the TTL and counter interceptors -- which read the operation/parent schema -- never act on these fields.
@DynamoDbItem
public data class Session(
    @DynamoDbPartitionKey var id: Int,
    @DynamoDbTtlSeconds(3600) var expiresAt: Long,
    @DynamoDbCounter var accessCount: Long,
)

@DynamoDbItem
public data class Account(
    @DynamoDbPartitionKey var accountId: String,
    var session: Session,
)
