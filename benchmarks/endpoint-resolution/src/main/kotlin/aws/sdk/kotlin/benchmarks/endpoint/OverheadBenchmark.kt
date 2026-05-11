/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.endpoint

import aws.smithy.kotlin.runtime.client.endpoints.Endpoint
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.benchmark.*
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.State
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn

/**
 * Measures the overhead of different suspend function invocation mechanisms.
 * - [directCall]: baseline (no suspend machinery)
 * - [suspendLambdaCall]: startCoroutineUninterceptedOrReturn (Option B)
 * - [runBlockingCall]: runBlocking (Option A)
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class OverheadBenchmark {

    private val completion = Continuation<Endpoint>(EmptyCoroutineContext) { result ->
        result.getOrThrow()
    }

    private val precomputedEndpoint = Endpoint(Url.parse("https://lambda.us-east-1.amazonaws.com"))

    private suspend fun noOpResolve(): Endpoint = precomputedEndpoint

    @Benchmark
    fun directCall(blackhole: Blackhole) {
        blackhole.consume(precomputedEndpoint)
    }

    @Benchmark
    fun suspendLambdaCall(blackhole: Blackhole) {
        val result = suspend { noOpResolve() }
            .startCoroutineUninterceptedOrReturn(completion)
        check(result !== COROUTINE_SUSPENDED)
        blackhole.consume(result)
    }

    @Benchmark
    fun runBlockingCall(blackhole: Blackhole) {
        val result = runBlocking { noOpResolve() }
        blackhole.consume(result)
    }
}
