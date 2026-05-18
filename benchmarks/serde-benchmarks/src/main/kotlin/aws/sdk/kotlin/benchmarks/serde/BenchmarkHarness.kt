/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.serde

import kotlin.math.sqrt

var MIN_ITERATIONS = System.getProperty("benchmark.minIterations", "1000").toInt()
var MAX_ITERATIONS = System.getProperty("benchmark.maxIterations", "10000").toInt()
var MAX_DURATION_NANOS = System.getProperty("benchmark.maxDurationSeconds", "30").toLong() * 1_000_000_000L
var WARMUP_FRACTION = System.getProperty("benchmark.warmupFraction", "0.5").toDouble()

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
     * - Min 1,000 iterations
     * - Max 30 seconds OR 10,000 iterations
     * - Discard up to 50% warmup from earliest iterations
     *
     * @param id the test case identifier
     * @param interceptor the benchmark interceptor to read timestamps from
     * @param extractNanos function to extract the relevant duration from the interceptor (serialization or deserialization)
     * @param operation the suspend function to benchmark (e.g., `{ client.putObject(input) }`)
     */
    suspend fun run(
        id: String,
        interceptor: BenchmarkInterceptor,
        extractNanos: (BenchmarkInterceptor) -> Long,
        operation: suspend () -> Unit,
    ): BenchmarkResult {
        val samples = LongArray(MAX_ITERATIONS)
        var count = 0
        val benchmarkStart = System.nanoTime()

        while (count < MAX_ITERATIONS) {
            interceptor.reset()
            operation()
            samples[count] = extractNanos(interceptor)
            count++

            if (count >= MIN_ITERATIONS) {
                val elapsed = System.nanoTime() - benchmarkStart
                if (elapsed >= MAX_DURATION_NANOS) break
            }
        }

        val warmupCount = (count * WARMUP_FRACTION).toInt()
        val measured = samples.copyOfRange(warmupCount, count)
        measured.sort()

        return BenchmarkResult(
            id = id,
            stats = computeStats(measured),
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
