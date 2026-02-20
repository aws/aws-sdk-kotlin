/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.KeyFilter
import aws.sdk.kotlin.hll.dynamodbmapper.items.AttributeDescriptor
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.SimpleItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.DdbLocalTest
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.assertContentEquals

class QueryTest : DdbLocalTest() {
    companion object {
        private const val TABLE_NAME = "query-test"
        private const val TITLE_INDEX_NAME = "emps-by-title"
        private const val NAME_INDEX_NAME = "emps-by-name"

        private data class NamedEmp(
            var companyId: String = "",
            var empId: String = "",
            var name: String = "",
            var title: String = "",
            var tenureYears: Int = 0,
        )

        private val empConverter = SimpleItemConverter(
            ::NamedEmp,
            { this },
            AttributeDescriptor("companyId", NamedEmp::companyId, NamedEmp::companyId::set, StringValueConverter),
            AttributeDescriptor("empId", NamedEmp::empId, NamedEmp::empId::set, StringValueConverter),
            AttributeDescriptor("name", NamedEmp::name, NamedEmp::name::set, StringValueConverter),
            AttributeDescriptor("title", NamedEmp::title, NamedEmp::title::set, StringValueConverter),
            AttributeDescriptor("tenureYears", NamedEmp::tenureYears, NamedEmp::tenureYears::set, NumberValueConverters.Int),
        )

        private val namedEmpSchema = ItemSchema(empConverter, KeySpec.string("companyId"), KeySpec.string("empId"))
        private val empsByNameSchema = ItemSchema(empConverter, KeySpec.string("companyId"), KeySpec.string("name"))

        private data class TitleEmp(
            var title: String = "",
            var name: String = "",
            var companyId: String = "",
            var empId: String = "",
        )

        private val titleConverter = SimpleItemConverter(
            ::TitleEmp,
            { this },
            AttributeDescriptor("title", TitleEmp::title, TitleEmp::title::set, StringValueConverter),
            AttributeDescriptor("name", TitleEmp::name, TitleEmp::name::set, StringValueConverter),
            AttributeDescriptor("empId", TitleEmp::empId, TitleEmp::empId::set, StringValueConverter),
            AttributeDescriptor("companyId", TitleEmp::companyId, TitleEmp::companyId::set, StringValueConverter),
        )

        private val titleSchema = ItemSchema(titleConverter, KeySpec.string("title"), KeySpec.string("name"))
    }

    @BeforeAll
    fun setUp() = runTest {
        createTable(
            name = TABLE_NAME,
            schema = namedEmpSchema,
            gsis = mapOf(TITLE_INDEX_NAME to titleSchema),
            lsis = mapOf(NAME_INDEX_NAME to empsByNameSchema),
            items = listOf(
                itemOf(
                    "companyId" to "foo-corp",
                    "empId" to "AB0123",
                    "name" to "Alice Birch",
                    "title" to "SDE",
                    "tenureYears" to 5,
                ),
                itemOf(
                    "companyId" to "foo-corp",
                    "empId" to "AB0126",
                    "name" to "Adriana Beech",
                    "title" to "Manager",
                    "tenureYears" to 7,
                ),
                itemOf(
                    "companyId" to "foo-corp",
                    "empId" to "EF0124",
                    "name" to "Eddie Fraser",
                    "title" to "SDE",
                    "tenureYears" to 3,
                ),
                itemOf(
                    "companyId" to "bar-corp",
                    "empId" to "157X",
                    "name" to "Charlie Douglas",
                    "title" to "Manager",
                    "tenureYears" to 4,
                ),
            ),
        )
    }

    @Test
    fun testQueryTable() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, namedEmpSchema)

        val items = table.queryPaginated {
            keyCondition = KeyFilter("foo-corp")
        }.items().toList()

        val expected = listOf(
            NamedEmp(
                companyId = "foo-corp",
                empId = "AB0123",
                name = "Alice Birch",
                title = "SDE",
                tenureYears = 5,
            ),
            NamedEmp(
                companyId = "foo-corp",
                empId = "AB0126",
                name = "Adriana Beech",
                title = "Manager",
                tenureYears = 7,
            ),
            NamedEmp(
                companyId = "foo-corp",
                empId = "EF0124",
                name = "Eddie Fraser",
                title = "SDE",
                tenureYears = 3,
            ),
        )

        assertContentEquals(expected, items)
    }

    @Test
    fun testQueryTableWithSortKeyCondition() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, namedEmpSchema)

        val items = table.queryPaginated {
            keyCondition = KeyFilter("foo-corp", { sortKey startsWith "AB0" })
        }.items().toList()

        val expected = listOf(
            NamedEmp(
                companyId = "foo-corp",
                empId = "AB0123",
                name = "Alice Birch",
                title = "SDE",
                tenureYears = 5,
            ),
            NamedEmp(
                companyId = "foo-corp",
                empId = "AB0126",
                name = "Adriana Beech",
                title = "Manager",
                tenureYears = 7,
            ),
        )

        assertContentEquals(expected, items)
    }

    @Test
    fun testQueryTableWithFilter() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, namedEmpSchema)

        val items = table.queryPaginated {
            keyCondition = KeyFilter("foo-corp")
            filter { attr("title") eq "SDE" }
        }.items().toList()

        val expected = listOf(
            NamedEmp(
                companyId = "foo-corp",
                empId = "AB0123",
                name = "Alice Birch",
                title = "SDE",
                tenureYears = 5,
            ),
            NamedEmp(
                companyId = "foo-corp",
                empId = "EF0124",
                name = "Eddie Fraser",
                title = "SDE",
                tenureYears = 3,
            ),
        )

        assertContentEquals(expected, items)
    }

    @Test
    fun testQueryGsi() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, namedEmpSchema)
        val index = table.getIndex(TITLE_INDEX_NAME, titleSchema)

        val items = index.queryPaginated {
            keyCondition = KeyFilter("Manager")
        }.items().toList()

        val expected = listOf(
            TitleEmp(
                companyId = "foo-corp",
                empId = "AB0126",
                name = "Adriana Beech",
                title = "Manager",
            ),
            TitleEmp(
                companyId = "bar-corp",
                empId = "157X",
                name = "Charlie Douglas",
                title = "Manager",
            ),
        )

        assertContentEquals(expected, items)
    }

    @Test
    fun testQueryLsi() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, namedEmpSchema)
        val index = table.getIndex(NAME_INDEX_NAME, empsByNameSchema)

        val items = index.queryPaginated {
            keyCondition = KeyFilter("foo-corp")
        }.items().toList()

        val expected = listOf(
            NamedEmp(
                companyId = "foo-corp",
                empId = "AB0126",
                name = "Adriana Beech",
                title = "Manager",
                tenureYears = 7,
            ),
            NamedEmp(
                companyId = "foo-corp",
                empId = "AB0123",
                name = "Alice Birch",
                title = "SDE",
                tenureYears = 5,
            ),
            NamedEmp(
                companyId = "foo-corp",
                empId = "EF0124",
                name = "Eddie Fraser",
                title = "SDE",
                tenureYears = 3,
            ),
        )

        assertContentEquals(expected, items)
    }
}
