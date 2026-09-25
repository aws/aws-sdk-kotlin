/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.smithy.kotlin.runtime.content.BigDecimal
import aws.smithy.kotlin.runtime.content.BigInteger

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
    var mapStringBigDecimal: Map<String, BigDecimal>,
    var mapStringBigInteger: Map<String, BigInteger>,
    var mapStringJvmBigDecimal: Map<String, java.math.BigDecimal>,
    var mapStringJvmBigInteger: Map<String, java.math.BigInteger>,
    var mapBigDecimalString: Map<BigDecimal, String>,
    var mapBigIntegerString: Map<BigInteger, String>,
    var mapJvmBigDecimalString: Map<java.math.BigDecimal, String>,
    var mapJvmBigIntegerString: Map<java.math.BigInteger, String>,
    var mapStringListString: Map<String, List<String>>,
    var mapStringListMapStringString: Map<String, List<Map<String, String>>>,
    var mapEnum: Map<String, EnumAnimals>,
    var mapEnumKey: Map<EnumAnimals, String>,
    var nullableMap: Map<String, String>?,
    var mapNullableValue: Map<String, String?>,
    var nullableMapNullableValue: Map<String, String?>?,
)
