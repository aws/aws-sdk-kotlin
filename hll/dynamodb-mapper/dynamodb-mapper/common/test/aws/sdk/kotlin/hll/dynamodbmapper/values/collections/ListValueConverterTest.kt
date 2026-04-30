/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.util.av
import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConvertersTest
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import kotlin.test.Test

class ListValueConverterTest : ValueConvertersTest() {
    @Test
    fun testListConverter() = given(ListValueConverter(FooConverter)) {
        listOf(Foo("apple", 1), Foo("banana", 2), Foo("cherry", 3)) inDdbIs listOf(
            mapOf("bar" to "apple", "baz" to 1),
            mapOf("bar" to "banana", "baz" to 2),
            mapOf("bar" to "cherry", "baz" to 3),
        )

        List(3) { Foo("date", 4) } inDdbIs List(3) { mapOf("bar" to "date", "baz" to 4) }

        listOf<Foo>() inDdbIs theSame
    }
}

private data class Foo(val bar: String, val baz: Int)

private object FooConverter : ValueConverter<Foo> {
    override val left: MonoConverter<AttributeValue, Foo> = MonoConverter { value ->
        val map = value.asM()
        val bar = map.getValue("bar").asS()
        val baz = map.getValue("baz").asN().toInt()
        Foo(bar, baz)
    }

    override val right: MonoConverter<Foo, AttributeValue> = MonoConverter { obj ->
        AttributeValue.M(
            mapOf(
                "bar" to av(obj.bar),
                "baz" to av(obj.baz),
            ),
        )
    }
}
