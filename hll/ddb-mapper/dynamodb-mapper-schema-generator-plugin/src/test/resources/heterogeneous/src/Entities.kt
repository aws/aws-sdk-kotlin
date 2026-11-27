/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbCounter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbHeterogeneousAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbHeterogeneousType
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSortKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSubtype
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbTtlSeconds

// An ad-hoc family of unrelated classes (single-table design), with keys, TTL and counter attributes declared on each
// subtype
@DynamoDbItem
@DynamoDbHeterogeneousType(typeAttribute = "entityType")
public object Entities

@DynamoDbSubtype("user", parent = Entities::class)
public data class UserEntity(
    @DynamoDbPartitionKey val pk: String,
    @DynamoDbSortKey val sk: String,
    val name: String,
    @DynamoDbCounter val version: Long,
    @DynamoDbTtlSeconds(3600) val expiresAt: Long,
)

@DynamoDbSubtype("order", parent = Entities::class)
public data class OrderEntity(
    @DynamoDbPartitionKey val pk: String,
    @DynamoDbSortKey val sk: String,
    val total: Long,
    @DynamoDbCounter val version: Long,
    @DynamoDbTtlSeconds(3600) val expiresAt: Long,
)

@DynamoDbMappable
public data class AuditRecord(
    @DynamoDbHeterogeneousAttribute(Entities::class) val entity: Any,
    @DynamoDbHeterogeneousAttribute(Entities::class) val related: List<Any>,
)
