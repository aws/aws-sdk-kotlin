/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.KeyFilter
import aws.sdk.kotlin.hll.dynamodbmapper.items.*
import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.DdbLocalTest
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.smithy.kotlin.runtime.testing.BeforeAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QueryMultiAttrKeyTest : DdbLocalTest() {
    companion object {
        private const val TABLE_NAME = "query-multi-attr-keys-test"
        private const val INDEX_NAME = "multi-attr-keys-index"

        private data class Event(
            var eventId: String = "", // Table PK
            var companyId: Int = 0, // Index PK[0]
            var department: String = "", // Index PK[1]
            var category: String = "", // Index SK[0]
            var timestamp: Long = 0L, // Index SK[1]
            var description: String = "",
        )

        private val eventConverter = SimpleItemConverter(
            ::Event,
            { this },
            AttributeDescriptor("eventId", Event::eventId, Event::eventId::set, StringValueConverter),
            AttributeDescriptor("companyId", Event::companyId, Event::companyId::set, NumberValueConverters.Int),
            AttributeDescriptor("department", Event::department, Event::department::set, StringValueConverter),
            AttributeDescriptor("category", Event::category, Event::category::set, StringValueConverter),
            AttributeDescriptor("timestamp", Event::timestamp, Event::timestamp::set, NumberValueConverters.Long),
            AttributeDescriptor("description", Event::description, Event::description::set, StringValueConverter),
        )

        private val tableSchema = eventConverter.withKeySpec(KeySpec.string("eventId"))

        private val indexSchema = eventConverter.withKeySpec(
            KeySpec.int("companyId").thenString("department"),
            KeySpec.string("category").thenLong("timestamp"),
        )
    }

    @BeforeAll
    fun setUp() = runTest {
        createTable(
            name = TABLE_NAME,
            schema = tableSchema,
            gsis = mapOf(INDEX_NAME to indexSchema),
            lsis = mapOf(),
            items = listOf(
                itemOf(
                    "eventId" to "05deb8cd-1b27-428c-b685-12890e4b9144",
                    "companyId" to 128,
                    "department" to "Billing",
                    "category" to "Personnel changes",
                    "timestamp" to 1_765_569_132_000_000_000L,
                    "description" to "New employee joined!",
                ),
                itemOf(
                    "eventId" to "46af0204-d738-4c6f-8a78-b08da8196f6c",
                    "companyId" to 128,
                    "department" to "Billing",
                    "category" to "Personnel changes",
                    "timestamp" to 1_765_569_133_000_000_000L,
                    "description" to "Employee retired",
                ),
            ),
        )
    }

    @Test
    fun testQueryIndexAllKeys() = runTest {
        val index = mapper().getTable(TABLE_NAME, tableSchema).getIndex(INDEX_NAME, indexSchema)

        val descriptions = index.queryPaginated {
            keyCondition = KeyFilter(
                Key(128)("Billing"),
                { sortKey eq "Personnel changes" },
                { sortKey eq 1_765_569_132_000_000_000L },
            )
        }.items().map { it.description }.toList()

        assertEquals(listOf("New employee joined!"), descriptions)
    }

    @Test
    fun testQueryIndexPartialKeys() = runTest {
        val index = mapper().getTable(TABLE_NAME, tableSchema).getIndex(INDEX_NAME, indexSchema)

        val descriptions = index.queryPaginated {
            keyCondition = KeyFilter(
                Key(128)("Billing"),
                { sortKey eq "Personnel changes" },
            )
        }.items().map { it.description }.toList()

        assertEquals(listOf("New employee joined!", "Employee retired"), descriptions)
    }

    @Test
    fun testQueryIndexInsufficientKeys() = runTest {
        val index = mapper().getTable(TABLE_NAME, tableSchema).getIndex(INDEX_NAME, indexSchema)

        val items = index.queryPaginated {
            keyCondition = KeyFilter(Key(128)) // Should fail because PK has two attributes but we only set one value
        }.items()

        assertFailsWith<IllegalArgumentException> { items.toList() }
    }
}
