/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.hll.dynamodbmapper.values.TTLValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConvertersTest
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TTLValueConverterTest : ValueConvertersTest() {
    @Test
    fun testTTLConversion() {
        val manualClock = ManualClock(Instant.fromEpochSeconds(0))
        val ttl = 7.days.inWholeSeconds
        val converter = TTLValueConverter(ttl, manualClock)

        given(converter) {
            7.days.inWholeSeconds inDdbIs theSame
        }

        manualClock.advance(2.days)

        given(converter) {
            9.days.inWholeSeconds inDdbIs theSame
        }
    }

    @Test
    fun testInvalidLifetime() = runTest {
        assertFailsWith<IllegalArgumentException> {
            TTLValueConverter(-1)
        }
        assertFailsWith<IllegalArgumentException> {
            TTLValueConverter(0)
        }
        TTLValueConverter(1)
    }
}
