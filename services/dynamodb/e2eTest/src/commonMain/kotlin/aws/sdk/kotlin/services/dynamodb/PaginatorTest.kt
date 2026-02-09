/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.dynamodb

import aws.sdk.kotlin.services.dynamodb.model.*
import aws.sdk.kotlin.services.dynamodb.paginators.scanPaginated
import aws.sdk.kotlin.services.dynamodb.waiters.waitUntilTableExists
import aws.smithy.kotlin.runtime.util.Uuid
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class PaginatorTest {
    private lateinit var client: DynamoDbClient
    private val table = "testTable-${Uuid.random()}"

    @BeforeTest
    fun setUp() = runBlocking {
        client = DynamoDbClient { region = "us-west-2" }
        
        if (!client.tableExists(table)) {
            client.createTable {
                tableName = table
                attributeDefinitions = listOf(
                    AttributeDefinition {
                        attributeName = "Artist"
                        attributeType = ScalarAttributeType.S
                    },
                    AttributeDefinition {
                        attributeName = "SongTitle"
                        attributeType = ScalarAttributeType.S
                    },
                )
                keySchema = listOf(
                    KeySchemaElement {
                        attributeName = "Artist"
                        keyType = KeyType.Hash
                    },
                    KeySchemaElement {
                        attributeName = "SongTitle"
                        keyType = KeyType.Range
                    },
                )
                provisionedThroughput = ProvisionedThroughput {
                    readCapacityUnits = 5
                    writeCapacityUnits = 5
                }
                tableClass = TableClass.Standard
            }

            client.waitUntilTableExists {
                tableName = table
            }
        }
    }

    @AfterTest
    fun cleanUp() = runBlocking {
        if (client.tableExists(table)) {
            client.deleteTable {
                tableName = table
            }
        }
        client.close()
    }

    @Test
    fun scanPaginatedRespectsExclusiveStartKey() = runBlocking {
        withTimeout(20.seconds) {
            client.putItem {
            tableName = table
            item = mapOf(
                "Artist" to AttributeValue.S("Foo"),
                "SongTitle" to AttributeValue.S("Bar"),
            )
        }

        client.putItem {
            tableName = table
            item = mapOf(
                "Artist" to AttributeValue.S("Foo"),
                "SongTitle" to AttributeValue.S("Baz"),
            )
        }

        client.putItem {
            tableName = table
            item = mapOf(
                "Artist" to AttributeValue.S("Foo"),
                "SongTitle" to AttributeValue.S("Qux"),
            )
        }

        val results = mutableListOf<Map<String, AttributeValue>?>()

        client.scanPaginated {
            tableName = table
            exclusiveStartKey = mapOf(
                "Artist" to AttributeValue.S("Foo"),
                "SongTitle" to AttributeValue.S("Bar"),
            )
            limit = 1
        }.collect { scan ->
            if (scan.items?.isNotEmpty() == true) {
                results.add(scan.items.single())
            }
        }

        assertEquals(2, results.size)
        // NOTE: Items are returned in alphabetical order
        assertEquals((AttributeValue.S("Baz")), results[0]?.get("SongTitle"))
        assertEquals((AttributeValue.S("Qux")), results[1]?.get("SongTitle"))
        }
    }
}

/**
 * Checks if a Dynamo DB table exists based on its name
 */
private suspend fun DynamoDbClient.tableExists(name: String): Boolean = this.listTables {}.tableNames?.contains(name) ?: false
