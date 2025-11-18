/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.util.attr
import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConvertersTest
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import kotlin.test.Test

class MapValueConverterTest : ValueConvertersTest() {
    @Test
    fun testMapConverter() = given(MapValueConverter(BarConverter)) {
        mapOf("short" to Bar(false, "meh"), "long" to Bar(true, "m", "e", "h")) inDdbIs mapOf(
            "short" to listOf(false, "meh"),
            "long" to listOf(true, "m", "e", "h"),
        )

        mapOf<String, Bar>() inDdbIs theSame
    }
}

private data class Bar(val foo: Boolean, val baz: List<String>) {
    constructor(foo: Boolean, vararg baz: String) : this(foo, baz.toList())
}

private object BarConverter : ValueConverter<Bar> {
    override val left: MonoConverter<AttributeValue, Bar> = MonoConverter { value ->
        val list = value.asL()
        val foo = list.first().asBool()
        val baz = list.drop(1).map { it.asS() }
        Bar(foo, baz)
    }

    override val right: MonoConverter<Bar, AttributeValue> = MonoConverter { obj ->
        val list = listOf(attr(obj.foo)) + obj.baz.map(::attr)
        AttributeValue.L(list)
    }
}
