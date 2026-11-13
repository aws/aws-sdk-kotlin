/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import org.example.dynamodbmapper.generatedschemas.PersonConverter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NestedItemTest {
    @Test
    fun testNestedRoundTrip() {
        val person = Person(
            id = 1,
            name = "Steve",
            homeAddress = Address("123 Main St", "Springfield", "00001"),
            otherAddresses = listOf(
                Address("1 Elm St", "Portland", "00002"),
                Address("2 Oak St", "Portland", "00003"),
            ),
            addressesByLabel = mapOf("work" to Address("500 5th Ave", "New York", "10110")),
            mailingAddress = null,
        )

        val item = PersonConverter.convertRight(person)

        // A nested @DynamoDbMappable value is stored as a DynamoDB Map (M)
        assertTrue(item["homeAddress"] is AttributeValue.M)
        // A list of nested values is a List (L) of Maps (M)
        val list = item["otherAddresses"] as AttributeValue.L
        assertTrue(list.value.all { it is AttributeValue.M })
        // A map of nested values is a Map (M) of Maps (M)
        val map = item["addressesByLabel"] as AttributeValue.M
        assertTrue(map.value.values.all { it is AttributeValue.M })

        val roundTripped = PersonConverter.convertLeft(item)
        assertEquals(person, roundTripped)
    }
}
