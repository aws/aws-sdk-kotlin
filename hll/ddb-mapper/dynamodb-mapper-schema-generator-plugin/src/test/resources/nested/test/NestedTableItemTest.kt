/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import org.example.dynamodbmapper.generatedschemas.ProductConverter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NestedTableItemTest {
    @Test
    fun testNestedDynamoDbItemRoundTrip() {
        val product = Product(
            sku = "ABC-123",
            manufacturer = Manufacturer(id = 42, name = "Acme"),
        )

        val item = ProductConverter.convertRight(product)

        // The nested @DynamoDbItem is stored as a DynamoDB Map (M)
        val nested = item["manufacturer"] as AttributeValue.M

        // The nested item's partition key field is serialized as an ordinary attribute, honoring @DynamoDbAttribute
        assertTrue(nested.value.containsKey("mfrId"))
        assertEquals(AttributeValue.N("42"), nested.value["mfrId"])
        assertEquals(AttributeValue.S("Acme"), nested.value["name"])

        val roundTripped = ProductConverter.convertLeft(item)
        assertEquals(product, roundTripped)
    }
}
