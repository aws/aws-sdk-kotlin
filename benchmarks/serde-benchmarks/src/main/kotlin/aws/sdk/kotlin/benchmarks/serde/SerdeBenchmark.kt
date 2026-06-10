/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.serde

interface SerdeBenchmark {
    suspend fun benchmarks(): List<BenchmarkResult>
}

data class BenchmarkRegistryEntry(
    val name: String,
    val create: () -> SerdeBenchmark,
)

object BenchmarkRegistry {
    val entries: MutableList<BenchmarkRegistryEntry> = mutableListOf()

    fun register(name: String, create: () -> SerdeBenchmark) {
        entries.add(BenchmarkRegistryEntry(name, create))
    }
}
