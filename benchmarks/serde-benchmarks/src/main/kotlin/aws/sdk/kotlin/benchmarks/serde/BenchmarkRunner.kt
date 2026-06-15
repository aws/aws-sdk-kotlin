/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.serde

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val protocolFilter = System.getProperty("benchmark.protocol")
    val filters = protocolFilter?.split(",")?.map { it.trim() }

    val registrations = if (filters != null) {
        protocolRegistrations.filterKeys { key -> filters.any { key.contains(it, ignoreCase = true) } }
    } else {
        protocolRegistrations
    }

    registrations.forEach { (name, register) ->
        println("Registering protocol: $name")
        register()
    }

    val allResults = mutableListOf<BenchmarkResult>()

    for (entry in BenchmarkRegistry.entries) {
        println("Running: ${entry.name}")
        val benchmark = entry.create()
        val results = benchmark.benchmarks()
        allResults.addAll(results)
        println("  Completed ${results.size} test cases")
    }

    val metadata = KotlinBenchmarkMetadata(
        smithyKotlinVersion = System.getProperty("smithy.kotlin.version"),
        sdkVersion = System.getProperty("aws.sdk.kotlin.version"),
    )
    println()
    println(BenchmarkHarness.toJson(metadata, allResults))
}
