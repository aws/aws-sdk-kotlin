/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

enum class EnumAnimals {
    CAT,
    DOG,
    SHEEP,
}

@DynamoDbItem
public data class Maps(
    @DynamoDbPartitionKey var id: Int,
    var mapStringString: Map<String, String>,
    var mapStringInt: Map<String, Int>,
    var mapIntString: Map<Int, String>,
    var mapLongInt: Map<Long, Int>,
    var mapStringBoolean: Map<String, Boolean>,
    var mapStringListString: Map<String, List<String>>,
    var mapStringListMapStringString: Map<String, List<Map<String, String>>>,
    var mapEnum: Map<String, EnumAnimals>,
    var nullableMap: Map<String, String>?,
    var mapNullableValue: Map<String, String?>,
    var nullableMapNullableValue: Map<String, String?>?,
)
