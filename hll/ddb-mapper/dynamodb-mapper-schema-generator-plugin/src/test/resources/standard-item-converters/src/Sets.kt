/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

@DynamoDbItem
public data class Sets(
    @DynamoDbPartitionKey var id: Int,

    /**
     * Sets
     */
    var setString: Set<String>,
    var setCharArray: Set<CharArray>,
    var setChar: Set<Char>,
    var setByte: Set<Byte>,
    var setDouble: Set<Double>,
    var setFloat: Set<Float>,

    var setInt: Set<Int>,
    var setLong: Set<Long>,
    var setShort: Set<Short>,
    var setUByte: Set<UByte>,
    var setUInt: Set<UInt>,
    var setULong: Set<ULong>,
    var setUShort: Set<UShort>,
    var nullableSet: Set<String>?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Sets) return false

        if (id != other.id) return false
        if (setString != other.setString) return false
        if (setCharArray.size != other.setCharArray.size) return false
        if (!setCharArray.all { thisArray -> other.setCharArray.any { otherArray -> thisArray.contentEquals(otherArray) } }) return false
        if (setChar != other.setChar) return false
        if (setByte != other.setByte) return false
        if (setDouble != other.setDouble) return false
        if (setFloat != other.setFloat) return false
        if (setInt != other.setInt) return false
        if (setLong != other.setLong) return false
        if (setShort != other.setShort) return false
        if (setUByte != other.setUByte) return false
        if (setUInt != other.setUInt) return false
        if (setULong != other.setULong) return false
        if (setUShort != other.setUShort) return false

        if (nullableSet != other.nullableSet) return false

        return true
    }
}
