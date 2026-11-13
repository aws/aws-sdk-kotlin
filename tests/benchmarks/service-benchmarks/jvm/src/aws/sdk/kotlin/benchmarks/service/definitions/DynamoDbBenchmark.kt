/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.definitions

import aws.sdk.kotlin.benchmarks.service.Common
import aws.sdk.kotlin.services.dynamodb.*
import aws.sdk.kotlin.services.dynamodb.model.*
import aws.sdk.kotlin.services.dynamodb.waiters.waitUntilTableExists

class DynamoDbBenchmark : ServiceBenchmark<DynamoDbClient> {
    private val table = Common.random("sdk-benchmark-table-")

    override suspend fun client() = DynamoDbClient.fromEnvironment {
        retryStrategy = Common.noRetries
        telemetryProvider = Common.telemetryProvider
        httpClient {
            telemetryProvider = Common.telemetryProvider
        }
    }

    override suspend fun setup(client: DynamoDbClient) {
        client.createTable {
            tableName = table
            billingMode = BillingMode.PayPerRequest
            attributeDefinitions = listOf(
                AttributeDefinition {
                    attributeName = "id"
                    attributeType = ScalarAttributeType.S
                },
            )
            keySchema = listOf(
                KeySchemaElement {
                    attributeName = "id"
                    keyType = KeyType.Hash
                },
            )
        }
        client.waitUntilTableExists { tableName = table }
    }

    override val operations get() = listOf(getItemBenchmark, putItemBenchmark)

    override suspend fun tearDown(client: DynamoDbClient) {
        client.deleteTable { tableName = table }
    }

    private val getItemBenchmark = object : AbstractOperationBenchmark<DynamoDbClient>("GetItem") {
        private val knownId = randomAttr()

        override suspend fun setup(client: DynamoDbClient) {
            client.putItem {
                tableName = table
                item = mapOf(
                    "id" to knownId,
                    "value" to randomAttr(),
                )
            }
        }

        override suspend fun transact(client: DynamoDbClient) {
            client.getItem {
                tableName = table
                key = mapOf("id" to knownId)
            }
        }
    }

    private val putItemBenchmark = object : AbstractOperationBenchmark<DynamoDbClient>("PutItem") {
        override suspend fun transact(client: DynamoDbClient) {
            client.putItem {
                tableName = table
                item = mapOf(
                    "id" to randomAttr(),
                    "value" to randomAttr(),
                )
            }
        }
    }
}

private fun randomAttr() = AttributeValue.S(Common.random())
