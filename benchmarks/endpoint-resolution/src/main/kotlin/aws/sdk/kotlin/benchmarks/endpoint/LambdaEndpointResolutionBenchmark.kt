/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.endpoint

import aws.sdk.kotlin.services.lambda.endpoints.DefaultLambdaEndpointProvider
import aws.sdk.kotlin.services.lambda.endpoints.LambdaEndpointParameters
import aws.smithy.kotlin.runtime.client.endpoints.Endpoint
import kotlinx.benchmark.*
import org.openjdk.jmh.annotations.State
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn

/**
 * Benchmarks for Lambda endpoint resolution.
 *
 * Uses [startCoroutineUninterceptedOrReturn] to invoke the suspend function directly
 * without coroutine infrastructure overhead. This matches production behavior where
 * resolveEndpoint is called from within an existing coroutine and never actually suspends.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class LambdaEndpointResolutionBenchmark {

    private val provider = DefaultLambdaEndpointProvider()

    private val completion = Continuation<Endpoint>(EmptyCoroutineContext) { result ->
        result.getOrThrow()
    }

    private val usEast1StandardParams = LambdaEndpointParameters {
        region = "us-east-1"
        useFips = false
        useDualStack = false
    }

    private val usGovEast1FipsDualStackParams = LambdaEndpointParameters {
        region = "us-gov-east-1"
        useFips = true
        useDualStack = true
    }

    @Benchmark
    fun usEast1Standard(blackhole: Blackhole) {
        val result = suspend { provider.resolveEndpoint(usEast1StandardParams) }
            .startCoroutineUninterceptedOrReturn(completion)
        check(result !== COROUTINE_SUSPENDED) { "resolveEndpoint suspended unexpectedly" }
        blackhole.consume(result)
    }

    @Benchmark
    fun usGovEast1FipsDualStack(blackhole: Blackhole) {
        val result = suspend { provider.resolveEndpoint(usGovEast1FipsDualStackParams) }
            .startCoroutineUninterceptedOrReturn(completion)
        check(result !== COROUTINE_SUSPENDED) { "resolveEndpoint suspended unexpectedly" }
        blackhole.consume(result)
    }
}
