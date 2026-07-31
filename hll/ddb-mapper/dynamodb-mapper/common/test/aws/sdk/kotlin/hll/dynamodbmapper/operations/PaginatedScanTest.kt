/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.*
import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.DdbLocalTest
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.smithy.kotlin.runtime.testing.BeforeAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaginatedScanTest : DdbLocalTest() {
    companion object {
        private const val TABLE_NAME = "paginated-scan-test"

        private fun rankName(rank: Int) = when (rank) {
            2, 3, 4, 5, 6, 7, 8, 9, 10 -> rank.toString()
            11 -> "Jack"
            12 -> "Queen"
            13 -> "King"
            14 -> "Ace"
            else -> "Unknown ($rank)"
        }

        private data class Card(
            var rank: Int = 14,
            var suit: String = "Spades",
            var description: String = "This is the Ace of Spades. ".repeat(2000),
        ) {
            val rankName: String
                get() = rankName(rank)

            override fun toString() = "$rankName of $suit" // Easier when debugging big outputs
        }

        private val converter = SimpleItemConverter(
            ::Card,
            { this },
            AttributeDescriptor("rank", Card::rank, Card::rank::set, NumberValueConverters.Int),
            AttributeDescriptor("suit", Card::suit, Card::suit::set, StringValueConverter),
            AttributeDescriptor("description", Card::description, Card::description::set, StringValueConverter),
        )

        private val schema = ItemSchema(converter, KeySpec.string("suit"), KeySpec.int("rank"))

        private val allCards = listOf("Spades", "Clubs", "Hearts", "Diamonds").flatMap { suit ->
            (2..14).map { rank ->
                Card(rank, suit, "This is the ${rankName(rank)} of $suit. ".repeat(2000))
            }
        }.toSet()
    }

    @BeforeAll
    fun setUp() = runTest {
        createTable(
            name = TABLE_NAME,
            schema = schema,
            items = allCards.map { card ->
                itemOf(
                    "rank" to card.rank,
                    "suit" to card.suit,
                    "description" to card.description,
                )
            }.toTypedArray(),
        )
    }

    @Test
    fun testPaginatedScan() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, schema)
        var pages = 0

        val actual = table
            .scanPaginated { }
            .map {
                pages++
                it
            }
            .items()
            .toSet()
        assertEquals(allCards, actual)
        assertTrue(pages > 1, "Got only $pages pages but expected at least 2")
    }

    @Test
    fun testPaginatedScanWithOffset() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, schema)
        val startItem = allCards.first()
        var pages = 0

        val actual = table
            .scanPaginated {
                exclusiveStartPartitionKey = Key(startItem.suit)
                exclusiveStartSortKey = Key(startItem.rank)
            }
            .map {
                pages++
                it
            }
            .items()
            .toSet()
        assertTrue(startItem !in actual)
        assertTrue(pages > 1, "Got only $pages pages but expected at least 2")
    }
}
