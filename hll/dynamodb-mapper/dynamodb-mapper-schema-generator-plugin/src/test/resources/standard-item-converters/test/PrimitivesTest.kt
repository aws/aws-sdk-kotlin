/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.smithy.kotlin.runtime.content.Document
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.time.Instant
import org.example.dynamodbmapper.generatedschemas.PrimitivesConverter
import kotlin.test.Test
import kotlin.test.assertEquals

public class PrimitivesTest {
    @Test
    fun converterTest() {
        val primitive = Primitives(
            id = 1,
            animal = EnumAnimals.CAT,
            boolean = true,
            string = "string",
            charArray = charArrayOf('a', 'b', 'c'),
            char = 'b',
            byte = 42,
            byteArray = byteArrayOf(5, 4, 3, 2, 1),
            short = Short.MAX_VALUE,
            int = Int.MAX_VALUE,
            long = Long.MAX_VALUE,
            double = Double.MAX_VALUE,
            float = Float.MAX_VALUE,
            uByte = UByte.MAX_VALUE,
            uInt = UInt.MAX_VALUE,
            uShort = UShort.MAX_VALUE,
            uLong = ULong.MAX_VALUE,
            instant = Instant.now(),
            url = Url.parse("https://aws.amazon.com"),
            document = Document.Number(5),
        )

        val item = PrimitivesConverter.convertRight(primitive)
        val converted = PrimitivesConverter.convertLeft(item)
        assertEquals(primitive, converted)
    }
}
