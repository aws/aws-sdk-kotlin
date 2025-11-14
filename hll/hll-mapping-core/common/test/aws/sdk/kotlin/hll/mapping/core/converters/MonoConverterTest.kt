package aws.sdk.kotlin.hll.mapping.core.converters

import kotlin.test.Test
import kotlin.test.assertEquals

class MonoConverterTest {
    @Test
    fun testPlus() {
        val first = MonoConverter<Byte, Short> { it.toShort() }
        val second = MonoConverter<Short, Int> { it.toInt() }

        val chain = first + second
        assertEquals(42, chain.convert(42.toByte()))

        val third = MonoConverter<Int, Long> { it.toLong() }
        val longerChain = chain + third
        assertEquals(42L, longerChain.convert(42.toByte()))
    }
}
