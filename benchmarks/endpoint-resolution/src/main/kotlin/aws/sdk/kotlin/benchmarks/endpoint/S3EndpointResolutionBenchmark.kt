/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.endpoint

import aws.sdk.kotlin.services.s3.endpoints.DefaultS3EndpointProvider
import aws.sdk.kotlin.services.s3.endpoints.S3EndpointParameters
import aws.smithy.kotlin.runtime.client.endpoints.Endpoint
import kotlinx.benchmark.*
import org.openjdk.jmh.annotations.State
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn

/**
 * Benchmarks for S3 endpoint resolution.
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
class S3EndpointResolutionBenchmark {

    private val provider = DefaultS3EndpointProvider()

    private val completion = Continuation<Endpoint>(EmptyCoroutineContext) { result ->
        result.getOrThrow()
    }

    private val vanillaVirtualAddressingParams = S3EndpointParameters {
        accelerate = false
        bucket = "bucket-name"
        forcePathStyle = false
        region = "us-west-2"
        useDualStack = false
        useFips = false
    }

    private val vanillaPathStyleParams = S3EndpointParameters {
        accelerate = false
        bucket = "bucket-name"
        forcePathStyle = true
        region = "us-west-2"
        useDualStack = false
        useFips = false
    }

    private val dataPlaneShortZoneNameParams = S3EndpointParameters {
        region = "us-east-1"
        bucket = "mybucket--abcd-ab1--x-s3"
        useFips = false
        useDualStack = false
        accelerate = false
        useS3ExpressControlEndpoint = false
    }

    private val vanillaAccessPointArnParams = S3EndpointParameters {
        accelerate = false
        bucket = "arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint"
        forcePathStyle = false
        region = "us-west-2"
        useDualStack = false
        useFips = false
    }

    private val s3OutpostsVanillaParams = S3EndpointParameters {
        region = "us-west-2"
        useFips = false
        useDualStack = false
        accelerate = false
        bucket = "arn:aws:s3-outposts:us-west-2:123456789012:outpost/op-01234567890123456/accesspoint/reports"
    }

    @Benchmark
    fun vanillaVirtualAddressing(blackhole: Blackhole) {
        val result = suspend { provider.resolveEndpoint(vanillaVirtualAddressingParams) }
            .startCoroutineUninterceptedOrReturn(completion)
        check(result !== COROUTINE_SUSPENDED) { "resolveEndpoint suspended unexpectedly" }
        blackhole.consume(result)
    }

    @Benchmark
    fun vanillaPathStyle(blackhole: Blackhole) {
        val result = suspend { provider.resolveEndpoint(vanillaPathStyleParams) }
            .startCoroutineUninterceptedOrReturn(completion)
        check(result !== COROUTINE_SUSPENDED) { "resolveEndpoint suspended unexpectedly" }
        blackhole.consume(result)
    }

    @Benchmark
    fun dataPlaneShortZoneName(blackhole: Blackhole) {
        val result = suspend { provider.resolveEndpoint(dataPlaneShortZoneNameParams) }
            .startCoroutineUninterceptedOrReturn(completion)
        check(result !== COROUTINE_SUSPENDED) { "resolveEndpoint suspended unexpectedly" }
        blackhole.consume(result)
    }

    @Benchmark
    fun vanillaAccessPointArn(blackhole: Blackhole) {
        val result = suspend { provider.resolveEndpoint(vanillaAccessPointArnParams) }
            .startCoroutineUninterceptedOrReturn(completion)
        check(result !== COROUTINE_SUSPENDED) { "resolveEndpoint suspended unexpectedly" }
        blackhole.consume(result)
    }

    @Benchmark
    fun s3OutpostsVanilla(blackhole: Blackhole) {
        val result = suspend { provider.resolveEndpoint(s3OutpostsVanillaParams) }
            .startCoroutineUninterceptedOrReturn(completion)
        check(result !== COROUTINE_SUSPENDED) { "resolveEndpoint suspended unexpectedly" }
        blackhole.consume(result)
    }
}
