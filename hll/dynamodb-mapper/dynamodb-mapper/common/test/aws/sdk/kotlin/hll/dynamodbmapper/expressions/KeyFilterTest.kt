/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.FilterImpl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.toExpression
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KeyFilterTest {
    private val singleKeySchema = ItemSchema(DummyConverter, KeySpec.String("primary"))
    private val compositeSchema = ItemSchema(DummyConverter, KeySpec.String("primary"), KeySpec.Number("secondary"))

    @Test
    fun testSingleKeySchema() {
        val kf = KeyFilter("foo")
        val actual = kf.toExpression(singleKeySchema)
        val expected = FilterImpl.run { attr("primary") eq "foo" }

        assertEquals(expected, actual)
    }

    @Test
    fun testSingleKeySchemaWithErroneousSortKey() {
        val kf = KeyFilter("foo") { sortKey eq 2 }

        assertFailsWith<IllegalArgumentException> {
            kf.toExpression(singleKeySchema)
        }
    }

    @Test
    fun testCompositeSchema() {
        val kf = KeyFilter("foo") { sortKey lte 10 }
        val actual = kf.toExpression(compositeSchema)
        val expected = FilterImpl.run {
            and(
                attr("primary") eq "foo",
                attr("secondary") lte 10,
            )
        }

        assertEquals(expected, actual)
    }

    @Test
    fun testCompositeSchemaWithoutSortKey() {
        val kf = KeyFilter("foo")
        val actual = kf.toExpression(compositeSchema)
        val expected = FilterImpl.run { attr("primary") eq "foo" }

        assertEquals(expected, actual)
    }
}

val DummyConverter = Converter<Any, Item>( { error("Not needed") }, { error("Not needed") })
