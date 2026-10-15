/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import org.example.dynamodbmapper.generatedschemas.AccountConverter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NestedItemWithTtlCounterTest {
    @Test
    fun testNestedTtlAndCounterAreInert() {
        // Sentinel values that would obviously change if the TTL/counter interceptors acted on them:
        //  - a TTL interceptor would overwrite expiresAt with (now + 3600), not keep 111L
        //  - a counter interceptor would increment accessCount, not keep 7L
        val account = Account(
            accountId = "acct-1",
            session = Session(id = 1, expiresAt = 111L, accessCount = 7L),
        )

        val item = AccountConverter.convertRight(account)
        val nested = item["session"] as AttributeValue.M

        // The nested item's TTL and counter fields are serialized as ordinary attributes with their exact values
        assertEquals(AttributeValue.N("111"), nested.value["expiresAt"])
        assertEquals(AttributeValue.N("7"), nested.value["accessCount"])

        val roundTripped = AccountConverter.convertLeft(item)
        assertEquals(account, roundTripped)
        assertEquals(111L, roundTripped.session.expiresAt)
        assertEquals(7L, roundTripped.session.accessCount)
        assertTrue(item.containsKey("session"))
    }
}
