/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbItem("my.custom.item.converter.MyCustomUserConverter")
public data class CustomUser(
    @DynamoDbPartitionKey var id: Int = 1,
    var givenName: String = "Johnny",
    var surname: String = "Appleseed",
    var age: Int = 0,
)
