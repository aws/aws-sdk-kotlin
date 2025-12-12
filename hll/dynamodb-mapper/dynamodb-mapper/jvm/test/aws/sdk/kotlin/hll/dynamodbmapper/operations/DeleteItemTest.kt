/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.AttributeDescriptor
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.SimpleItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.DdbLocalTest
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity
import aws.sdk.kotlin.services.dynamodb.model.ReturnValue
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DeleteItemTest : DdbLocalTest() {
    companion object {
        private const val TABLE_NAME = "delete-item-test"

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
        createTable(
            TABLE_NAME,
            schema,
            itemOf("id" to "foo", "value" to 42),
        )
    }

    @Test
    fun testDeleteItem() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, schema)

        val resp = table.deleteItem {
            key = Item(id = "foo")
            returnConsumedCapacity = ReturnConsumedCapacity.Indexes
            returnValues = ReturnValue.AllOld
        }

        val item = assertNotNull(resp.attributes)
        assertEquals("foo", item.id)
        assertEquals(42, item.value)

        val cc = assertNotNull(resp.consumedCapacity)
        assertEquals(1.0, cc.capacityUnits)
        assertEquals(TABLE_NAME, cc.tableName)

        val tableCapacity = assertNotNull(cc.table)
        assertEquals(1.0, tableCapacity.capacityUnits)
    }
}
