/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.visibility

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbItem
internal data class InternalUser(
    @DynamoDbPartitionKey var id: Int,
    @DynamoDbAttribute("fName") var givenName: String,
    @DynamoDbAttribute("lName") var surname: String,
    var age: Int,
)
