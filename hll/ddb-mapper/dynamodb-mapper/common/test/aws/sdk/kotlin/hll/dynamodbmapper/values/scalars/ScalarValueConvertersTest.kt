/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConvertersTest
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import kotlin.test.Test

class ScalarValueConvertersTest : ValueConvertersTest() {
    @Test
    fun testBooleanConverter() = given(BooleanValueConverter) {
        true inDdbIs theSame
        false inDdbIs theSame
    }

    @Test
    fun testByteArrayConverter() = given(ByteArrayValueConverter) {
        byteArrayOf() inDdbIs theSame
        "Foo".encodeToByteArray() inDdbIs theSame
        ByteArray(1024) { it.toByte() } inDdbIs theSame
    }

    @Test
    fun testByteConverter() = given(NumberValueConverters.Byte) {
        1.toByte() inDdbIs theSame
        47.toByte() inDdbIs theSame
        Byte.MIN_VALUE inDdbIs theSame
    }

    @Test
    fun testCharArrayConverter() = given(CharArrayValueConverter) {
        charArrayOf() inDdbIs ""
        charArrayOf('G', 'u', 'i', 'n', 'e', 'a', ' ', 'p', 'i', 'g') inDdbIs "Guinea pig"
    }

    @Test
    fun testCharConverter() = given(CharValueConverter) {
        '!' inDdbIs "!"
        'X' inDdbIs "X"
        '7' inDdbIs "7"
    }

    @Test
    fun testDoubleConverter() = given(NumberValueConverters.Double) {
        (-1.41421) inDdbIs theSame
        2.71828 inDdbIs theSame
        3.14159 inDdbIs theSame
        6.62607e-34 inDdbIs theSame
        Double.NEGATIVE_INFINITY inDdbIs anError
        Double.NaN inDdbIs anError
    }

    @Test
    fun testEnumConverter() = given(EnumValueConverter<Suit>()) {
        Suit.HEARTS inDdbIs "HEARTS"
        Suit.DIAMONDS inDdbIs "DIAMONDS"
        Suit.CLUBS inDdbIs "CLUBS"
        Suit.SPADES inDdbIs "SPADES"
    }

    @Test
    fun testFloatConverter() = given(NumberValueConverters.Float) {
        (-1.73205f) inDdbIs theSame
        1.61803f inDdbIs theSame
        2.68545f inDdbIs theSame
        Float.POSITIVE_INFINITY inDdbIs anError
        Float.NaN inDdbIs anError
    }

    @Test
    fun testIntConverter() = given(NumberValueConverters.Int) {
        0 inDdbIs theSame
        1_000_000 inDdbIs theSame
        Int.MIN_VALUE inDdbIs theSame
    }

    @Test
    fun testLongConverter() = given(NumberValueConverters.Long) {
        31_536_000L inDdbIs theSame
        9_460_730_472_580_800L inDdbIs theSame
        Long.MIN_VALUE inDdbIs theSame
    }

    @Test
    fun testShortConverter() = given(NumberValueConverters.Short) {
        1.toShort() inDdbIs theSame
        47.toShort() inDdbIs theSame
        Short.MIN_VALUE inDdbIs theSame
    }

    @Test
    fun testStringConverter() = given(StringValueConverter) {
        "The quick brown fox jumped over the lazy dogs" inDdbIs theSame
        "Jackdaws love my big sphinx of quartz" inDdbIs theSame
        """
            Benjamín pidió una bebida de kiwi y fresa.
            Noé, sin vergüenza, la más exquisita champaña del menú.
        """.trimIndent() inDdbIs theSame
    }

    @Test
    fun testUByteConverter() = given(NumberValueConverters.UByte) {
        1.toUByte() inDdbIs 1
        47.toUByte() inDdbIs 47
        UByte.MAX_VALUE inDdbIs UByte.MAX_VALUE.toInt()
    }

    @Test
    fun testUIntConverter() = given(NumberValueConverters.UInt) {
        0u inDdbIs 0
        1_000_000u inDdbIs 1_000_000
        UInt.MAX_VALUE inDdbIs UInt.MAX_VALUE.toLong()
    }

    @Test
    fun testULongConverter() = given(NumberValueConverters.ULong) {
        31_536_000uL inDdbIs 31_536_000L
        9_460_730_472_580_800uL inDdbIs 9_460_730_472_580_800L
        ULong.MAX_VALUE inDdbIs AttributeValue.N(ULong.MAX_VALUE.toString())
    }

    @Test
    fun testUShortConverter() = given(NumberValueConverters.UShort) {
        1.toUShort() inDdbIs 1
        47.toUShort() inDdbIs 47
        UShort.MAX_VALUE inDdbIs UShort.MAX_VALUE.toInt()
    }
}

private enum class Suit {
    HEARTS,
    DIAMONDS,
    CLUBS,
    SPADES,
}
