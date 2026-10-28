/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import org.example.dynamodbmapper.generatedschemas.GarageConverter
import org.example.dynamodbmapper.generatedschemas.VehicleConverter
import org.example.dynamodbmapper.generatedschemas.VehicleSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class VehiclesTest {
    @Test
    fun testRoundTrip() {
        val vehicles = listOf(
            Car(1, "Ford", "Model T") to "car",
            Bike(2, "Schwinn", 10) to "bike",
            Unicycle to "unicycle",
            Pickup(4, "Ford", 6) to "truck",
            Flatbed(5, "Mack", 20) to "truck",
        )

        vehicles.forEach { (vehicle, typeName) ->
            val item = VehicleConverter.convertRight(vehicle)
            assertEquals(AttributeValue.S(typeName), item["type"])
            assertEquals(vehicle, VehicleConverter.convertLeft(item))
        }
    }

    @Test
    fun testNestedFamilyWritesBothTypeAttributes() {
        val item = VehicleConverter.convertRight(Pickup(4, "Ford", 6))
        assertEquals(AttributeValue.S("truck"), item["type"])
        assertEquals(AttributeValue.S("pickup"), item["truckType"])
        assertEquals(AttributeValue.N("6"), item["bedLength"])
    }

    @Test
    fun testObjectSubtypeWritesItsProperties() {
        val item = VehicleConverter.convertRight(Unicycle)
        assertEquals(AttributeValue.N("0"), item["id"])
        assertEquals(AttributeValue.S("Generic"), item["manufacturer"])
    }

    @Test
    fun testUnknownTypeFails() {
        val item = mapOf("type" to AttributeValue.S("hovercraft"), "id" to AttributeValue.N("1")).toItem()
        assertFailsWith<IllegalArgumentException> { VehicleConverter.convertLeft(item) }
    }

    @Test
    fun testSchema() {
        assertSame(VehicleConverter, VehicleSchema.converter)
    }

    @Test
    fun testNestedUse() {
        val garage = Garage(
            name = "Main St",
            vehicles = listOf(Car(1, "Ford", "Model T"), Pickup(4, "Ford", 6), Unicycle),
            favorite = Bike(2, "Schwinn", 10),
        )

        val item = GarageConverter.convertRight(garage)
        val vehicles = item["vehicles"] as AttributeValue.L
        assertTrue(vehicles.value.all { it is AttributeValue.M })
        assertEquals(AttributeValue.S("truck"), (vehicles.value[1] as AttributeValue.M).value["type"])

        assertEquals(garage, GarageConverter.convertLeft(item))
        assertEquals(garage.copy(favorite = null), GarageConverter.convertLeft(GarageConverter.convertRight(garage.copy(favorite = null))))
    }
}
