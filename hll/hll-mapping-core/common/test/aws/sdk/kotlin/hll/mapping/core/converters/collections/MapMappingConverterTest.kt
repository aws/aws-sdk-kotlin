/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.collections

import aws.sdk.kotlin.hll.mapping.core.converters.testutils.AnimalEmojiConverter
import aws.sdk.kotlin.hll.mapping.core.converters.testutils.NumberToWordConverter
import kotlin.test.Test
import kotlin.test.assertEquals

class MapMappingConverterTest {
    @Test
    fun testMapMapping() {
        val converter = MapMappingConverter(AnimalEmojiConverter, NumberToWordConverter)

        val left = mapOf("horse" to 5, "bat" to 3, "eagle" to 10)
        val right = mapOf("🐎" to "five", "🦇" to "three", "🦅" to "ten")

        assertEquals(right, converter.convertRight(left))
        assertEquals(left, converter.convertLeft(right))
    }
}
