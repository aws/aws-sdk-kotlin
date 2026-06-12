/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.endpoint

import aws.sdk.kotlin.services.lambda.endpoints.DefaultLambdaEndpointProvider
import aws.sdk.kotlin.services.lambda.endpoints.LambdaEndpointParameters
import kotlinx.benchmark.*

/**
 * Benchmarks for Lambda endpoint resolution.
 *
 * Uses [startCoroutineUninterceptedOrReturn] to invoke the suspend function directly
 * without coroutine infrastructure overhead. This matches production behavior where
 * resolveEndpoint is called from within an existing coroutine and never actually suspends.
 *
 * Each benchmark binds parameters per the smithy.endpoints#endpointTests trait definitions.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class LambdaEndpointResolutionBenchmark {

    private val provider = DefaultLambdaEndpointProvider()

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

    // For region us-east-1 with FIPS disabled and DualStack disabled
    @Benchmark
    fun usEast1Standard(blackhole: Blackhole) {
        blackhole.consume(resolveEndpointSync { provider.resolveEndpoint(usEast1StandardParams) })
    }

    // For region us-gov-east-1 with FIPS enabled and DualStack enabled
    @Benchmark
    fun usGovEast1FipsDualStack(blackhole: Blackhole) {
        blackhole.consume(resolveEndpointSync { provider.resolveEndpoint(usGovEast1FipsDualStackParams) })
    }
}
