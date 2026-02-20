/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.AttributeDescriptor
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.SimpleItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.DdbLocalTest
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.getItem
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PutItemTest : DdbLocalTest() {
    companion object {
        private const val TABLE_NAME = "put-item-test"

        private data class Item(var id: String = "", var value: Int = 0)

        private val converter = SimpleItemConverter(
            ::Item,
            { this },
            AttributeDescriptor("id", Item::id, Item::id::set, StringValueConverter),
            AttributeDescriptor("value", Item::value, Item::value::set, NumberValueConverters.Int),
        )
        private val schema = ItemSchema(converter, KeySpec.string("id"))
    }

    @BeforeAll
    fun setUp() = runTest {
        createTable(TABLE_NAME, schema)
    }

    @Test
    fun testPutItem() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, schema)

        table.putItem { item = Item(id = "foo", value = 42) }

        val resp = lowLevelAccess { getItem(TABLE_NAME, "id" to "foo") }

        val item = assertNotNull(resp.item)
        assertEquals("foo", item["id"]?.asSOrNull())
        assertEquals(42, item["value"]?.asNOrNull()?.toIntOrNull())
    }
}
