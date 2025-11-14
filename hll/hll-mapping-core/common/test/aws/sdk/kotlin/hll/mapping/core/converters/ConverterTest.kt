package aws.sdk.kotlin.hll.mapping.core.converters

import kotlin.test.Test
import kotlin.test.assertEquals

class ConverterTest {
    @Test
    fun testPlus() {
        val first = Converter<Byte, Short>({ it.toShort() }, { it.toByte() })
        val second = Converter<Short, Int>({ it.toInt() }, { it.toShort() })

        val chain = first + second
        assertEquals(42, chain.convertRight(42.toByte()))
        assertEquals(42.toByte(), chain.convertLeft(42))

        val third = Converter<Int, Long>({ it.toLong() }, { it.toInt() })
        val longerChain = chain + third
        assertEquals(42L, longerChain.convertRight(42.toByte()))
        assertEquals(42.toByte(), longerChain.convertLeft(42L))
    }
}
