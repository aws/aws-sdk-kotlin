/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.converters

import kotlin.test.Test
import kotlin.test.assertEquals

class ConverterChainTest {
    @Test
    fun testPlus() {
        val first = ConverterImpl<Byte, Short>({ it.toShort() }, { it.toByte() })
        val second = ConverterImpl<Short, Int>({ it.toInt() }, { it.toShort() })

        val chain = ConverterChain(first, second)
        assertEquals(42, chain.convertRight(42.toByte()))
        assertEquals(42.toByte(), chain.convertLeft(42))

        val third = ConverterImpl<Int, Long>({ it.toLong() }, { it.toInt() })
        val longerChain = ConverterChain(chain, third)
        assertEquals(42L, longerChain.convertRight(42.toByte()))
        assertEquals(42.toByte(), longerChain.convertLeft(42L))
    }
}
