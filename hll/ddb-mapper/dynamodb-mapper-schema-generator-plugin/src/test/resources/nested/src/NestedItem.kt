/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbMappable
public data class Address(
    var street: String,
    var city: String,
    var zip: String,
)

@DynamoDbItem
public data class Person(
    @DynamoDbPartitionKey var id: Int,
    var name: String,
    var homeAddress: Address,
    var otherAddresses: List<Address>,
    var addressesByLabel: Map<String, Address>,
    var mailingAddress: Address?,
)
