/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.telemetry.cloudwatch

import aws.sdk.kotlin.services.cloudwatch.model.MetricDatum
import aws.sdk.kotlin.services.cloudwatch.model.StandardUnit
import aws.smithy.kotlin.runtime.telemetry.metrics.aggregation.Dimension
import aws.smithy.kotlin.runtime.telemetry.metrics.aggregation.InstrumentDescriptor
import aws.smithy.kotlin.runtime.telemetry.metrics.aggregation.MetricData
import aws.smithy.kotlin.runtime.telemetry.metrics.aggregation.MetricPoint
import aws.smithy.kotlin.runtime.telemetry.metrics.aggregation.MetricValue
import aws.smithy.kotlin.runtime.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Chunking against the two hard `PutMetricData` limits, plus the mapping from each aggregation shape
 * onto the `MetricDatum` member that carries it.
 *
 * The limits are exact API constraints rather than tuning choices, so an off-by-one is a rejected
 * request — and a rejected request discards every datum batched with it, not just the offending one.
 * Cheap to test, expensive to get wrong.
 */
class MetricDatumChunkerTest {
    private fun data(
        value: MetricValue,
        units: String? = null,
        dimensions: List<Dimension> = emptyList(),
    ) = MetricData(
        InstrumentDescriptor("t", "latency", units),
        Instant.fromEpochSeconds(0),
        listOf(MetricPoint(dimensions, value)),
    )

    /**
     * 400 distinct values exceed the 150 value-count pairs a single datum allows, so they must span
     * several datums for the same metric and dimension set — which CloudWatch merges back together.
     *
     * The 3 is `ceil(400 / 150)`, and the trailing partial chunk of 100 is the interesting case: it
     * confirms the remainder is emitted rather than dropped.
     */
    @Test
    fun testDistributionSplitsAtValueLimit() {
        val values = (1..400).associate { it.toDouble() to 1L }
        val datums = MetricDatumChunker(storageResolution = 60).toDatums(
            listOf(data(MetricValue.Distribution(values), units = "ms")),
        )

        assertEquals(3, datums.size) // 150 + 150 + 100
        assertTrue(datums.all { (it.values?.size ?: 0) <= CloudWatchLimits.MAX_VALUES_PER_DATUM })

        // Every pair survives the split, and values line up with counts positionally.
        assertEquals(400, datums.sumOf { it.values!!.size })
        assertTrue(datums.all { it.values!!.size == it.counts!!.size })
    }

    /**
     * The 1000-datums-per-request limit, applied one level up: datums are grouped into requests.
     *
     * Distinct metric names so nothing merges and the count is unambiguous. Note this deliberately
     * produces more requests than `maxCallsPerUpload` allows — the chunker's job is to batch
     * correctly, and enforcing the cost cap is the exporter's, so these are tested separately.
     */
    @Test
    fun testRequestsSplitAtDatumLimit() {
        val datums = List(2_500) {
            MetricDatum {
                metricName = "m$it"
                value = 1.0
            }
        }
        val requests = MetricDatumChunker(60).toRequests(datums)

        assertEquals(3, requests.size)
        assertTrue(requests.all { it.size <= CloudWatchLimits.MAX_DATUMS_PER_REQUEST })
        assertEquals(2_500, requests.sumOf { it.size })
    }

    /**
     * A `Summary` goes to `StatisticValues`, never to `Value`.
     *
     * Publishing the sum as `Value` instead would look correct on a `Sum` graph and be wrong on every
     * other statistic — average, min, max, and sample count would all be lost or fabricated.
     */
    @Test
    fun testSummaryMapsToStatisticSet() {
        val datum = MetricDatumChunker(60)
            .toDatums(listOf(data(MetricValue.Summary(count = 4, sum = 10.0, min = 1.0, max = 4.0))))
            .single()

        assertNull(datum.value)
        val stats = datum.statisticValues!!
        assertEquals(4.0, stats.sampleCount)
        assertEquals(10.0, stats.sum)
        assertEquals(1.0, stats.minimum)
        assertEquals(4.0, stats.maximum)
    }

    /** `Sum` and `LastValue` both carry a scalar, and both belong in `Value`. */
    @Test
    fun testScalarAggregationsMapToValue() {
        val chunker = MetricDatumChunker(60)

        val sum = chunker.toDatums(listOf(data(MetricValue.Sum(7.0, monotonic = true)))).single()
        assertEquals(7.0, sum.value)

        val last = chunker.toDatums(listOf(data(MetricValue.LastValue(3.0)))).single()
        assertEquals(3.0, last.value)
    }

    /**
     * Shared datum fields — unit, timestamp, dimensions, storage resolution — are stamped from the
     * instrument and the cycle rather than defaulted.
     *
     * The timestamp is the one to watch: taking "now" per datum instead of the cycle's timestamp
     * would scatter one interval's points across several CloudWatch periods.
     */
    @Test
    fun testSharedDatumFieldsArePopulated() {
        val datum = MetricDatumChunker(storageResolution = 1)
            .toDatums(
                listOf(
                    data(
                        MetricValue.Sum(1.0, monotonic = true),
                        units = "ms",
                        dimensions = listOf(Dimension("op", "GetObject")),
                    ),
                ),
            )
            .single()

        assertEquals("latency", datum.metricName)
        assertEquals(StandardUnit.Milliseconds, datum.unit)
        assertEquals(Instant.fromEpochSeconds(0), datum.timestamp)
        assertEquals(1, datum.storageResolution)
        assertEquals("op", datum.dimensions!!.single().name)
        assertEquals("GetObject", datum.dimensions!!.single().value)
    }

    /**
     * Dimensions past the per-metric limit are truncated rather than costing the whole request.
     *
     * Unreachable through the configured pipeline — the dimension allowlist caps this well below 30 —
     * but a datum that violates the limit takes up to 999 unrelated datums down with it.
     */
    @Test
    fun testDimensionsAreTruncatedAtLimit() {
        val dims = List(40) { Dimension("d$it", "v") }
        val datum = MetricDatumChunker(60)
            .toDatums(listOf(data(MetricValue.Sum(1.0, monotonic = true), dimensions = dims)))
            .single()

        assertEquals(CloudWatchLimits.MAX_DIMENSIONS, datum.dimensions!!.size)
    }
}
