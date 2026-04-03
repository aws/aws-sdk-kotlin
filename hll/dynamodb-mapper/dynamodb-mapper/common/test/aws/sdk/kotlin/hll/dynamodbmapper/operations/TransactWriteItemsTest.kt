/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.*
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.DdbLocalTest
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.sdk.kotlin.services.dynamodb.model.ReturnValuesOnConditionCheckFailure
import aws.sdk.kotlin.services.dynamodb.model.TransactionCanceledException
import aws.smithy.kotlin.runtime.testing.BeforeAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.assertThrows
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TransactWriteItemsTest : DdbLocalTest() {
    companion object {
        private const val ARTISTS_TABLE_NAME = "artists-table"
        private data class Artist(var name: String = "", var recordLabel: String = "")
        private val artistConverter = SimpleItemConverter(
            ::Artist,
            { this },
            AttributeDescriptor("name", Artist::name, Artist::name::set, StringValueConverter),
            AttributeDescriptor("recordLabel", Artist::recordLabel, Artist::recordLabel::set, StringValueConverter),
        )
        private val artistSchema = ItemSchema(artistConverter, KeySpec.string("name"))

        private const val SONGS_TABLE_NAME = "songs-table"
        private data class Song(var artist: String = "", var title: String = "", var year: Int = 0)
        private val songConverter = SimpleItemConverter(
            ::Song,
            { this },
            AttributeDescriptor("artist", Song::artist, Song::artist::set, StringValueConverter),
            AttributeDescriptor("title", Song::title, Song::title::set, StringValueConverter),
            AttributeDescriptor("year", Song::year, Song::year::set, NumberValueConverters.Int),
        )
        private val songSchema = ItemSchema(songConverter, KeySpec.string("artist"), KeySpec.string("title"))
    }

    @BeforeAll
    fun setUp() = runTest {
        createTable(ARTISTS_TABLE_NAME, artistSchema)
        createTable(SONGS_TABLE_NAME, songSchema)
    }

    @AfterTest
    fun truncateTables() = runTest {
        truncateTable(ARTISTS_TABLE_NAME)
        truncateTable(SONGS_TABLE_NAME)
    }

    @Test
    fun testTransactWriteItems() = runTest {
        val mapper = mapper()
        val artistsTable = mapper.getTable(ARTISTS_TABLE_NAME, artistSchema)
        val songsTable = mapper.getTable(SONGS_TABLE_NAME, songSchema)

        mapper.transactWriteItems {
            table(artistsTable) {
                put(Artist("Beyoncé", "Columbia Records"))
                put(Artist("Madonna", "Warner Brothers")) // Old record label
            }

            table(songsTable) {
                put(Song("Beyoncé", "Single Ladies", 2008)) // Missing full title
                put(Song("Beyoncé", "Break My Soul", 2022))

                put(Song("Madonna", "Hung Up", 2005))
                put(Song("Madonna", "4 Minutes", 2018)) // Whoops, wrong year!
            }
        }

        assertEquals(2, tableSize(ARTISTS_TABLE_NAME))
        assertEquals(4, tableSize(SONGS_TABLE_NAME))

        mapper.transactWriteItems {
            table(artistsTable) {
                update(Key("Madonna")) {
                    condition { attr["recordLabel"] eq "Warner Brothers" }
                    update {
                        set { attr["recordLabel"] = "Interscope Records" }
                    }
                }

                conditionCheck(Key("Beyoncé")) {
                    condition { attr["recordLabel"] eq "Columbia Records" }
                }
            }

            table(songsTable) {
                delete(Key("Beyoncé"), Key("Single Ladies")) // Cannot update primary key value; delete and recreate
                put(Song("Beyoncé", "Single Ladies (Put a Ring on It)", 2008))

                update(Key("Madonna"), Key("4 Minutes")) {
                    update {
                        set { attr["year"] = 2008 }
                    }
                }
            }
        }

        val initialResp = mapper.transactGetItems {
            table(artistsTable) {
                key(Key("Beyoncé"))
                key(Key("Madonna"))
            }

            table(songsTable) {
                key(Key("Beyoncé"), Key("Single Ladies (Put a Ring on It)"))
                key(Key("Beyoncé"), Key("Break My Soul"))
                key(Key("Madonna"), Key("Hung Up"))
                key(Key("Madonna"), Key("4 Minutes"))
            }
        }

        val artists = initialResp.table(artistsTable).items.iterator()
        val songs = initialResp.table(songsTable).items.iterator()

        assertEquals(Artist("Beyoncé", "Columbia Records"), artists.next())
        assertEquals(Artist("Madonna", "Interscope Records"), artists.next())
        assertFalse(artists.hasNext())

        assertEquals(Song("Beyoncé", "Single Ladies (Put a Ring on It)", 2008), songs.next())
        assertEquals(Song("Beyoncé", "Break My Soul", 2022), songs.next())
        assertEquals(Song("Madonna", "Hung Up", 2005), songs.next())
        assertEquals(Song("Madonna", "4 Minutes", 2008), songs.next())
        assertFalse(songs.hasNext())
    }

    @Test
    fun testTransactWriteItemsWithFailedCondition() = runTest {
        val mapper = mapper()
        val artistsTable = mapper.getTable(ARTISTS_TABLE_NAME, artistSchema)

        mapper.transactWriteItems {
            table(artistsTable) {
                put(Artist("Beyoncé", "Columbia Records"))
                put(Artist("Madonna", "Warner Brothers"))
            }
        }

        assertEquals(2, tableSize(ARTISTS_TABLE_NAME))

        val exception = assertThrows<TransactionCanceledException> {
            mapper.transactWriteItems {
                table(artistsTable) {
                    update(Key("Madonna")) {
                        returnValuesOnConditionCheckFailure = ReturnValuesOnConditionCheckFailure.AllOld
                        condition { attr["recordLabel"] eq "Interscope Records" } // Condition check will fail
                        update {
                            set { attr["recordLabel"] = "Warner Brothers" }
                        }
                    }

                    delete(Key("Beyoncé")) // Will be skipped since transaction fails
                }
            }
        }

        val reasons = exception.cancellationReasons.orEmpty().filterNot { it.code == "None" }
        assertEquals(1, reasons.size)
        val reason = reasons.single()
        assertEquals("ConditionalCheckFailed", reason.code)
        assertEquals("Madonna", reason.item?.get("name")?.asS())
        assertEquals("Warner Brothers", reason.item?.get("recordLabel")?.asS())
    }
}
