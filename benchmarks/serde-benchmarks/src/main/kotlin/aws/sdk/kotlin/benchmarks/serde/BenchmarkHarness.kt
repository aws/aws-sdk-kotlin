/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.serde

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

val WARMUP_SECONDS = System.getProperty("benchmark.warmupSeconds").toLong()
val MEASUREMENT_SECONDS = System.getProperty("benchmark.measurementSeconds").toLong()
val MIN_ITERATIONS = System.getProperty("benchmark.minIterations").toInt()
val MAX_ITERATIONS = System.getProperty("benchmark.maxIterations").toInt()

internal val WARMUP_DURATION = WARMUP_SECONDS.seconds
internal val MEASUREMENT_DURATION = MEASUREMENT_SECONDS.seconds
internal val WINDOW_DURATION = 1.seconds

@Serializable
data class BenchmarkResult(
    val id: String,
    val n: Int,
    val mean: Double,
    val p50: Double,
    val p90: Double,
    val p95: Double,
    val p99: Double,
    @SerialName("std_dev") val stdDev: Double,
)

@Serializable
private data class BenchmarkMetadataJson(
    val lang: String = "Kotlin",
    val software: List<List<String>>,
    val os: String,
    val instance: String,
    val precision: String = "-9",
)

@Serializable
private data class BenchmarkReportJson(
    val metadata: BenchmarkMetadataJson,
    @SerialName("serde_benchmarks") val serdeBenchmarks: List<BenchmarkResult>,
)

object BenchmarkHarness {

    /**
     * Runs a benchmark following the spec:
     * - Warmup phase: run for WARMUP_SECONDS (default 10s) to let JIT reach steady state.
     *   All warmup samples are discarded.
     * - Measurement phase: run for MEASUREMENT_SECONDS (default 30s) or until MAX_ITERATIONS,
     *   whichever comes first. Must record at least MIN_ITERATIONS (default 1,000).
     */
    internal inline suspend fun run(
        id: String,
        interceptor: BenchmarkInterceptor,
        extractNanos: (BenchmarkInterceptor) -> Long,
        operation: suspend () -> Unit,
    ): BenchmarkResult {
        val timeSource = TimeSource.Monotonic

        // Phase 1: Warmup — discard all results
        val warmupEnd = timeSource.markNow() + WARMUP_DURATION
        while (warmupEnd.hasNotPassedNow()) {
            interceptor.reset()
            operation()
        }

        // Flush garbage accumulated during warmup before measuring
        System.gc()
        Thread.sleep(100)

        // Phase 2: Measurement — collect individual samples and track 1-second windows
        val samples = ArrayList<Long>(MAX_ITERATIONS)
        val windowMeans = ArrayList<Double>(MEASUREMENT_SECONDS.toInt())
        val measureStart = timeSource.markNow()
        var windowMark = measureStart
        var windowSum = 0L
        var windowCount = 0

        while (samples.size < MAX_ITERATIONS) {
            interceptor.reset()
            operation()
            val sample = extractNanos(interceptor)
            samples.add(sample)
            windowSum += sample
            windowCount++

            if (windowMark.elapsedNow() >= WINDOW_DURATION) {
                windowMeans.add(windowSum.toDouble() / windowCount)
                windowMark = timeSource.markNow()
                windowSum = 0L
                windowCount = 0
            }

            if (samples.size >= MIN_ITERATIONS) {
                if (measureStart.elapsedNow() >= MEASUREMENT_DURATION) break
            }
        }

        // Flush any remaining partial window
        if (windowCount > 0) {
            windowMeans.add(windowSum.toDouble() / windowCount)
        }

        val sorted = samples.toLongArray()
        sorted.sort()

        return computeResult(id, sorted, windowMeans)
    }

    internal fun computeResult(id: String, sorted: LongArray, windowMeans: List<Double>): BenchmarkResult {
        val n = sorted.size
        val sum = sorted.sum()
        val mean = sum.toDouble() / n

        val wmMean = windowMeans.average()
        val variance = windowMeans.sumOf { (it - wmMean) * (it - wmMean) } / windowMeans.size
        val stdDev = sqrt(variance)

        return BenchmarkResult(
            id = id,
            n = n,
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

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun toJson(metadata: KotlinBenchmarkMetadata, results: List<BenchmarkResult>): String {
        val report = BenchmarkReportJson(
            metadata = BenchmarkMetadataJson(
                software = listOf(
                    listOf("smithy-kotlin", metadata.smithyKotlinVersion),
                    listOf("AWS SDK for Kotlin", metadata.sdkVersion),
                ),
                os = metadata.os,
                instance = metadata.instance,
            ),
            serdeBenchmarks = results,
        )
        return json.encodeToString(report)
    }
}

data class KotlinBenchmarkMetadata(
    val smithyKotlinVersion: String,
    val sdkVersion: String,
    val os: String = "${System.getProperty("os.name")} ${System.getProperty("os.version")}",
    val instance: String = System.getProperty("benchmark.instance"),
)
