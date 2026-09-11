/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.telemetry.cloudwatch

import aws.sdk.kotlin.services.cloudwatch.model.MetricDatum
import aws.sdk.kotlin.services.cloudwatch.model.StatisticSet
import aws.smithy.kotlin.runtime.telemetry.metrics.aggregation.MetricData
import aws.smithy.kotlin.runtime.telemetry.metrics.aggregation.MetricValue
import aws.sdk.kotlin.services.cloudwatch.model.Dimension as CwDimension

/**
 * `PutMetricData` service limits.
 *
 * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_PutMetricData.html">PutMetricData</a>
 * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html">MetricDatum</a>
 */
internal object CloudWatchLimits {
    /**
     * Metrics per request, counted across `MetricData` and `EntityMetricData` combined.
     *
     * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_PutMetricData.html#API_PutMetricData_RequestParameters">PutMetricData request parameters</a>
     */
    const val MAX_DATUMS_PER_REQUEST = 1_000

    /**
     * Unique value/count pairs per datum. CloudWatch merges datums sharing a metric name, dimensions,
     * and timestamp, so a distribution exceeding this is split across datums without affecting the
     * resulting statistics.
     *
     * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html#ACW-Type-MetricDatum-Values">MetricDatum.Values</a>
     */
    const val MAX_VALUES_PER_DATUM = 150

    /**
     * Dimensions per metric. Dimensions are part of a metric's identity, so dropping one yields a
     * different metric rather than a less detailed one.
     *
     * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html#ACW-Type-MetricDatum-Dimensions">MetricDatum.Dimensions</a>
     */
    const val MAX_DIMENSIONS = 30
}

/**
 * Converts aggregated [MetricData] into [MetricDatum]s, splitting wherever a service limit requires.
 *
 * | [MetricValue] | `MetricDatum` member |
 * |---|---|
 * | `Sum`, `LastValue` | `Value` |
 * | `Summary` | `StatisticValues` |
 * | `Distribution` | `Values` + `Counts` |
 *
 * @param storageResolution stamped onto every datum: 60 for standard, 1 for high-resolution.
 */
internal class MetricDatumChunker(private val storageResolution: Int) {
    /**
     * Flattens every instrument's points into datums. May return more datums than there were points,
     * since one [MetricValue.Distribution] can exceed [CloudWatchLimits.MAX_VALUES_PER_DATUM].
     */
    fun toDatums(metrics: List<MetricData>): List<MetricDatum> = buildList {
        metrics.forEach { data ->
            // Named to avoid shadowing MetricDatum.Builder.unit in the DSL block below.
            val datumUnit = cloudWatchUnit(data.descriptor.units)
            data.points.forEach { point ->
                // Truncate rather than drop the datum: a request violating the limit is rejected
                // wholesale, taking unrelated datums with it.
                val dims = point.dimensions
                    .take(CloudWatchLimits.MAX_DIMENSIONS)
                    .map {
                        CwDimension {
                            name = it.name
                            value = it.value
                        }
                    }

                fun datum(block: MetricDatum.Builder.() -> Unit) = MetricDatum {
                    metricName = data.descriptor.name
                    // The cycle's timestamp, so every point in a batch lines up on a chart.
                    timestamp = data.timestamp
                    dimensions = dims
                    unit = datumUnit
                    storageResolution = this@MetricDatumChunker.storageResolution
                    block()
                }

                when (val v = point.value) {
                    is MetricValue.Sum -> add(datum { value = v.value })
                    is MetricValue.LastValue -> add(datum { value = v.value })

                    is MetricValue.Summary -> add(
                        datum {
                            statisticValues = StatisticSet {
                                sampleCount = v.count.toDouble()
                                sum = v.sum
                                minimum = v.min
                                maximum = v.max
                            }
                        },
                    )

                    is MetricValue.Distribution ->
                        v.values.entries
                            .chunked(CloudWatchLimits.MAX_VALUES_PER_DATUM)
                            .forEach { chunk ->
                                add(
                                    datum {
                                        // Positional: index i of values corresponds to index i of counts.
                                        values = chunk.map { it.key }
                                        counts = chunk.map { it.value.toDouble() }
                                    },
                                )
                            }
                }
            }
        }
    }

    /**
     * Groups datums into per-request batches by count. The 1 MB payload limit is not enforced;
     * computing a serialised size here would duplicate the protocol's serialisation, and
     * [CloudWatchLimits.MAX_DATUMS_PER_REQUEST] datums stay well under it at the sizes this exporter
     * produces.
     */
    fun toRequests(datums: List<MetricDatum>): List<List<MetricDatum>> = datums.chunked(CloudWatchLimits.MAX_DATUMS_PER_REQUEST)
}
