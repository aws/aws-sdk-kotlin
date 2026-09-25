/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import org.example.dynamodbmapper.generatedschemas.DeepListHolderConverter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeepNestingTest {
    @Test
    fun testDeepNestingRoundTrip() {
        val holder = DeepListHolder(
            id = 1,
            data = listOf(
                mapOf(
                    "groupA" to listOf(Leaf("a", 1), Leaf("b", 2)),
                    "groupB" to listOf(Leaf("c", 3)),
                ),
                mapOf("groupC" to emptyList()),
            ),
        )

        val item = DeepListHolderConverter.convertRight(holder)

        // The attribute is a List (L) of Maps (M) of Lists (L) of Maps (M) — the nested Leaf items
        val outerList = item["data"] as AttributeValue.L
        val firstMap = outerList.value.first() as AttributeValue.M
        val innerList = firstMap.value["groupA"] as AttributeValue.L
        assertTrue(innerList.value.all { it is AttributeValue.M })

        val roundTripped = DeepListHolderConverter.convertLeft(item)
        assertEquals(holder, roundTripped)
    }
}
