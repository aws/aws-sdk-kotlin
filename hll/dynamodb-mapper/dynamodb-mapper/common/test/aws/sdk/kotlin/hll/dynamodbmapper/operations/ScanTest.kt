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
import aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes.InstantValueConverter
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals

class ScanTest : DdbLocalTest() {
    companion object {
        private const val TABLE_NAME = "scan-test"

        private data class Product(
            var id: Int = 0,
            var name: String = "",
            var modelNumber: String = "",
            var released: Instant = Instant.now(),
        )

        private val converter = SimpleItemConverter(
            ::Product,
            { this },
            AttributeDescriptor("id", Product::id, Product::id::set, NumberValueConverters.Int),
            AttributeDescriptor("name", Product::name, Product::name::set, StringValueConverter),
            AttributeDescriptor("modelNumber", Product::modelNumber, Product::modelNumber::set, StringValueConverter),
            AttributeDescriptor("released", Product::released, Product::released::set, InstantValueConverter.Iso8601),
        )

        private val schema = ItemSchema(converter, KeySpec.number<Int>("id"))
    }

    @BeforeAll
    fun setUp() = runTest {
        createTable(
            name = TABLE_NAME,
            schema = schema,
            itemOf(
                "id" to 1,
                "name" to "Standard Widget",
                "modelNumber" to "wid-001",
                "released" to "2024-09-24T10:15:23Z",
            ),
            itemOf(
                "id" to 2,
                "name" to "Widget Plus+",
                "modelNumber" to "wid-010",
                "released" to "2024-09-25T10:15:23Z",
            ),
            itemOf(
                "id" to 3,
                "name" to "Gizmo",
                "modelNumber" to "GIZ.m0",
                "released" to "2024-09-20T10:15:23Z",
            ),
            itemOf(
                "id" to 4,
                "name" to "Thingy",
                "modelNumber" to "T1",
                "released" to "2024-09-19T10:15:23Z",
            ),
            itemOf(
                "id" to 5,
                "name" to "Doohickey",
                "modelNumber" to "doohick/x",
                "released" to "2024-09-21T10:15:23Z",
            ),
        )
    }

    @Test
    fun testScanTable() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, schema)

        table.scanPaginated { }.assertItems("Standard Widget", "Widget Plus+", "Gizmo", "Thingy", "Doohickey")
    }

    @Test
    fun testScanTableWithComparisonFilter() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, schema)

        table.scanPaginated {
            filter { attr("id") gt 2 }
        }.assertItems("Gizmo", "Thingy", "Doohickey")
    }

    @Test
    fun testScanTableWithRangeFilter() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, schema)

        table.scanPaginated {
            filter { attr("released") isIn "2024-09-21T00:00:00Z".."2024-09-25T00:00:00Z" }
        }.assertItems("Standard Widget", "Doohickey")
    }

    @Test
    fun testScanTableWithSizeFilter() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, schema)

        table.scanPaginated {
            filter { attr("name").size gte 10 }
        }.assertItems("Standard Widget", "Widget Plus+")
    }

    private suspend fun Flow<ScanResponse<Product>>.assertItems(vararg names: String) {
        // Use sets for comparison because DDB partition keys aren't always sorted the way one might expect
        val expected = names.toSet()
        val actual = items().map { it.name }.toSet()

        assertEquals(expected, actual)
    }
}
