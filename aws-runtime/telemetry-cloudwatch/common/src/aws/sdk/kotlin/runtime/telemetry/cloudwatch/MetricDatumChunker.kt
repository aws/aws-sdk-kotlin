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
 * Each constant links to the shape member that defines it, so the value can be re-checked against the
 * API reference without hunting for it. Centralised as named constants rather than inlined at each
 * call site so that a service limit change is a one-line edit, and so the chunking tests can assert
 * against the same source of truth the production code uses.
 *
 * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_PutMetricData.html">PutMetricData</a>
 * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html">MetricDatum</a>
 */
internal object CloudWatchLimits {
    /**
     * Metrics per request, counted across `MetricData` and `EntityMetricData` combined. This exporter
     * only populates `MetricData`, so it gets the whole allowance.
     *
     * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_PutMetricData.html#API_PutMetricData_RequestParameters">PutMetricData request parameters</a>
     */
    const val MAX_DATUMS_PER_REQUEST = 1_000

    /**
     * Unique value/count pairs for one metric.
     *
     * CAUTION — the scope of this limit is unresolved. `MetricDatum.Values` describes it as 150 unique
     * values "in each `PutMetricData` action", and `PutMetricData` as 150 "per metric with one
     * `PutMetricData` request"; both phrasings bind the *request* rather than the individual datum,
     * whereas [MetricDatumChunker.toDatums] splits across datums within one request. This needs an
     * empirical check before release: if the limit binds the request, the chunking strategy changes.
     *
     * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html#ACW-Type-MetricDatum-Values">MetricDatum.Values</a>
     */
    const val MAX_VALUES_PER_DATUM = 150

    /**
     * Dimensions per metric. Note that dimensions are part of a metric's identity, so a datum that
     * silently loses a dimension becomes a *different* metric rather than a less detailed one.
     *
     * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html#ACW-Type-MetricDatum-Dimensions">MetricDatum.Dimensions</a>
     */
    const val MAX_DIMENSIONS = 30

    /**
     * Namespace length for `PutMetricData`.
     *
     * Deliberately 255, not the 1024 that `EmfConstants.MAX_NAMESPACE_LENGTH` uses. Which bound
     * governs an EMF-extracted metric is unconfirmed; for this exporter, which calls the API
     * directly, 255 is the documented limit and the one to validate against.
     *
     * Length is not the only constraint the API documents: the namespace must be ASCII without control
     * characters, must match `[^:].*`, and should not begin with `AWS/` (reserved for service-published
     * metrics). [CloudWatchMetricExporter.Builder.validate] checks length only — see the note there.
     *
     * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_PutMetricData.html#API_PutMetricData_RequestParameters">PutMetricData request parameters</a>
     */
    const val MAX_NAMESPACE_LENGTH = 255
}

/**
 * Converts aggregated [MetricData] into [MetricDatum]s, splitting wherever a service limit requires.
 *
 * Separated from [CloudWatchMetricExporter] so that conversion and chunking — the parts with fiddly
 * boundary conditions — are unit-testable without a CloudWatch client, credentials, or a network.
 *
 * ### Aggregation to `MetricDatum` member
 *
 * | [MetricValue] | `MetricDatum` member |
 * |---|---|
 * | `Sum`, `LastValue` | `Value` |
 * | `Summary` | `StatisticValues` (a `StatisticSet`) |
 * | `Distribution` | `Values` + `Counts`, positional pairs |
 *
 * @param storageResolution stamped onto every datum. 60 (standard) or 1 (high-resolution); 1 costs
 *   substantially more, so it is not the default.
 *
 * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html">MetricDatum</a>
 * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_StatisticSet.html">StatisticSet</a>
 * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_Dimension.html">Dimension</a>
 */
