/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.serde

import kotlin.math.sqrt

val WARMUP_SECONDS = System.getProperty("benchmark.warmupSeconds", "10").toLong()
val MEASUREMENT_SECONDS = System.getProperty("benchmark.measurementSeconds", "30").toLong()
val MIN_ITERATIONS = System.getProperty("benchmark.minIterations", "1000").toInt()
val MAX_ITERATIONS = System.getProperty("benchmark.maxIterations", "10000000").toInt()

private val WARMUP_NANOS = WARMUP_SECONDS * 1_000_000_000L
private val MEASUREMENT_NANOS = MEASUREMENT_SECONDS * 1_000_000_000L

data class BenchmarkStats(
    val iterations: Int,
    val mean: Double,
    val p50: Double,
    val p90: Double,
    val p95: Double,
    val p99: Double,
    val stdDev: Double,
)

data class BenchmarkResult(
    val id: String,
    val stats: BenchmarkStats,
)

object BenchmarkHarness {

    /**
     * Runs a benchmark following the spec:
     * - Warmup phase: run for WARMUP_SECONDS (default 10s) to let JIT reach steady state.
     *   All warmup samples are discarded.
     * - Measurement phase: run for MEASUREMENT_SECONDS (default 30s) or until MAX_ITERATIONS,
     *   whichever comes first. Must record at least MIN_ITERATIONS (default 1,000).
     */
    suspend fun run(
        id: String,
        interceptor: BenchmarkInterceptor,
        extractNanos: (BenchmarkInterceptor) -> Long,
        operation: suspend () -> Unit,
    ): BenchmarkResult {
        // Phase 1: Warmup — discard all results
        val warmupStart = System.nanoTime()
        while (System.nanoTime() - warmupStart < WARMUP_NANOS) {
            interceptor.reset()
            operation()
        }

        // Phase 2: Measurement
        val samples = ArrayList<Long>(MAX_ITERATIONS.coerceAtMost(1_000_000))
        val measureStart = System.nanoTime()

        while (samples.size < MAX_ITERATIONS) {
            interceptor.reset()
            operation()
            samples.add(extractNanos(interceptor))

            if (samples.size >= MIN_ITERATIONS) {
                val elapsed = System.nanoTime() - measureStart
                if (elapsed >= MEASUREMENT_NANOS) break
            }
        }

        val sorted = samples.toLongArray()
        sorted.sort()

        return BenchmarkResult(
            id = id,
            stats = computeStats(sorted),
        )
    }

    private fun computeStats(sorted: LongArray): BenchmarkStats {
        val n = sorted.size
        val sum = sorted.sum()
        val mean = sum.toDouble() / n

        var varianceSum = 0.0
        for (v in sorted) {
            val diff = v.toDouble() - mean
            varianceSum += diff * diff
        }
        val stdDev = sqrt(varianceSum / n)

        return BenchmarkStats(
            iterations = n,
            mean = mean,
            p50 = percentile(sorted, 50.0),
            p90 = percentile(sorted, 90.0),
            p95 = percentile(sorted, 95.0),
            p99 = percentile(sorted, 99.0),
            stdDev = stdDev,
        )
    }

    private fun percentile(sorted: LongArray, p: Double): Double {
        val index = (p / 100.0) * (sorted.size - 1)
        val lower = index.toInt()
        val upper = lower + 1
        if (upper >= sorted.size) return sorted.last().toDouble()
        val fraction = index - lower
        return sorted[lower] + fraction * (sorted[upper] - sorted[lower])
    }

    fun toJson(metadata: BenchmarkMetadata, results: List<BenchmarkResult>): String = buildString {
        appendLine("{")
        appendLine("  \"metadata\": {")
        appendLine("    \"lang\": \"Kotlin\",")
        appendLine("    \"software\": [")
        appendLine("      [\"smithy-kotlin\", \"${metadata.smithyKotlinVersion}\"],")
        appendLine("      [\"AWS SDK for Kotlin\", \"${metadata.sdkVersion}\"]")
        appendLine("    ],")
        appendLine("    \"os\": \"${metadata.os}\",")
        appendLine("    \"instance\": \"${metadata.instance}\",")
        appendLine("    \"precision\": \"-9\"")
        appendLine("  },")
        appendLine("  \"serde_benchmarks\": [")
        results.forEachIndexed { i, result ->
            appendLine("    {")
            appendLine("      \"id\": \"${result.id}\",")
            appendLine("      \"n\": ${result.stats.iterations},")
            appendLine("      \"mean\": ${result.stats.mean},")
            appendLine("      \"p50\": ${result.stats.p50},")
            appendLine("      \"p90\": ${result.stats.p90},")
            appendLine("      \"p95\": ${result.stats.p95},")
            appendLine("      \"p99\": ${result.stats.p99},")
            appendLine("      \"std_dev\": ${result.stats.stdDev}")
            append("    }")
            if (i < results.size - 1) appendLine(",") else appendLine()
        }
        appendLine("  ]")
        appendLine("}")
    }
}

data class BenchmarkMetadata(
    val smithyKotlinVersion: String,
    val sdkVersion: String,
    val os: String = "${System.getProperty("os.name")} ${System.getProperty("os.version")}",
    val instance: String = System.getProperty("benchmark.instance", "unknown"),
)
