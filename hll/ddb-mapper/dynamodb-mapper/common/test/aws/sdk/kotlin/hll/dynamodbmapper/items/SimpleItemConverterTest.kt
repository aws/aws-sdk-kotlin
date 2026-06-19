/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.model.intersectKeys
import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.BooleanValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SimpleItemConverterTest {
    @Test
    fun testBasicConversion() {
        val converter = SimpleItemConverter(
            ::ProductBuilder,
            ProductBuilder::build,
            AttributeDescriptor("id", Product::id, ProductBuilder::id::set, NumberValueConverters.Int),
            AttributeDescriptor("name", Product::name, ProductBuilder::name::set, StringValueConverter),
            AttributeDescriptor("in-stock", Product::inStock, ProductBuilder::inStock::set, BooleanValueConverter),
        )

        val foo = Product(42, "Foo 2.0", inStock = true)
        val item = converter.convertRight(foo)

        assertEquals(3, item.size)
        assertEquals(42, item.getValue("id").asN().toInt())
        assertEquals("Foo 2.0", item.getValue("name").asS())
        assertTrue(item.getValue("in-stock").asBool())

        val unconverted = converter.convertLeft(item)
        assertEquals(foo, unconverted)
    }

    @Test
    fun testKeyOnlyConversion() {
        val converter = SimpleItemConverter(
            ::ProductBuilder,
            ProductBuilder::build,
            AttributeDescriptor("id", Product::id, ProductBuilder::id::set, NumberValueConverters.Int),
            AttributeDescriptor("name", Product::name, ProductBuilder::name::set, StringValueConverter),
            AttributeDescriptor("in-stock", Product::inStock, ProductBuilder::inStock::set, BooleanValueConverter),
        )

        val foo = Product(42, "Foo 2.0", inStock = true)
        val item = converter.convertRight(foo).intersectKeys(setOf("id", "name"))

        assertEquals(2, item.size)
        assertEquals(42, item.getValue("id").asN().toInt())
        assertEquals("Foo 2.0", item.getValue("name").asS())
    }

    @Test
    fun testUnknownAttributesThrowExceptions() {
        val converter = SimpleItemConverter(
            ::ProductBuilder,
            ProductBuilder::build,
            AttributeDescriptor("id", Product::id, ProductBuilder::id::set, NumberValueConverters.Int),
            AttributeDescriptor("name", Product::name, ProductBuilder::name::set, StringValueConverter),
            AttributeDescriptor("in-stock", Product::inStock, ProductBuilder::inStock::set, BooleanValueConverter),
            unknownValueHandling = UnknownValueHandling.ThrowException,
        )

        val item = itemOf(
            "id" to 42,
            "name" to "Foo 2.0",
            "in-stock" to true,
            "sneaky-unknown-attribute" to "😈",
        )

        assertFailsWith<IllegalArgumentException> { converter.convertLeft(item) }
    }

    @Test
    fun testUnknownAttributesCustomHandler() {
        data class UnknownAttribute(val name: String, val value: AttributeValue, val builder: ProductBuilder)
        val unknownAttributes = mutableListOf<UnknownAttribute>()

        val converter = SimpleItemConverter(
            ::ProductBuilder,
            ProductBuilder::build,
            AttributeDescriptor("id", Product::id, ProductBuilder::id::set, NumberValueConverters.Int),
            AttributeDescriptor("name", Product::name, ProductBuilder::name::set, StringValueConverter),
            AttributeDescriptor("in-stock", Product::inStock, ProductBuilder::inStock::set, BooleanValueConverter),
            unknownValueHandling = UnknownValueHandling.Custom { name, value, builder ->
                unknownAttributes += UnknownAttribute(name, value, builder)
            },
        )

        val item = itemOf(
            "id" to 42,
            "name" to "Foo 2.0",
            "in-stock" to true,
            "sneaky-unknown-attribute" to "😈",
        )
        val product = converter.convertLeft(item)

        assertEquals(1, unknownAttributes.size)
        val unknown = unknownAttributes.single()

        assertEquals("sneaky-unknown-attribute", unknown.name)
        assertEquals("😈", unknown.value.asS())
        assertEquals(42, unknown.builder.id)
        assertEquals("Foo 2.0", unknown.builder.name)
        assertEquals(true, unknown.builder.inStock)

        assertEquals(Product(42, "Foo 2.0", inStock = true), product)
    }
}

private data class Product(
    val id: Int,
    val name: String,
    val inStock: Boolean,
)

private class ProductBuilder {
    var id: Int? = null
    var name: String? = null
    var inStock: Boolean? = null

    fun build() = Product(id!!, name!!, inStock!!)
}
