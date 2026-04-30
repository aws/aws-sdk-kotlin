/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.*
import aws.sdk.kotlin.hll.dynamodbmapper.model.Table
import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.DdbLocalTest
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.ByteArrayValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.smithy.kotlin.runtime.testing.BeforeAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class CrossTableGetItemsTestBase : DdbLocalTest() {
    protected val shuttlesTableName = "shuttles-table"
    protected data class Shuttle(var name: String = "", var launchYear: Int = 0)
    protected val shuttleConverter = SimpleItemConverter(
        ::Shuttle,
        { this },
        AttributeDescriptor("name", Shuttle::name, Shuttle::name::set, StringValueConverter),
        AttributeDescriptor("launchYear", Shuttle::launchYear, Shuttle::launchYear::set, NumberValueConverters.Int),
    )
    protected val shuttleSchema = ItemSchema(shuttleConverter, KeySpec.string("name"))

    protected val hyperCarsTableName = "hypercars"
    protected data class HyperCar(var make: String = "", var model: String = "", var modelYear: Int = 0)
    protected val hyperCarConverter = SimpleItemConverter(
        ::HyperCar,
        { this },
        AttributeDescriptor("make", HyperCar::make, HyperCar::make::set, StringValueConverter),
        AttributeDescriptor("model", HyperCar::model, HyperCar::model::set, StringValueConverter),
        AttributeDescriptor("modelYear", HyperCar::modelYear, HyperCar::modelYear::set, NumberValueConverters.Int),
    )
    protected val hyperCarsSchema = ItemSchema(
        hyperCarConverter,
        partitionKey = KeySpec.string("make"),
        sortKey = KeySpec.string("model"),
    )

    protected val bigDataTableName = "bigdata"
    protected data class BigData(var id: String = "", var dataset: ByteArray = byteArrayOf()) {
        override fun equals(other: Any?) = other is BigData && other.id == id && other.dataset contentEquals dataset
        override fun hashCode() = id.hashCode() * dataset.contentHashCode()
    }
    protected val bigDataConverter = SimpleItemConverter(
        ::BigData,
        { this },
        AttributeDescriptor("id", BigData::id, BigData::id::set, StringValueConverter),
        AttributeDescriptor("dataset", BigData::dataset, BigData::dataset::set, ByteArrayValueConverter),
    )
    protected val bigDataSchema = ItemSchema(bigDataConverter, KeySpec.string("id"))
    protected val bigDataIds = (0..<100).map { "id-${it.toString().padStart(2, '0')}" } // 100 items
    protected val bigPayload = ByteArray(300 * 1024) { it.toByte() } // 300KB each

    @BeforeAll
    fun setUp() = runTest {
        createTable(
            shuttlesTableName,
            shuttleSchema,
            itemOf("name" to "Enterprise", "launchYear" to 1976),
            itemOf("name" to "Columbia", "launchYear" to 1981),
            itemOf("name" to "Challenger", "launchYear" to 1983),
            itemOf("name" to "Discovery", "launchYear" to 1984),
            itemOf("name" to "Atlantis", "launchYear" to 1985),
            itemOf("name" to "Endeavour", "launchYear" to 1992),
        )

        createTable(
            hyperCarsTableName,
            hyperCarsSchema,
            itemOf("make" to "Mercedes-Benz", "model" to "300 SL", "modelYear" to 1954),
            itemOf("make" to "Lamborghini", "model" to "Miura", "modelYear" to 1967),
            itemOf("make" to "McLaren", "model" to "F1", "modelYear" to 1993),
            itemOf("make" to "Bugatti", "model" to "Veyron", "modelYear" to 2005),
        )

        createTable(
            bigDataTableName,
            bigDataSchema,
            bigDataIds.map { id -> itemOf("id" to id, "dataset" to bigPayload) },
        )
    }

    @Test
    fun testGetItem() = runTest {
        val mapper = mapper()
        val shuttlesTable = mapper.getTable(shuttlesTableName, shuttleSchema)
        val hyperCarsTable = mapper.getTable(hyperCarsTableName, hyperCarsSchema)

        val (actualShuttles, actualHyperCars) = executeGetItem(
            mapper,
            shuttlesTable,
            listOf(Key("Discovery"), Key("Endeavour")),
            hyperCarsTable,
            listOf(Key("Lamborghini") to Key("Miura"), Key("Bugatti") to Key("Veyron"), Key("McLaren") to Key("F1")),
        )

        val expectedShuttles = setOf(Shuttle("Discovery", 1984), Shuttle("Endeavour", 1992))
        assertEquals(expectedShuttles, actualShuttles)

        val expectedHyperCars = setOf(
            HyperCar("Lamborghini", "Miura", 1967),
            HyperCar("Bugatti", "Veyron", 2005),
            HyperCar("McLaren", "F1", 1993),
        )
        assertEquals(expectedHyperCars, actualHyperCars)
    }

    protected abstract suspend fun executeGetItem(
        mapper: DynamoDbMapper,
        shuttlesTable: Table.PartitionKey<Shuttle, KeyType.Key1<String>>,
        shuttleKeys: List<KeyType.Key1<String>>,
        hyperCarsTable: Table.CompositeKey<HyperCar, KeyType.Key1<String>, KeyType.Key1<String>>,
        hyperCarKeys: List<Pair<KeyType.Key1<String>, KeyType.Key1<String>>>,
    ): Pair<Set<Shuttle?>, Set<HyperCar?>>
}
