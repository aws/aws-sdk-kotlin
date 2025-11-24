/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import org.example.dynamodbmapper.generatedschemas.SetsConverter
import kotlin.test.Test
import kotlin.test.assertEquals

public class SetsTest {
    @Test
    fun converterTest() {
        val sets = Sets(
            id = 1,
            setString = setOf("one", "two", "three"),
            setCharArray = setOf(charArrayOf('a', 'b'), charArrayOf('c', 'd')),
            setChar = setOf('x', 'y', 'z'),
            setByte = setOf(10, 20, 30),
            setDouble = setOf(1.1, 2.2, 3.3),
            setFloat = setOf(1.0f, 2.0f, 3.0f),
            setInt = setOf(100, 200, 300),
            setLong = setOf(1000L, 2000L, 3000L),
            setShort = setOf(1000.toShort(), 2000.toShort(), 3000.toShort()),
            setUByte = setOf(10u, 20u, 30u),
            setUInt = setOf(100u, 200u, 300u),
            setULong = setOf(1000uL, 2000uL, 3000uL),
            setUShort = setOf(1000u.toUShort(), 2000u.toUShort(), 3000u.toUShort()),
            nullableSet = null,
        )

        val item = SetsConverter.convertRight(sets)
        val converted = SetsConverter.convertLeft(item)
        assertEquals(sets, converted)
    }
}
