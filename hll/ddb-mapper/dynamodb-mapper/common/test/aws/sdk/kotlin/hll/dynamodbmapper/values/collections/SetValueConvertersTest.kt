/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConvertersTest
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import kotlin.test.Test

private val emptyNumberSet = AttributeValue.Ns(listOf())

class SetValueConvertersTest : ValueConvertersTest() {
    @Test
    fun testByteArraySetConverter() = given(ByteArraySetConverter) {
        setOf(byteArrayOf(1, 1, 2, 3), byteArrayOf(5, 8), byteArrayOf(13, 21, 34, 55, 89)) inDdbIs theSame
        setOf<ByteArray>() inDdbIs AttributeValue.Bs(listOf())
    }

    @Test
    fun testByteSetConverter() = given(NumberSetValueConverters.Byte) {
        setOf(1.toByte(), 5.toByte(), Byte.MAX_VALUE) inDdbIs theSame
        setOf<Byte>() inDdbIs emptyNumberSet
    }

    @Test
    fun testCharSetConverter() = given(CharSetValueConverter) {
        setOf('a', 'b', 'A', 'B') inDdbIs setOf("a", "b", "A", "B")
        setOf<Char>() inDdbIs AttributeValue.Ss(listOf())
    }

    @Test
    fun testDoubleSetConverter() = given(NumberSetValueConverters.Double) {
        setOf(1.0, -3.14159, Double.MAX_VALUE) inDdbIs theSame
        setOf<Double>() inDdbIs emptyNumberSet
    }

    @Test
    fun testFloatSetConverter() = given(NumberSetValueConverters.Float) {
        setOf(1.0f, -3.14159f, Float.MAX_VALUE) inDdbIs theSame
        setOf<Float>() inDdbIs emptyNumberSet
    }

    @Test
    fun testIntSetConverter() = given(NumberSetValueConverters.Int) {
        setOf(392, -5_129_352, Int.MAX_VALUE) inDdbIs theSame
        setOf<Int>() inDdbIs emptyNumberSet
    }

    @Test
    fun testLongSetConverter() = given(NumberSetValueConverters.Long) {
        setOf(392L, -5_129_352_000_000_000L, Long.MAX_VALUE) inDdbIs theSame
        setOf<Long>() inDdbIs emptyNumberSet
    }

    @Test
    fun testShortSetConverter() = given(NumberSetValueConverters.Short) {
        setOf(392.toShort(), (-1024).toShort(), Short.MAX_VALUE) inDdbIs theSame
        setOf<Short>() inDdbIs emptyNumberSet
    }

    @Test
    fun tesStringSetConverter() = given(StringSetValueConverter) {
        setOf("The", "quick", "brown", "fox", "jumped", "over", "the", "lazy", "dogs") inDdbIs theSame
        setOf<String>() inDdbIs AttributeValue.Ss(listOf())
    }

    @Test
    fun testUByteSetConverter() = given(NumberSetValueConverters.UByte) {
        setOf(1.toUByte(), (Byte.MAX_VALUE.toInt() + 1).toUByte(), UByte.MAX_VALUE) inDdbIs theSame
        setOf<UByte>() inDdbIs emptyNumberSet
    }

    @Test
    fun testUIntSetConverter() = given(NumberSetValueConverters.UInt) {
        setOf(392u, Int.MAX_VALUE.toUInt() + 1u, UInt.MAX_VALUE) inDdbIs theSame
        setOf<UInt>() inDdbIs emptyNumberSet
    }

    @Test
    fun testULongSetConverter() = given(NumberSetValueConverters.ULong) {
        setOf(392uL, Long.MAX_VALUE.toULong() + 1uL, ULong.MAX_VALUE) inDdbIs theSame
        setOf<ULong>() inDdbIs emptyNumberSet
    }

    @Test
    fun testUShortSetConverter() = given(NumberSetValueConverters.UShort) {
        setOf(392.toUShort(), (Short.MAX_VALUE.toInt() + 1).toUShort(), UShort.MAX_VALUE) inDdbIs theSame
        setOf<UShort>() inDdbIs emptyNumberSet
    }
}
