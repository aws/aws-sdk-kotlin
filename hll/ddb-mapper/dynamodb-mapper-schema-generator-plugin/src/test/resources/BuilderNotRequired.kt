/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbItem
public data class BuilderNotRequired(
    @DynamoDbPartitionKey var id: Int = 1,
    @DynamoDbAttribute("fName") var givenName: String = "Johnny",
    @DynamoDbAttribute("lName") var surname: String = "Appleseed",
    var age: Int = 0,
)
