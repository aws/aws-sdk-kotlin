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
public data class Lists(
    @DynamoDbPartitionKey var id: Int,
    var listBoolean: List<Boolean>,
    var listString: List<String>,
    var listCharArray: List<CharArray>,
    var listChar: List<Char>,
    var listByte: List<Byte>,
    var listByteArray: List<ByteArray>,
    var listShort: List<Short>,
    var listInt: List<Int>,
    var listLong: List<Long>,
    var listDouble: List<Double>,
    var listFloat: List<Float>,
    var listUByte: List<UByte>,
    var listUInt: List<UInt>,
    var listUShort: List<UShort>,
    var listULong: List<ULong>,
    var listEnum: List<EnumAnimals>,
    var nullableList: List<String>?,
    var listNullableElement: List<String?>,
    var nullableListNullableElement: List<String?>?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Lists) return false

        if (id != other.id) return false
        if (listBoolean != other.listBoolean) return false
        if (listString != other.listString) return false
        if (listCharArray.size != other.listCharArray.size) return false
        if (!listCharArray.zip(other.listCharArray).all { (a, b) -> a.contentEquals(b) }) return false
        if (listChar != other.listChar) return false
        if (listByte != other.listByte) return false
        if (listByteArray.size != other.listByteArray.size) return false
        if (!listByteArray.zip(other.listByteArray).all { (a, b) -> a.contentEquals(b) }) return false
        if (listShort != other.listShort) return false
        if (listInt != other.listInt) return false
        if (listLong != other.listLong) return false
        if (listDouble != other.listDouble) return false
        if (listFloat != other.listFloat) return false
        if (listUByte != other.listUByte) return false
        if (listUInt != other.listUInt) return false
        if (listUShort != other.listUShort) return false
        if (listULong != other.listULong) return false
        if (listEnum != other.listEnum) return false
        if (nullableList != other.nullableList) return false
        if (listNullableElement != other.listNullableElement) return false
        if (nullableListNullableElement != other.nullableListNullableElement) return false

        return true
    }
}
