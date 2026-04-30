/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.Key
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.model.Table
import aws.sdk.kotlin.services.dynamodb.model.DynamoDbException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TransactGetItemsTest : CrossTableGetItemsTestBase() {
    override suspend fun executeGetItem(
        mapper: DynamoDbMapper,
        shuttlesTable: Table.PartitionKey<Shuttle, KeyType.Key1<String>>,
        shuttleKeys: List<KeyType.Key1<String>>,
        hyperCarsTable: Table.CompositeKey<HyperCar, KeyType.Key1<String>, KeyType.Key1<String>>,
        hyperCarKeys: List<Pair<KeyType.Key1<String>, KeyType.Key1<String>>>,
    ): Pair<Set<Shuttle?>, Set<HyperCar?>> {
        val resp = mapper.transactGetItems {
            table(shuttlesTable) { keys(shuttleKeys) }
            table(hyperCarsTable) { keys(hyperCarKeys) }
        }

        val shuttles = resp.table(shuttlesTable).items.toSet()
        val hyperCars = resp.table(hyperCarsTable).items.toSet()
        return shuttles to hyperCars
    }

    @Test
    fun testTransactGetReallyBigItems() = runTest {
        val mapper = mapper()
        val bigDataTable = mapper.getTable(bigDataTableName, bigDataSchema)

        assertFailsWith<DynamoDbException> {
            mapper.transactGetItems {
                table(bigDataTable) {
                    keys = bigDataIds.map(::Key)
                }
            }
        }
    }
}
