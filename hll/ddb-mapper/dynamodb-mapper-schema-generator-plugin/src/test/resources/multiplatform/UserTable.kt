/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.model.Table
import org.example.dynamodbmapper.generatedschemas.UserSchema
import org.example.dynamodbmapper.generatedschemas.getUserTable

// Common code referencing the generated schema, so every target's compilation depends on it being visible
public val userSchema: UserSchema = UserSchema

public fun DynamoDbMapper.users(): Table.PartitionKey<User, KeyType.Key1<Int>> = getUserTable("users")
