/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.BooleanValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterImpl
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private val ford = Car(1, "Ford", "Model T", 1928)
private val schwinn = Bike(2, "Schwinn", 10, false)

class HeterogeneousItemConverterTest {
    // Mirrors the constructor-based usage: subconverters are typed to the subclass, as generated converters are
    private val ctorConverter = HeterogeneousItemConverter(
        typeMapper = ::vehicleType,
        typeAttribute = "type",
        subConverters = mapOf(
            "car" to CarConverter,
            "bike" to BikeConverter,
        ),
    )

    private val builderConverter = HeterogeneousItemConverter<Vehicle>(typeAttribute = "type") {
        instanceOf("car", CarConverter)
        instanceOf("bike", BikeConverter)
    }

    @Test
    fun testConstructorRoundTrip() = assertRoundTrip(ctorConverter)

    @Test
    fun testBuilderRoundTrip() = assertRoundTrip(builderConverter)

    private fun assertRoundTrip(converter: ItemConverter<Vehicle>) {
        val vehicles = listOf(ford, schwinn)
        val items = vehicles.map(converter::convertRight)

        assertEquals(AttributeValue.S("car"), items[0]["type"])
        assertEquals(setOf("type", "id", "manufacturer", "model", "year"), items[0].keys)
        assertEquals(AttributeValue.S("bike"), items[1]["type"])
        assertEquals(setOf("type", "id", "manufacturer", "gears", "isElectric"), items[1].keys)

        assertEquals(vehicles, items.map(converter::convertLeft))
    }

    @Test
    fun testConstructorAcceptsSupertypeConverter() {
        val nested = HeterogeneousItemConverter(
            typeMapper = { _: Vehicle -> "vehicle" },
            typeAttribute = "kind",
            subConverters = mapOf("vehicle" to ctorConverter),
        )

        assertEquals(ford, nested.convertLeft(nested.convertRight(ford)))
    }

    @Test
    fun testBuilderMatchesFirstRegisteredSubtype() {
        val converter = HeterogeneousItemConverter<Vehicle>(typeAttribute = "kind") {
            instanceOf("car", CarConverter)
            instanceOf<Vehicle>("vehicle", ctorConverter) // catch-all for everything else
        }

        assertEquals(AttributeValue.S("car"), converter.convertRight(ford)["kind"])
        assertEquals(AttributeValue.S("vehicle"), converter.convertRight(schwinn)["kind"])
        assertEquals(listOf(ford, schwinn), listOf(ford, schwinn).map { converter.convertLeft(converter.convertRight(it)) })
    }

    @Test
    fun testUnmappedObjects() {
        val ctorPartial = HeterogeneousItemConverter<Vehicle>(::vehicleType, "type", mapOf("car" to CarConverter))
        val builderPartial = HeterogeneousItemConverter<Vehicle>("type") { instanceOf("car", CarConverter) }

        listOf(ctorPartial, builderPartial).forEach { converter ->
            assertFailsWith<IllegalArgumentException> { converter.convertRight(schwinn) }
        }
    }

    @Test
    fun testInvalidTypeAttributesOnRead() {
        val bikeItem = ctorConverter.convertRight(schwinn)
        val invalidItems = listOf(
            bikeItem - "type", // missing
            bikeItem + ("type" to AttributeValue.N("1")), // not a string
            bikeItem + ("type" to AttributeValue.S("unicycle")), // unknown
        )

        listOf(ctorConverter, builderConverter).forEach { converter ->
            invalidItems.forEach { item ->
                assertFailsWith<IllegalArgumentException> { converter.convertLeft(item.toItem()) }
            }
        }
        assertFailsWith<IllegalArgumentException> {
            HeterogeneousItemConverter<Vehicle>("type") { instanceOf("car", CarConverter) }.convertLeft(bikeItem)
        }
    }

    @Test
    fun testBuilderStripsTypeAttributeBeforeDelegating() {
        val converter = HeterogeneousItemConverter<Vehicle>(typeAttribute = "type") {
            instanceOf("car", StrictCarConverter)
        }

        assertEquals(ford, converter.convertLeft(converter.convertRight(ford)))
    }

    @Test
    fun testConstructorPassesTypeAttributeThroughToSubconverters() {
        // Pre-builder behavior: a subconverter may own the type attribute as long as its value agrees with typeMapper
        val converter = HeterogeneousItemConverter(
            typeMapper = Car::manufacturer,
            typeAttribute = "manufacturer",
            subConverters = mapOf("Ford" to CarConverter),
        )

        val item = converter.convertRight(ford)
        assertEquals(AttributeValue.S("Ford"), item["manufacturer"])
        assertEquals(ford, converter.convertLeft(item))
    }

    @Test
    fun testConstructorRejectsConflictingTypeAttribute() {
        val converter = HeterogeneousItemConverter(
            typeMapper = { _: Car -> "car" },
            typeAttribute = "manufacturer",
            subConverters = mapOf("car" to CarConverter),
        )

        assertFailsWith<IllegalArgumentException> { converter.convertRight(ford) }
    }

    @Test
    fun testBuilderRejectsSubconverterWritingTypeAttribute() {
        val converter = HeterogeneousItemConverter<Vehicle>(typeAttribute = "manufacturer") {
            instanceOf("Ford", CarConverter) // writes "manufacturer" even though the value matches
        }

        assertFailsWith<IllegalArgumentException> { converter.convertRight(ford) }
    }

