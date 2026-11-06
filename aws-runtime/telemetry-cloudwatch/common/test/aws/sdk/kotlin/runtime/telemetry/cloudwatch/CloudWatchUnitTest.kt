/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.telemetry.cloudwatch

import aws.sdk.kotlin.services.cloudwatch.model.StandardUnit
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit translation, which is untypechecked on both ends: the input is a free-form string and the
 * output only becomes a string again on the wire, so a wrong mapping is a mislabelled chart rather
 * than an error anyone sees.
 */
class CloudWatchUnitTest {
    /** The units the SDK's own instruments actually emit, which are the ones that must be right. */
    @Test
    fun testSdkEmittedUnitsMap() {
        assertEquals(StandardUnit.Milliseconds, cloudWatchUnit("ms"))
        assertEquals(StandardUnit.Seconds, cloudWatchUnit("s"))
        assertEquals(StandardUnit.Bytes, cloudWatchUnit("By"))
        assertEquals(StandardUnit.Count, cloudWatchUnit("1"))
    }

    /**
     * Case is normalised, because UCUM spells bytes `"By"` while hand-written instrumentation
     * routinely writes `"bytes"`.
     */
    @Test
    fun testMappingIsCaseInsensitive() {
        assertEquals(StandardUnit.Bytes, cloudWatchUnit("BYTES"))
        assertEquals(StandardUnit.Milliseconds, cloudWatchUnit("Milliseconds"))
    }

    /**
     * An absent or dimensionless unit becomes `Count` rather than `None`, which labels the axis of the
     * counters that dominate this category.
     */
    @Test
    fun testAbsentUnitBecomesCount() {
        assertEquals(StandardUnit.Count, cloudWatchUnit(null))
        assertEquals(StandardUnit.Count, cloudWatchUnit(""))
    }

    /**
     * An unrecognised unit degrades to `None` instead of throwing or dropping the datum: the value is
     * still correct and still charted, and losing the metric would be the worse outcome.
     */
    @Test
    fun testUnknownUnitDegradesToNone() {
        assertEquals(StandardUnit.None, cloudWatchUnit("furlongs"))
    }
}
