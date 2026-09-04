/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.smithy.kotlin.runtime.time.Instant
import org.example.dynamodbmapper.generatedschemas.NullableItemConverter
import kotlin.test.Test
import kotlin.test.assertEquals

public class NullableItemTest {
    @Test
    fun converterTest() {
        val nullable = NullableItem(
            id = 1,
            string = null,
            byte = null,
            int = 5,
            instant = Instant.now(),
        )

        val item = NullableItemConverter.convertRight(nullable)
        val converted = NullableItemConverter.convertLeft(item)
        assertEquals(nullable, converted)
    }
}
