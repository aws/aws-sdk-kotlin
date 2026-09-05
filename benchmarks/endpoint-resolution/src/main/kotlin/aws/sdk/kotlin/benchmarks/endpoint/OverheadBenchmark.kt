/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.endpoint

import aws.smithy.kotlin.runtime.client.endpoints.Endpoint
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.benchmark.*
import kotlinx.coroutines.runBlocking

/**
 * Measures the overhead of different suspend function invocation mechanisms.
 * - [suspendLambdaCall]: startCoroutineUninterceptedOrReturn
 * - [runBlockingCall]: runBlocking
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class OverheadBenchmark {

    private val precomputedEndpoint = Endpoint(Url.parse("https://lambda.us-east-1.amazonaws.com"))

    private suspend fun noOpResolve(): Endpoint = precomputedEndpoint

    @Benchmark
    fun suspendLambdaCall(blackhole: Blackhole) {
        blackhole.consume(resolveEndpointSync { noOpResolve() })
    }

    @Benchmark
    fun runBlockingCall(blackhole: Blackhole) {
        blackhole.consume(runBlocking { noOpResolve() })
    }
}
