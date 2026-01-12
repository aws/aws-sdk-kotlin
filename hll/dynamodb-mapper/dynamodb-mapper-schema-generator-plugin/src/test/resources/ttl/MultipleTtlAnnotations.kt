/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.ttl

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbTtlSeconds

@DynamoDbItem
public data class MultipleTtlAnnotations(
    @DynamoDbPartitionKey var id: Int,
    var givenName: String,
    var surname: String,
    var age: Int,

    @DynamoDbTtlSeconds(3600)
    var expiresAt: Long,

    @DynamoDbTtlSeconds(7200)
    var actuallyExpiresAt: Long,
)
