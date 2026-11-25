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
    private val _entries = mutableListOf<BenchmarkRegistryEntry>()
    val entries: List<BenchmarkRegistryEntry>
        get() = _entries

    fun register(name: String, create: () -> SerdeBenchmark) {
        _entries.add(BenchmarkRegistryEntry(name, create))
    }
}
