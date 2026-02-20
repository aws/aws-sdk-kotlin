/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.*
import aws.sdk.kotlin.hll.dynamodbmapper.model.getItem
import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.DdbLocalTest
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GetItemTest : DdbLocalTest() {
    companion object {
        private const val PK_TABLE_NAME = "get-item-test-pk"
        private const val CK_TABLE_NAME = "get-item-test-ck"

        private data class PkItem(var id: Int = 0, var value: String = "")

        private val pkConverter = SimpleItemConverter(
            ::PkItem,
            { this },
            AttributeDescriptor("id", PkItem::id, PkItem::id::set, NumberValueConverters.Int),
            AttributeDescriptor("value", PkItem::value, PkItem::value::set, StringValueConverter),
        )
        private val pkSchema = ItemSchema(pkConverter, KeySpec.int("id"))

        private data class CkItem(var id: String = "", var version: Int = 0, var value: String = "")

        private val ckConverter = SimpleItemConverter(
            ::CkItem,
            { this },
            AttributeDescriptor("id", CkItem::id, CkItem::id::set, StringValueConverter),
            AttributeDescriptor("version", CkItem::version, CkItem::version::set, NumberValueConverters.Int),
            AttributeDescriptor("value", CkItem::value, CkItem::value::set, StringValueConverter),
        )
        private val ckSchema = ItemSchema(ckConverter, KeySpec.string("id"), KeySpec.int("version"))
    }

    @BeforeAll
    fun setUp() = runTest {
        createTable(PK_TABLE_NAME, pkSchema, itemOf("id" to 1, "value" to "foo"))
        createTable(CK_TABLE_NAME, ckSchema, itemOf("id" to "abcd", "version" to 42, "value" to "foo"))
    }

    private fun testGetItem(
        vararg keys: Int,
        returnConsumedCapacity: ReturnConsumedCapacity? = null,
        action: (GetItemResponse<PkItem>) -> Unit,
    ) = runTest {
        val table = mapper().getTable(PK_TABLE_NAME, pkSchema)
        keys.forEach { key ->
            val response = table.getItem {
                partitionKey = Key(key)
                this.returnConsumedCapacity = returnConsumedCapacity
            }

            action(response)
        }
    }

    private fun testGetItem(
        vararg keys: Pair<String, Int>,
        returnConsumedCapacity: ReturnConsumedCapacity? = null,
        action: (GetItemResponse<CkItem>) -> Unit,
    ) = runTest {
        val table = mapper().getTable(CK_TABLE_NAME, ckSchema)
        keys.forEach { key ->
            val response = table.getItem {
                partitionKey = Key(key.first)
                sortKey = Key(key.second)
                this.returnConsumedCapacity = returnConsumedCapacity
            }

            action(response)
        }
    }

    @Test
    fun testPkGetItem() = testGetItem(1) {
        val item = assertNotNull(it.item)
        assertEquals(1, item.id)
        assertEquals("foo", item.value)
    }

    @Test
    fun testPkGetItemInvalidKey() = testGetItem(2, 3) {
        assertNull(it.item)
    }

    @Test
    fun testCkGetItem() = testGetItem("abcd" to 42) {
        val item = assertNotNull(it.item)
        assertEquals("abcd", item.id)
        assertEquals(42, item.version)
        assertEquals("foo", item.value)
    }

    @Test
    fun testCkGetItemInvalidKey() = testGetItem("bcde" to 41, "abcd" to 41, "bcde" to 42) {
        assertNull(it.item)
    }

    @Test
    fun testGetItemAdditionalParams() = testGetItem(42, returnConsumedCapacity = ReturnConsumedCapacity.Indexes) {
        val cc = assertNotNull(it.consumedCapacity)
        assertEquals(0.5, cc.capacityUnits)
        assertEquals(PK_TABLE_NAME, cc.tableName)

        val tableCapacity = assertNotNull(cc.table)
        assertEquals(0.5, tableCapacity.capacityUnits)
    }

    @Test
    fun testPkGetItemByScalarKey() = runTest {
        val table = mapper().getTable(PK_TABLE_NAME, pkSchema)

        val item = assertNotNull(table.getItem(1))
        assertEquals("foo", item.value)

        assertNull(table.getItem(2))
    }

    @Test
    fun testCkGetItemByScalarKeys() = runTest {
        val table = mapper().getTable(CK_TABLE_NAME, ckSchema)

        val item = assertNotNull(table.getItem(Key("abcd"), Key(42)))
        assertEquals("foo", item.value)

        assertNull(table.getItem(Key("abcd"), Key(43)))
    }
}
