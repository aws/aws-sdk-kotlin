/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.*
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.DdbLocalTest
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.smithy.kotlin.runtime.testing.BeforeAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BatchWriteItemTest : DdbLocalTest() {
    companion object {
        private const val PLAYERS_TABLE_NAME = "players-table"
        private data class Player(var teamName: String = "", var number: Int = 0, var name: String = "")
        private val playerConverter = SimpleItemConverter(
            ::Player,
            { this },
            AttributeDescriptor("teamName", Player::teamName, Player::teamName::set, StringValueConverter),
            AttributeDescriptor("number", Player::number, Player::number::set, NumberValueConverters.Int),
            AttributeDescriptor("name", Player::name, Player::name::set, StringValueConverter),
        )
        private val playerSchema = ItemSchema(playerConverter, KeySpec.string("teamName"), KeySpec.int("number"))

        private const val TEAMS_TABLE_NAME = "teams-table"
        private data class Team(var teamName: String = "", var location: String = "", var foundedYear: Int = 0)
        private val teamConverter = SimpleItemConverter(
            ::Team,
            { this },
            AttributeDescriptor("teamName", Team::teamName, Team::teamName::set, StringValueConverter),
            AttributeDescriptor("location", Team::location, Team::location::set, StringValueConverter),
            AttributeDescriptor("number", Team::foundedYear, Team::foundedYear::set, NumberValueConverters.Int),
        )
        private val teamSchema = ItemSchema(teamConverter, KeySpec.string("teamName"))
    }

    @BeforeAll
    fun setUp() = runTest {
        createTable(PLAYERS_TABLE_NAME, playerSchema)
        createTable(TEAMS_TABLE_NAME, teamSchema)
    }

    @Test
    fun testBatchWriteItem() = runTest {
        assertEquals(0, tableSize(PLAYERS_TABLE_NAME))
        assertEquals(0, tableSize(TEAMS_TABLE_NAME))

        val mapper = mapper()
        val playersTable = mapper.getTable(PLAYERS_TABLE_NAME, playerSchema)
        val teamsTable = mapper.getTable(TEAMS_TABLE_NAME, teamSchema)

        mapper.batchWriteItem {
            table(teamsTable) {
                putItem(Team("Mariners", "Seattle", 1977))
                putItem(Team("Angels", "Los Angeles", 1961))
                putItem(Team("Rangers", "Texas", 1961))
                putItem(Team("Astroids", "Houston", 1962))
                putItem(Team("Athletics", "Sacramento", 1901))
            }

            table(playersTable) {
                putItem(Player("Athletics", 5, "Jacob Wilson"))
                putItem(Player("Astroids", 3, "Jeremy Peña"))
                putItem(Player("Angels", 18, "Nolan Schanuel"))
                putItem(Player("Mariners", 44, "Julio Rodríguez"))
                putItem(Player("Rangers", 6, "Josh Jung"))
            }
        }

        assertEquals(5, tableSize(PLAYERS_TABLE_NAME))
        assertEquals(5, tableSize(TEAMS_TABLE_NAME))

        val originalTeamNames = teamsTable.scanPaginated { }.items().map { it.teamName }.toList()
        assertTrue("Astroids" in originalTeamNames)

        mapper.batchWriteItem {
            table(teamsTable) {
                deleteKey(Key("Astroids"))
                putItem(Team("Astros", "Houston", 1962))
            }

            table(playersTable) {
                deleteKey(Key("Astroids"), Key(3))
                putItem(Player("Astros", 3, "Jeremy Peña"))
            }
        }

        assertEquals(5, tableSize(PLAYERS_TABLE_NAME))
        assertEquals(5, tableSize(TEAMS_TABLE_NAME))

        val updatedTeamNames = teamsTable.scanPaginated { }.items().map { it.teamName }.toList()
        assertFalse("Astroids" in updatedTeamNames)
        assertTrue("Astros" in updatedTeamNames)
    }
}