internal class MetricDatumChunker(private val storageResolution: Int) {
    /**
     * Flatten every instrument's points into datums.
     *
     * The output can be longer than the input point count, because one [MetricValue.Distribution]
     * may need several datums to stay under [CloudWatchLimits.MAX_VALUES_PER_DATUM].
     */
    fun toDatums(metrics: List<MetricData>): List<MetricDatum> = buildList {
        metrics.forEach { data ->
            // Named to avoid shadowing MetricDatum.Builder.unit inside the DSL block below.
            val datumUnit = cloudWatchUnit(data.descriptor.units)
            data.points.forEach { point ->
                // `take` rather than rejecting the datum: truncating keeps the metric flowing,
                // whereas dropping it loses data entirely. In practice unreachable, because
                // maxCardinality and the dimension allowlist both cap this far below 30 — but a
                // request that violates the limit is rejected wholesale, taking up to 999 unrelated
                // datums with it, so the cheap guard is worth keeping.
                val dims = point.dimensions
                    .take(CloudWatchLimits.MAX_DIMENSIONS)
                    .map {
                        CwDimension {
                            name = it.name
                            value = it.value
                        }
                    }

                // Local helper for the fields every datum shares, so the four value-shape branches
                // below differ only in the part that actually differs.
                fun datum(block: MetricDatum.Builder.() -> Unit) = MetricDatum {
                    metricName = data.descriptor.name
                    // The cycle's timestamp, not "now": every point in a batch must share one
                    // timestamp to line up on a chart.
                    timestamp = data.timestamp
                    dimensions = dims
                    unit = datumUnit
                    storageResolution = this@MetricDatumChunker.storageResolution
                    block()
                }

                // Exhaustive `when` over the sealed MetricValue: adding a fifth aggregation later
                // becomes a compile error here rather than a silently unpublished metric.
                when (val v = point.value) {
                    // Sum and LastValue both map to a plain `value`. Identical output, but they must
                    // stay separate branches — they arrived here with different temporality, and
                    // collapsing them would invite someone to "simplify" by diffing LastValue.
                    is MetricValue.Sum -> add(datum { value = v.value })
                    is MetricValue.LastValue -> add(datum { value = v.value })

                    is MetricValue.Summary -> add(
                        datum {
                            statisticValues = StatisticSet {
                                // sampleCount is a Double in the CloudWatch model even though it is
                                // conceptually a count; the API defines it that way.
                                sampleCount = v.count.toDouble()
                                sum = v.sum
                                minimum = v.min
                                maximum = v.max
                            }
                        },
                    )

                    // A distribution can exceed the per-datum pair limit, so it may need several
                    // datums for the same metric and dimension set. That is valid and intended:
                    // CloudWatch merges datums sharing a metric name, dimensions, and timestamp, so
                    // the statistics come out the same as if they had fitted in one.
                    is MetricValue.Distribution ->
                        v.values.entries
                            .chunked(CloudWatchLimits.MAX_VALUES_PER_DATUM)
                            .forEach { chunk ->
                                add(
                                    datum {
                                        // `values` and `counts` are positional: index i of one
                                        // corresponds to index i of the other. Both are derived from
                                        // the same `chunk` in the same order so they cannot drift.
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
     * Group datums into per-request batches.
     *
     * Only the datum-count limit is enforced here. The 1 MB payload limit is not, deliberately:
     * computing a serialised size before the request is built would mean duplicating the protocol's
     * serialisation, and 1000 datums are comfortably under 1 MB at the sizes this exporter produces.
     * A `Distribution` with 150 pairs is the largest datum shape and is roughly 3 KB.
     *
     * KNOWN GAP: a pathological configuration — very long metric names and many long dimension
     * values — could in principle exceed 1 MB inside 1000 datums. The failure mode is a rejected
     * request logged by the exporter, not corrupted data. Worth revisiting if it is ever observed.
     */
    fun toRequests(datums: List<MetricDatum>): List<List<MetricDatum>> = datums.chunked(CloudWatchLimits.MAX_DATUMS_PER_REQUEST)
}
