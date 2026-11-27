/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.visibility

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

// UserValueConverter would be generated for both, but User's value converter is only generated if something references
// User
@DynamoDbItem
public data class User(@DynamoDbPartitionKey var id: Int = 0, var name: String = "")

@DynamoDbItem
public data class UserValue(@DynamoDbPartitionKey var id: Int = 0, var amount: Int = 0)
