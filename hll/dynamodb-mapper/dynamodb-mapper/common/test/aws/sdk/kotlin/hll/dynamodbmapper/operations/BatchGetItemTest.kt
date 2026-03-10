/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.*
import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.DdbLocalTest
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.ByteArrayValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.smithy.kotlin.runtime.testing.BeforeAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class BatchGetItemTest : DdbLocalTest() {
    companion object {
        private const val SHUTTLES_TABLE_NAME = "shuttles-table"
        private data class Shuttle(var name: String = "", var launchYear: Int = 0)
        private val shuttleConverter = SimpleItemConverter(
            ::Shuttle,
            { this },
            AttributeDescriptor("name", Shuttle::name, Shuttle::name::set, StringValueConverter),
            AttributeDescriptor("launchYear", Shuttle::launchYear, Shuttle::launchYear::set, NumberValueConverters.Int),
        )
        private val shuttleSchema = ItemSchema(shuttleConverter, KeySpec.string("name"))

        private const val HYPERCARS_TABLE_NAME = "hypercars"
        private data class HyperCar(var make: String = "", var model: String = "", var modelYear: Int = 0)
        private val hyperCarConverter = SimpleItemConverter(
            ::HyperCar,
            { this },
            AttributeDescriptor("make", HyperCar::make, HyperCar::make::set, StringValueConverter),
            AttributeDescriptor("model", HyperCar::model, HyperCar::model::set, StringValueConverter),
            AttributeDescriptor("modelYear", HyperCar::modelYear, HyperCar::modelYear::set, NumberValueConverters.Int),
        )
        private val hyperCarsSchema = ItemSchema(
            hyperCarConverter,
            partitionKey = KeySpec.string("make"),
            sortKey = KeySpec.string("model"),
        )

        private const val BIGDATA_TABLE_NAME = "bigdata"
        private data class BigData(var id: String = "", var dataset: ByteArray = byteArrayOf()) {
            override fun equals(other: Any?) = other is BigData && other.id == id && other.dataset contentEquals dataset
            override fun hashCode() = id.hashCode() * dataset.contentHashCode()
        }
        private val bigDataConverter = SimpleItemConverter(
            ::BigData,
            { this },
            AttributeDescriptor("id", BigData::id, BigData::id::set, StringValueConverter),
            AttributeDescriptor("dataset", BigData::dataset, BigData::dataset::set, ByteArrayValueConverter),
        )
        private val bigDataSchema = ItemSchema(bigDataConverter, KeySpec.string("id"))
        private val bigDataIds = (0..<100).map { "id-${it.toString().padStart(2, '0')}" } // 100 items
        private val bigPayload = ByteArray(300 * 1024) { it.toByte() } // 300KB each
    }

    @BeforeAll
    fun setUp() = runTest {
        createTable(
            SHUTTLES_TABLE_NAME,
            shuttleSchema,
            itemOf("name" to "Enterprise", "launchYear" to 1976),
            itemOf("name" to "Columbia", "launchYear" to 1981),
            itemOf("name" to "Challenger", "launchYear" to 1983),
            itemOf("name" to "Discovery", "launchYear" to 1984),
            itemOf("name" to "Atlantis", "launchYear" to 1985),
            itemOf("name" to "Endeavour", "launchYear" to 1992),
        )

        createTable(
            HYPERCARS_TABLE_NAME,
            hyperCarsSchema,
            itemOf("make" to "Mercedes-Benz", "model" to "300 SL", "modelYear" to 1954),
            itemOf("make" to "Lamborghini", "model" to "Miura", "modelYear" to 1967),
            itemOf("make" to "McLaren", "model" to "F1", "modelYear" to 1993),
            itemOf("make" to "Bugatti", "model" to "Veyron", "modelYear" to 2005),
        )

        createTable(
            BIGDATA_TABLE_NAME,
            bigDataSchema,
            bigDataIds.map { id -> itemOf("id" to id, "dataset" to bigPayload) },
        )
    }

    @Test
    fun testBatchGetItem() = runTest {
        val mapper = mapper()
        val shuttlesTable = mapper.getTable(SHUTTLES_TABLE_NAME, shuttleSchema)
        val hyperCarsTable = mapper.getTable(HYPERCARS_TABLE_NAME, hyperCarsSchema)

        val resp = mapper.batchGetItem {
            table(shuttlesTable) {
                keys = listOf(Key("Discovery"), Key("Endeavour"))
            }

            table(hyperCarsTable) {
                key(Key("Lamborghini"), Key("Miura"))
                key(Key("Bugatti"), Key("Veyron"))
                key(Key("McLaren"), Key("F1"))
            }
        }

        val expectedShuttles = setOf(Shuttle("Discovery", 1984), Shuttle("Endeavour", 1992))
        val actualShuttles = resp.table(shuttlesTable).items.toSet()
        assertEquals(expectedShuttles, actualShuttles)

        val expectedHyperCars = setOf(
            HyperCar("Lamborghini", "Miura", 1967),
            HyperCar("Bugatti", "Veyron", 2005),
            HyperCar("McLaren", "F1", 1993),
        )
        val actualHyperCars = resp.table(hyperCarsTable).items.toSet()
        assertEquals(expectedHyperCars, actualHyperCars)
    }

    @Test
    fun testBatchGetReallyBigItems() = runTest {
        val mapper = mapper()
        val bigDataTable = mapper.getTable(BIGDATA_TABLE_NAME, bigDataSchema)
        val resp = mapper.batchGetItem {
            table(bigDataTable) {
                keys = bigDataIds.map(::Key)
            }
        }

        val bigDataResp = resp.table(bigDataTable)

        val processedIds = bigDataResp.items.map { it.id }
        assertEquals(54, processedIds.size) // only 54 items fit in the 16MB maximum response size

        val unprocessedIds = bigDataResp.unprocessedKeys.map { it.value1 }
        assertEquals(46, unprocessedIds.size) // the remaining items are returned as "unprocessed"

        val actualIds = (processedIds + unprocessedIds).sorted()
        assertContentEquals(bigDataIds, actualIds)
    }
}
