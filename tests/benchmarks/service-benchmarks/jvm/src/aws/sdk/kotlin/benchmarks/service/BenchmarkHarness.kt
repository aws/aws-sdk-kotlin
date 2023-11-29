/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service

import aws.sdk.kotlin.benchmarks.service.definitions.*
import aws.sdk.kotlin.benchmarks.service.telemetry.MetricSummary
import aws.smithy.kotlin.runtime.client.SdkClient
import aws.smithy.kotlin.runtime.io.use
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

val DEFAULT_WARMUP_TIME = 5.seconds
val DEFAULT_ITERATION_TIME = 15.seconds

private val benchmarks = setOf(
    S3Benchmark(),
    SnsBenchmark(),
    StsBenchmark(),
    CloudwatchBenchmark(),
    CloudwatchEventsBenchmark(),
    DynamoDbBenchmark(),
    PinpointBenchmark(),
).map {
    @Suppress("UNCHECKED_CAST")
    it as ServiceBenchmark<SdkClient>
}

suspend fun main() {
    val harness = BenchmarkHarness()
    harness.execute()
}

class BenchmarkHarness {
    private val summaries = mutableMapOf<String, MutableMap<String, Map<String, MetricSummary>>>()

    suspend fun execute() {
        benchmarks.forEach { execute(it) }
        println()
        printResults()
    }

    private suspend fun execute(benchmark: ServiceBenchmark<SdkClient>) {
        benchmark.client().use { client ->
            println("${client.config.clientName}:")

            println("  Setting up...")
            benchmark.setup(client)

            try {
                benchmark.operations.forEach { execute(it, client) }
            } finally {
                benchmark.tearDown(client)
            }
        }
        println()
    }

    private suspend fun execute(operation: OperationBenchmark<SdkClient>, client: SdkClient) {
        println("  ${operation.name}:")

        println("    Setting up...")
        operation.setup(client)

        try {
            println("    Warming up for ${operation.warmupMode.explanation}...")
            forAtLeast(operation.warmupMode) {
                operation.transact(client)
            }

            Common.metricAggregator.clear()

            println("    Measuring for ${operation.iterationMode.explanation}...")
            forAtLeast(operation.iterationMode) {
                operation.transact(client)
            }

            val summary = Common.metricAggregator.summarizeAndClear()
            summaries.getOrPut(client.config.clientName, ::mutableMapOf)[operation.name] = summary
        } finally {
            println("    Tearing down...")
            operation.tearDown(client)
        }
    }

    private fun printResults() {
        val table = ResultsTable.from(summaries)
        println(table)
    }
}

private inline fun forAtLeast(runMode: RunMode, block: () -> Unit) {
    val start = TimeSource.Monotonic.markNow()

    when (runMode) {
        is RunMode.Time -> {
            var cnt = 0
            while (start.elapsedNow() < runMode.time) {
                block()
                cnt++
            }
            println("      (completed $cnt iterations)")
        }

        is RunMode.Iterations -> {
            repeat(runMode.iterations) {
                block()
            }
            println("      (took ${start.elapsedNow()})")
        }
    }
}

private val RunMode.explanation get() = when (this) {
    is RunMode.Iterations -> "$iterations iterations"
    is RunMode.Time -> time.toString()
}
