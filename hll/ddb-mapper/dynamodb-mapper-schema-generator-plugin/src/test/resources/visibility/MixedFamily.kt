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
public sealed interface Account {
    @DynamoDbPartitionKey
    public val id: String
}

@DynamoDbSubtype("personal")
public data class PersonalAccount(override val id: String, val owner: String) : Account

@DynamoDbSubtype("service")
internal data class ServiceAccount(override val id: String, val service: String) : Account
