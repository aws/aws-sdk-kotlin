/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.smithy.kotlin.runtime.content.Document
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.time.Instant

enum class EnumAnimals {
    CAT,
    DOG,
    SHEEP,
}

@DynamoDbItem
public data class Primitives(
    @DynamoDbPartitionKey var id: Int,

    /**
     * Enums
     */
    var animal: EnumAnimals,

    /**
     * Primitives
     */
    var boolean: Boolean,
    var string: String,
    var charArray: CharArray,
    var char: Char,
    var byte: Byte,
    var byteArray: ByteArray,
    var short: Short,
    var int: Int,
    var long: Long,
    var double: Double,
    var float: Float,
    var uByte: UByte,
    var uInt: UInt,
    var uShort: UShort,
    var uLong: ULong,

    /**
     * Smithy types
     */
    var instant: Instant,
    var url: Url,
    var document: Document,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Primitives) return false

        if (id != other.id) return false
        if (animal != other.animal) return false
        if (boolean != other.boolean) return false
        if (string != other.string) return false
        if (!charArray.contentEquals(other.charArray)) return false
        if (char != other.char) return false
        if (byte != other.byte) return false
        if (!byteArray.contentEquals(other.byteArray)) return false
        if (short != other.short) return false
        if (int != other.int) return false
        if (long != other.long) return false
        if (double != other.double) return false
        if (float != other.float) return false
        if (uByte != other.uByte) return false
        if (uInt != other.uInt) return false
        if (uShort != other.uShort) return false
        if (uLong != other.uLong) return false
        if (instant.epochSeconds != other.instant.epochSeconds) return false
        if (url != other.url) return false
        if (document != other.document) return false

        return true
    }
}
