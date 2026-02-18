/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.telemetry

import aws.smithy.kotlin.runtime.ExperimentalApi
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.telemetry.AbstractTelemetryProvider
import aws.smithy.kotlin.runtime.telemetry.context.Context
import aws.smithy.kotlin.runtime.telemetry.metrics.*

private val capturedMetrics = mapOf(
    "smithy.client.call.attempt_overhead_duration" to "Overhead",
    // "smithy.client.http.time_to_first_byte" to "TTFB",
    // "smithy.client.call.attempt_duration" to "Call",
    // "smithy.client.call.serialization_duration" to "Serlz",
    // "smithy.client.call.deserialization_duration" to "Deserlz",
    // "smithy.client.call.resolve_endpoint_duration" to "EPR",
    // "smithy.client.call.request_payload_size" to "ReqSize",
    // "smithy.client.call.response_payload_size" to "RespSize",
)

@ExperimentalApi
class BenchmarkTelemetryProvider(private val metricAggregator: MetricAggregator) : AbstractTelemetryProvider() {
    override val meterProvider = object : AbstractMeterProvider() {
        override fun getOrCreateMeter(scope: String) = object : AbstractMeter() {
            override fun createDoubleHistogram(name: String, units: String?, description: String?) = capturedMetrics[name]?.let { BenchmarkDoubleHistogram(it, units) } ?: Histogram.DoubleNone
        }
    }

    private inner class BenchmarkDoubleHistogram(name: String, units: String?) : DoubleHistogram {
        private val newUnit: String?
        private val transform: (Double) -> Double

        init {
            when (units) {
                "s" -> {
                    newUnit = "ms"
                    transform = { it * 1000 }
                }
                "bytes" -> {
                    newUnit = "bytes"
                    transform = { it }
                }
                null -> {
                    newUnit = null
                    transform = { it }
                }

                else -> throw IllegalArgumentException("Unknown unit type $units")
            }
        }

        private val formattedName = name + (newUnit?.let { " ($it)" } ?: "")

        override fun record(value: Double, attributes: Attributes, context: Context?) {
            metricAggregator.add(formattedName, transform(value))
        }
    }
}
