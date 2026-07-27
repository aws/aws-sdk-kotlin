/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConvertersTest
import aws.smithy.kotlin.runtime.time.Instant
import kotlin.test.Test

private val WHOLE_TIME = Instant.fromEpochSeconds(1234567890L) // 2009-02-13T23:31:30Z
private val MS_TIME = Instant.fromEpochSeconds(1234567890L, 123000000) // 2009-02-13T23:31:30.123Z
private val MICRO_TIME = Instant.fromEpochSeconds(1234567890L, 123456000) // 2009-02-13T23:31:30.123456Z
private val NS_TIME = Instant.fromEpochSeconds(1234567890L, 123456789) // 2009-02-13T23:31:30.123456789Z

class InstantValueConverterTest : ValueConvertersTest() {
    @Test
    fun testEpochS() = given(InstantValueConverter.EpochS) {
        WHOLE_TIME inDdbIs 1234567890L
        NS_TIME inDdbIs 1234567890L whenGoing Direction.TO_ATTRIBUTE_VALUE
    }

    @Test
    fun testEpochMs() = given(InstantValueConverter.EpochMs) {
        WHOLE_TIME inDdbIs 1234567890000L
        NS_TIME inDdbIs 1234567890123L whenGoing Direction.TO_ATTRIBUTE_VALUE
        MS_TIME inDdbIs 1234567890123L whenGoing Direction.FROM_ATTRIBUTE_VALUE
    }

    @Test
    fun testIso8601() = given(InstantValueConverter.Iso8601) {
        WHOLE_TIME inDdbIs "2009-02-13T23:31:30Z"
        MS_TIME inDdbIs "2009-02-13T23:31:30.123Z"
        MICRO_TIME inDdbIs "2009-02-13T23:31:30.123456Z"
        NS_TIME inDdbIs "2009-02-13T23:31:30.123456789Z"
    }
}
