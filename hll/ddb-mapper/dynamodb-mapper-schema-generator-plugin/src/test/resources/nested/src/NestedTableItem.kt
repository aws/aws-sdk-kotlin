/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

// A @DynamoDbItem (has its own primary key) that is also nested inside another @DynamoDbItem. Its key field is
// serialized as an ordinary attribute (respecting @DynamoDbAttribute renames) within the nested map; its key-ness is
// only relevant when used as a standalone table item.
@DynamoDbItem
public data class Manufacturer(
    @DynamoDbPartitionKey @DynamoDbAttribute("mfrId") var id: Int,
    var name: String,
)

@DynamoDbItem
public data class Product(
    @DynamoDbPartitionKey var sku: String,
    var manufacturer: Manufacturer,
)
