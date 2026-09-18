/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service

import aws.sdk.kotlin.benchmarks.service.telemetry.BenchmarkTelemetryProvider
import aws.sdk.kotlin.benchmarks.service.telemetry.MetricAggregator
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.util.Uuid
import kotlin.random.Random

object Common {
    val metricAggregator = MetricAggregator()

    val noRetries = StandardRetryStrategy {
        maxAttempts = 1
    }

    val telemetryProvider = BenchmarkTelemetryProvider(metricAggregator)

    fun random(prefix: String = "") = "$prefix${Uuid.random()}"

    fun randomStringPayload(scale: Int): String = (1..scale)
        .map { Random.nextInt(32, 127).toChar() }
        .joinToString("")

    fun randomBytesPayload(scale: Int): ByteArray {
        val bytes = ByteArray(scale)
        Random.nextBytes(bytes)
        return bytes
    }
}
