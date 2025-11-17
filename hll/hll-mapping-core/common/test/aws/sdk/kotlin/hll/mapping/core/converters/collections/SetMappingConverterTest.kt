/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters.collections

import aws.sdk.kotlin.hll.mapping.core.converters.testutils.NumberToWordConverter
import kotlin.test.Test
import kotlin.test.assertEquals

class SetMappingConverterTest {
    @Test
    fun testSetMapping() {
        val converter = SetMappingConverter(NumberToWordConverter)

        val left = setOf(1, 1, 2, 3, 5, 8)
        val right = setOf("one", "one", "two", "three", "five", "eight")

        assertEquals(right, converter.convertRight(left))
        assertEquals(left, converter.convertLeft(right))
    }
}
