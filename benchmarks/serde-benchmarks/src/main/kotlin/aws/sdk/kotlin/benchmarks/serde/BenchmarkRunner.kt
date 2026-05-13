/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.serde

import kotlinx.coroutines.runBlocking

/**
 * Discovers and runs all generated serde benchmark classes.
 *
 * Each generated benchmark class follows the pattern:
 * - Has a no-arg constructor
 * - Has a `suspend fun benchmarks(): List<BenchmarkResult>` method
 *
 * This runner uses reflection to find all benchmark classes on the classpath.
 */
fun main() = runBlocking {
    val benchmarkClassNames = discoverBenchmarkClasses()
    val allResults = mutableListOf<BenchmarkResult>()

    for (className in benchmarkClassNames) {
        println("Running: $className")
        val results = runBenchmarkClass(className)
        allResults.addAll(results)
        println("  Completed ${results.size} test cases")
    }

    val metadata = BenchmarkMetadata(
        smithyKotlinVersion = System.getProperty("smithy.kotlin.version", "SNAPSHOT"),
        sdkVersion = System.getProperty("aws.sdk.kotlin.version", "SNAPSHOT"),
    )
    println()
    println(BenchmarkHarness.toJson(metadata, allResults))
}

private suspend fun runBenchmarkClass(className: String): List<BenchmarkResult> {
    val clazz = Class.forName(className)
    val instance = clazz.getDeclaredConstructor().newInstance()
    val method = clazz.getMethod("benchmarks", kotlin.coroutines.Continuation::class.java)

    @Suppress("UNCHECKED_CAST")
    val result = kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn<List<BenchmarkResult>> { cont ->
        method.invoke(instance, cont)
    }
    return result
}

private fun discoverBenchmarkClasses(): List<String> {
    val packages = listOf(
        "aws.sdk.kotlin.benchmarks.serde.awsrestjson",
        "aws.sdk.kotlin.benchmarks.serde.awsjsonrpc10",
        "aws.sdk.kotlin.benchmarks.serde.smithyrpcv2cbor",
        "aws.sdk.kotlin.benchmarks.serde.awsrestxml",
        "aws.sdk.kotlin.benchmarks.serde.awsquery",
    )
    val suffixes = listOf("SerializationBenchmark", "DeserializationBenchmark")

    val classNames = mutableListOf<String>()
    for (pkg in packages) {
        for (suffix in suffixes) {
            // Try known operation names from the benchmark models
            val operations = listOf(
                "PutObject", "GetObject", "CopyObject", "HeadObject",
                "PutItem", "GetItem",
                "PutMetricData", "GetMetricData",
                "Healthcheck",
            )
            for (op in operations) {
                val className = "$pkg.$op$suffix"
                try {
                    Class.forName(className)
                    classNames.add(className)
                } catch (_: ClassNotFoundException) {
                    // Not all operations exist in all protocols
                }
            }
        }
    }
    return classNames
}
