/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import org.example.dynamodbmapper.generatedschemas.ListsConverter
import aws.smithy.kotlin.runtime.content.BigDecimal
import aws.smithy.kotlin.runtime.content.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals

public class ListsTest {
    @Test
    fun converterTest() {
        val lists = Lists(
            id = 1,
            listBoolean = listOf(false, true, false, true),
            listString = listOf("foo", "bar", "baz"),
            listCharArray = listOf(charArrayOf('a', 'b'), charArrayOf('c', 'd'), charArrayOf('e', 'f')),
            listChar = listOf('a', 'b', 'c'),
            listByte = listOf(1, 2, 3),
            listByteArray = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6)),
            listShort = listOf(Short.MIN_VALUE, 0, Short.MAX_VALUE),
            listInt = listOf(Int.MIN_VALUE, 0, Int.MAX_VALUE),
            listLong = listOf(Long.MIN_VALUE, 0L, Long.MAX_VALUE),
            listDouble = listOf(Double.MIN_VALUE, 0.0, Double.MAX_VALUE),
            listFloat = listOf(Float.MIN_VALUE, 0f, Float.MAX_VALUE),
            listUByte = listOf(UByte.MIN_VALUE, UByte.MAX_VALUE),
            listUInt = listOf(UInt.MIN_VALUE, UInt.MAX_VALUE),
            listUShort = listOf(UShort.MIN_VALUE, UShort.MAX_VALUE),
            listULong = listOf(ULong.MIN_VALUE, ULong.MAX_VALUE),
            listBigDecimal = listOf(BigDecimal("-1.5"), BigDecimal("3.141592653589793238462643383279502884")),
            listBigInteger = listOf(BigInteger("0"), BigInteger("99999999999999999999999999999999999999")),
            listJvmBigDecimal = listOf(java.math.BigDecimal("-1.5"), java.math.BigDecimal("3.141592653589793238462643383279502884")),
            listJvmBigInteger = listOf(java.math.BigInteger("0"), java.math.BigInteger("99999999999999999999999999999999999999")),
            listEnum = listOf(EnumAnimals.CAT, EnumAnimals.DOG, EnumAnimals.SHEEP),
            nullableList = null,
            listNullableElement = listOf("foo", null, "baz"),
            nullableListNullableElement = null,
        )

        val item = ListsConverter.convertRight(lists)
        val converted = ListsConverter.convertLeft(item)
        assertEquals(lists, converted)
    }
}
