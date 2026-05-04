
/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.endpoint

import aws.sdk.kotlin.services.s3.endpoints.DefaultS3EndpointProvider
import aws.sdk.kotlin.services.s3.endpoints.S3EndpointParameters
import kotlinx.benchmark.*
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.State

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class S3EndpointResolutionBenchmark {

    private val provider = DefaultS3EndpointProvider()

    // vanilla virtual addressing@us-west-2
    @Benchmark
    fun vanillaVirtualAddressing() = runBlocking {
        provider.resolveEndpoint(S3EndpointParameters {
            accelerate = false
            bucket = "bucket-name"
            forcePathStyle = false
            region = "us-west-2"
            useDualStack = false
            useFips = false
        })
    }

    // vanilla path style@us-west-2
    @Benchmark
    fun vanillaPathStyle() = runBlocking {
        provider.resolveEndpoint(S3EndpointParameters {
            accelerate = false
            bucket = "bucket-name"
            forcePathStyle = true
            region = "us-west-2"
            useDualStack = false
            useFips = false
        })
    }

    // Data Plane with short zone name
    @Benchmark
    fun dataPlaneShortZoneName() = runBlocking {
        provider.resolveEndpoint(S3EndpointParameters {
            region = "us-east-1"
            bucket = "mybucket--abcd-ab1--x-s3"
            useFips = false
            useDualStack = false
            accelerate = false
            useS3ExpressControlEndpoint = false
        })
    }

    // vanilla access point arn@us-west-2
    @Benchmark
    fun vanillaAccessPointArn() = runBlocking {
        provider.resolveEndpoint(S3EndpointParameters {
            accelerate = false
            bucket = "arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint"
            forcePathStyle = false
            region = "us-west-2"
            useDualStack = false
            useFips = false
        })
    }

    // S3 outposts vanilla test
    @Benchmark
    fun s3OutpostsVanilla() = runBlocking {
        provider.resolveEndpoint(S3EndpointParameters {
            region = "us-west-2"
            useFips = false
            useDualStack = false
            accelerate = false
            bucket = "arn:aws:s3-outposts:us-west-2:123456789012:outpost/op-01234567890123456/accesspoint/reports"
        })
    }
}