    @Test
    fun testInvalidConfiguration() {
        assertFailsWith<IllegalArgumentException> { HeterogeneousItemConverter(::vehicleType, " ", mapOf("car" to CarConverter)) }
        assertFailsWith<IllegalArgumentException> { HeterogeneousItemConverter(::vehicleType, "type", mapOf()) }
        assertFailsWith<IllegalArgumentException> { HeterogeneousItemConverter<Vehicle>("") { instanceOf("car", CarConverter) } }
        assertFailsWith<IllegalArgumentException> { HeterogeneousItemConverter<Vehicle>("type") { } }
        assertFailsWith<IllegalArgumentException> {
            HeterogeneousItemConverter<Vehicle>("type") {
                instanceOf("car", CarConverter)
                instanceOf("car", BikeConverter)
            }
        }
    }

    @Test
    fun testExactTypeDoesNotMatchSubtypes() {
        val converter = HeterogeneousItemConverter<Vehicle>(typeAttribute = "type") {
            exactType("truck", TruckConverter)
        }

        assertEquals(AttributeValue.S("truck"), converter.convertRight(Truck(5))["type"])
        assertFailsWith<IllegalArgumentException> { converter.convertRight(TowTruck(6)) }
    }

    @Test
    fun testExactTypeThenInstanceOfSameClass() {
        val converter = HeterogeneousItemConverter<Vehicle>(typeAttribute = "type") {
            exactType("truck", TruckConverter)
            instanceOf("truckLike", TruckConverter)
        }

        assertEquals(AttributeValue.S("truck"), converter.convertRight(Truck(5))["type"])
        assertEquals(AttributeValue.S("truckLike"), converter.convertRight(TowTruck(6))["type"])
    }

    @Test
    fun testFirstMatchWinsRegardlessOfMatchKind() {
        val exactFirst = HeterogeneousItemConverter<Vehicle>(typeAttribute = "kind") {
            exactType("car", CarConverter)
            instanceOf<Vehicle>("vehicle", ctorConverter)
        }
        assertEquals(AttributeValue.S("car"), exactFirst.convertRight(ford)["kind"])
        assertEquals(AttributeValue.S("vehicle"), exactFirst.convertRight(schwinn)["kind"])

        // Shadowing by a supertype can't be detected at build time, so the earlier instanceOf wins
        val instanceFirst = HeterogeneousItemConverter<Vehicle>(typeAttribute = "kind") {
            instanceOf<Vehicle>("vehicle", ctorConverter)
            exactType("car", CarConverter)
        }
        assertEquals(AttributeValue.S("vehicle"), instanceFirst.convertRight(ford)["kind"])
    }

    @Test
    fun testUnreachableSameClassRegistrationsRejected() {
        val shadowed = listOf<HeterogeneousItemConverter.Builder<Vehicle>.() -> Unit>(
            {
                instanceOf("car", CarConverter)
                instanceOf("auto", StrictCarConverter)
            },
            {
                instanceOf("car", CarConverter)
                exactType("auto", StrictCarConverter)
            },
            {
                exactType("car", CarConverter)
                exactType("auto", StrictCarConverter)
            },
        )

        shadowed.forEach { block ->
            assertFailsWith<IllegalArgumentException> { HeterogeneousItemConverter("type", block) }
        }
    }
}

private sealed interface Vehicle

private data class Car(val id: Int, val manufacturer: String, val model: String, val year: Int) : Vehicle

private class CarBuilder {
    var id: Int? = null
    var manufacturer: String? = null
    var model: String? = null
    var year: Int? = null

    fun build() = Car(id!!, manufacturer!!, model!!, year!!)
}

private val carDescriptors = arrayOf(
    AttributeDescriptor("id", Car::id, CarBuilder::id::set, NumberValueConverters.Int),
    AttributeDescriptor("manufacturer", Car::manufacturer, CarBuilder::manufacturer::set, StringValueConverter),
    AttributeDescriptor("model", Car::model, CarBuilder::model::set, StringValueConverter),
    AttributeDescriptor("year", Car::year, CarBuilder::year::set, NumberValueConverters.Int),
)

private object CarConverter : ItemConverter<Car> by SimpleItemConverter(::CarBuilder, CarBuilder::build, *carDescriptors)

private object StrictCarConverter : ItemConverter<Car> by SimpleItemConverter(
    ::CarBuilder,
    CarBuilder::build,
    *carDescriptors,
    unknownValueHandling = UnknownValueHandling.ThrowException,
)

private data class Bike(val id: Int, val manufacturer: String, val gears: Int, val isElectric: Boolean) : Vehicle

private class BikeBuilder {
    var id: Int? = null
    var manufacturer: String? = null
    var gears: Int? = null
    var isElectric: Boolean? = null

    fun build() = Bike(id!!, manufacturer!!, gears!!, isElectric!!)
}

private object BikeConverter : ItemConverter<Bike> by SimpleItemConverter(
    ::BikeBuilder,
    BikeBuilder::build,
    AttributeDescriptor("id", Bike::id, BikeBuilder::id::set, NumberValueConverters.Int),
    AttributeDescriptor("manufacturer", Bike::manufacturer, BikeBuilder::manufacturer::set, StringValueConverter),
    AttributeDescriptor("gears", Bike::gears, BikeBuilder::gears::set, NumberValueConverters.Int),
    AttributeDescriptor("isElectric", Bike::isElectric, BikeBuilder::isElectric::set, BooleanValueConverter),
)

private open class Truck(val id: Int) : Vehicle

private class TowTruck(id: Int) : Truck(id)

private val TruckConverter: ItemConverter<Truck> = ConverterImpl(
    { truck -> itemOf("id" to AttributeValue.N(truck.id.toString())) },
    { item -> Truck(item.getValue("id").asN().toInt()) },
)

private fun vehicleType(vehicle: Vehicle) = when (vehicle) {
    is Car -> "car"
    is Bike -> "bike"
    is Truck -> "truck"
}
