/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.endpoint

import aws.sdk.kotlin.services.lambda.endpoints.DefaultLambdaEndpointProvider
import aws.sdk.kotlin.services.lambda.endpoints.LambdaEndpointParameters
import kotlinx.benchmark.*
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.State

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class LambdaEndpointResolutionBenchmark {

    private val provider = DefaultLambdaEndpointProvider()

    // For region us-east-1 with FIPS disabled and DualStack disabled
    @Benchmark
    fun usEast1Standard() = runBlocking {
        provider.resolveEndpoint(
            LambdaEndpointParameters {
                region = "us-east-1"
                useFips = false
                useDualStack = false
            },
        )
    }

    // For region us-gov-east-1 with FIPS enabled and DualStack enabled
    @Benchmark
    fun usGovEast1FipsDualStack() = runBlocking {
        provider.resolveEndpoint(
            LambdaEndpointParameters {
                region = "us-gov-east-1"
                useFips = true
                useDualStack = true
            },
        )
    }
}
