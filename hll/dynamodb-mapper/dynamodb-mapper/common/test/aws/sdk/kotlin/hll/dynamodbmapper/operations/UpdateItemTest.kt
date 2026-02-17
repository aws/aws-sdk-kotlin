/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.FilterDslImpl
import aws.sdk.kotlin.hll.dynamodbmapper.items.AttributeDescriptor
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.Key
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.SimpleItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.DdbLocalTest
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.getItem
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UpdateItemTest : DdbLocalTest() {
    companion object {
        private const val TABLE_NAME = "update-item-test"

        private data class Entity(var id: Int = 0)

        private val converter = SimpleItemConverter(
            ::Entity,
            { this },
            AttributeDescriptor("id", Entity::id, Entity::id::set, NumberValueConverters.Int),
        )
        private val schema = ItemSchema(converter, KeySpec.int("id"))
    }

    @BeforeAll
    fun setUp() = runTest {
        createTable(
            TABLE_NAME,
            schema,
            itemOf(
                "id" to 42,
                "name" to "Ian",
                "addresses" to mapOf(
                    "Home" to mapOf(
                        "street" to "1234 Side Street",
                        "city" to "Metropolis",
                        "state" to "Great State",
                        "country" to "USA",
                        "postalCode" to "12345",
                    ),
                    "Work" to mapOf(
                        "street" to "567 Main Street",
                        "city" to "Metropolis",
                        "state" to "Great State",
                        "country" to "USA",
                        "postalCode" to "23456",
                    ),
                ),
                "start" to 433753200,
                "end" to 3589513200,
                "petNames" to setOf(
                    "Bambi",
                    "Willow",
                    "Tori",
                    "Chewie",
                    "Ginny",
                    "Maisie",
                    "Pineapple",
                    "Gizmo",
                    "Bug",
                ),
                "recentAtBats" to listOf(
                    "Strikeout",
                    "Single",
                    "Strikeout",
                    "Base on balls",
                    "Double",
                ),
                "cardsInHand" to setOf(
                    "5 of Clubs",
                    "King of Hearts",
                    "Ace of Diamonds",
                    "8 of Spades",
                ),
            ),
        )
    }

    @Test
    fun testUpdateItem() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, schema)

        table.updateItem {
            partitionKey = Key(42)
            update {
                set {
                    attr["name"] = "Ian B"
                    attr["addresses"]["Work"]["postalCode"] = "65432"
                    attr["duration"] = attr["start"] + attr["end"]
                }
                remove {
                    -FilterDslImpl.attr["recentAtBats"][0]
                    -FilterDslImpl.attr["recentAtBats"][2]
                }
                add {
                    attr["luckyNumber"] += 13
                    attr["petNames"] += setOf("Arrayah", "Coffee")
                }
                delete {
                    attr["cardsInHand"] -= setOf("Ace of Diamonds")
                }
            }
        }

        val resp = lowLevelAccess { getItem(TABLE_NAME, "id" to 42) }
        val item = assertNotNull(resp.item)

        assertEquals(42, item["id"]?.asNOrNull()?.toIntOrNull())
        assertEquals("Ian B", item["name"]?.asSOrNull())
        assertEquals(13, item["luckyNumber"]?.asNOrNull()?.toIntOrNull())

        val addresses = assertNotNull(item["addresses"]?.asMOrNull(), """Missing item attribute "addresses"!""")

        val homeAddr = assertNotNull(addresses["Home"]?.asMOrNull(), """Missing address key "Home"!""")
        assertEquals("1234 Side Street", homeAddr["street"]?.asSOrNull())
        assertEquals("Metropolis", homeAddr["city"]?.asSOrNull())
        assertEquals("Great State", homeAddr["state"]?.asSOrNull())
        assertEquals("USA", homeAddr["country"]?.asSOrNull())
        assertEquals("12345", homeAddr["postalCode"]?.asSOrNull())

        val workAddr = assertNotNull(addresses["Work"]?.asMOrNull(), """Missing address key "Work"!""")
        assertEquals("567 Main Street", workAddr["street"]?.asSOrNull())
        assertEquals("Metropolis", workAddr["city"]?.asSOrNull())
        assertEquals("Great State", workAddr["state"]?.asSOrNull())
        assertEquals("USA", workAddr["country"]?.asSOrNull())
        assertEquals("65432", workAddr["postalCode"]?.asSOrNull())

        val petNames = assertNotNull(item["petNames"]?.asSsOrNull()?.toSet())
        assertEquals(
            petNames,
            setOf(
                "Bambi",
                "Willow",
                "Tori",
                "Chewie",
                "Ginny",
                "Maisie",
                "Pineapple",
                "Gizmo",
                "Bug",
                "Arrayah",
                "Coffee",
            ),
        )

        val recentAtBats = assertNotNull(
            item["recentAtBats"]?.asLOrNull()?.mapNotNull { it.asSOrNull() },
            """Missing item attribute "recentAtBats"!""",
        )
        assertEquals(listOf("Single", "Base on balls", "Double"), recentAtBats)

        val cardsInHand = assertNotNull(
            item["cardsInHand"]?.asSsOrNull()?.toSet(),
            """Missing item attribute "cardsInHand"!""",
        )
        assertEquals(setOf("5 of Clubs", "King of Hearts", "8 of Spades"), cardsInHand)
    }
}
