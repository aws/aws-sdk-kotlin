/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import org.example.dynamodbmapper.generatedschemas.AnimalConverter
import org.example.dynamodbmapper.generatedschemas.AuditRecordConverter
import org.example.dynamodbmapper.generatedschemas.EntitiesConverter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OpenAndAdHocFamiliesTest {
    @Test
    fun testRegisteredSubtypes() {
        val puppy = Puppy(ageWeeks = 8).apply {
            breed = "Beagle"
            name = "Snoopy"
        }
        val item = AnimalConverter.convertRight(puppy)
        assertEquals(AttributeValue.S("puppy"), item["kind"])

        val read = assertIs<Puppy>(AnimalConverter.convertLeft(item))
        assertEquals(8, read.ageWeeks)
        assertEquals("Beagle", read.breed)
        assertEquals("Snoopy", read.name)
    }

    @Test
    fun testInstanceOfSubtypeCatchesUnregisteredSubclass() {
        val item = AnimalConverter.convertRight(
            Poodle().apply {
                breed = "Standard"
                name = "Fifi"
            },
        )
        assertEquals(AttributeValue.S("dog"), item["kind"])

        val read = AnimalConverter.convertLeft(item)
        assertEquals(Dog::class, read::class)
        assertEquals("Standard", (read as Dog).breed)
    }

    @Test
    fun testBaseCatchesUnregisteredSubclass() {
        val item = AnimalConverter.convertRight(Cat(indoor = true).apply { name = "Tom" })
        assertEquals(AttributeValue.S("animal"), item["kind"])
        assertEquals(null, item["indoor"])

        val read = AnimalConverter.convertLeft(item)
        assertEquals(Animal::class, read::class)
        assertEquals("Tom", read.name)
    }

    @Test
    fun testAdHocFamily() {
        val user = UserEntity("USER#1", "PROFILE", "Alice", 3)
        val order = OrderEntity("USER#1", "ORDER#9", 1999, 1700000000)

        listOf(user to "user", order to "order").forEach { (entity, typeName) ->
            val item = EntitiesConverter.convertRight(entity)
            assertEquals(AttributeValue.S(typeName), item["entityType"])
            assertEquals(entity, EntitiesConverter.convertLeft(item))
        }
    }

    @Test
    fun testHeterogeneousAttribute() {
        val record = AuditRecord(
            entity = UserEntity("USER#1", "PROFILE", "Alice", 3),
            related = listOf(OrderEntity("USER#1", "ORDER#9", 1999, 1700000000)),
        )

        val item = AuditRecordConverter.convertRight(record)
        val entity = item["entity"] as AttributeValue.M
        assertEquals(AttributeValue.S("user"), entity.value["entityType"])

        assertEquals(record, AuditRecordConverter.convertLeft(item))
    }
}
