/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.visibility

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbHeterogeneousType
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSubtype

@DynamoDbItem
@DynamoDbHeterogeneousType(typeAttribute = "type")
private object PrivateMarker

@DynamoDbSubtype("a", parent = PrivateMarker::class)
public data class PrivateMarkerA(@DynamoDbPartitionKey val id: String, val a: Int)

@DynamoDbSubtype("b", parent = PrivateMarker::class)
public data class PrivateMarkerB(@DynamoDbPartitionKey val id: String, val b: Int)

@DynamoDbItem
@DynamoDbHeterogeneousType(typeAttribute = "type")
internal object InternalMarker

@DynamoDbSubtype("a", parent = InternalMarker::class)
public data class InternalMarkerA(@DynamoDbPartitionKey val id: String, val a: Int)

@DynamoDbSubtype("b", parent = InternalMarker::class)
public data class InternalMarkerB(@DynamoDbPartitionKey val id: String, val b: Int)
