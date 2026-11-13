/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.smithy.kotlin.runtime.content.BigDecimal
import aws.smithy.kotlin.runtime.content.BigInteger
import org.example.dynamodbmapper.generatedschemas.MapsConverter
import kotlin.test.Test
import kotlin.test.assertEquals

public class MapsTest {
    @Test
    fun converterTest() {
        val maps = Maps(
            id = 1,
            mapStringString = mapOf("key1" to "value1", "key2" to "value2"),
            mapStringInt = mapOf("one" to 1, "two" to 2, "three" to 3),
            mapIntString = mapOf(1 to "one", 2 to "two", 3 to "three"),
            mapLongInt = mapOf(1L to 10, 2L to 20, 3L to 30),
            mapStringBoolean = mapOf("true" to true, "false" to false),
            mapStringBigDecimal = mapOf("pi" to BigDecimal("3.141592653589793238462643383279502884")),
            mapStringBigInteger = mapOf("big" to BigInteger("99999999999999999999999999999999999999")),
            mapStringJvmBigDecimal = mapOf("pi" to java.math.BigDecimal("3.141592653589793238462643383279502884")),
            mapStringJvmBigInteger = mapOf("big" to java.math.BigInteger("99999999999999999999999999999999999999")),
            mapBigDecimalString = mapOf(BigDecimal("-1.5") to "neg", BigDecimal("2.5") to "pos"),
            mapBigIntegerString = mapOf(BigInteger("0") to "zero", BigInteger("42") to "answer"),
            mapJvmBigDecimalString = mapOf(java.math.BigDecimal("-1.5") to "neg", java.math.BigDecimal("2.5") to "pos"),
            mapJvmBigIntegerString = mapOf(java.math.BigInteger("0") to "zero", java.math.BigInteger("42") to "answer"),
            mapStringListString = mapOf(
                "fruits" to listOf("apple", "banana", "cherry"),
                "colors" to listOf("red", "green", "blue"),
            ),
            mapStringListMapStringString = mapOf(
                "person1" to listOf(
                    mapOf("name" to "John", "age" to "30"),
                    mapOf("city" to "New York", "country" to "USA"),
                ),
                "person2" to listOf(
                    mapOf("name" to "Alice", "age" to "25"),
                    mapOf("city" to "London", "country" to "UK"),
                ),
            ),
            mapEnum = mapOf("pet1" to EnumAnimals.CAT, "pet2" to EnumAnimals.DOG, "pet3" to EnumAnimals.SHEEP),
            mapEnumKey = mapOf(EnumAnimals.CAT to "meow", EnumAnimals.DOG to "woof"),
            nullableMap = null,
            mapNullableValue = mapOf("key1" to "value1", "key2" to null),
            nullableMapNullableValue = null,
        )

        val item = MapsConverter.convertRight(maps)
        val converted = MapsConverter.convertLeft(item)
        assertEquals(maps, converted)
    }
}
