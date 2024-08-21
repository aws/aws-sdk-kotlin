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
import aws.sdk.kotlin.hll.dynamodbmapper.values.IntConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.StringConverter
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
            AttributeDescriptor("id", Item::id, Item::id::set, StringConverter),
            AttributeDescriptor("value", Item::value, Item::value::set, IntConverter),
        )
        private val schema = ItemSchema(converter, KeySpec.String("id"))
    }

    @BeforeAll
    fun setUp() = runTest {
        createTable(
            TABLE_NAME,
            schema,
            mapOf("id" to "foo", "value" to 42),
        )
    }

    @Test
    fun testDeleteItem() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, schema)

        val resp = table.deleteItem(
            DeleteItemRequest(
                Item(id = "foo"),
                ReturnConsumedCapacity.Indexes,
                null,
                ReturnValue.AllOld,
                null,
            ),
        )

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
