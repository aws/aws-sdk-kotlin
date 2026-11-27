/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.definitions

import aws.sdk.kotlin.benchmarks.service.Common
import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.smithy.kotlin.runtime.content.ByteStream

class S3Benchmark : ServiceBenchmark<S3Client> {
    private val bucketName = Common.random("sdk-benchmark-bucket-")

    companion object {
        private const val KEY = "test-object"
        private const val CONTENTS = "test-contents"
    }

    override suspend fun client() = S3Client.fromEnvironment {
        retryStrategy = Common.noRetries
        telemetryProvider = Common.telemetryProvider
        httpClient {
            telemetryProvider = Common.telemetryProvider
        }
    }

    override suspend fun setup(client: S3Client) {
        client.createBucket {
            bucket = bucketName
            createBucketConfiguration {
                locationConstraint = BucketLocationConstraint.fromValue(client.config.region!!)
            }
        }
    }

    override val operations get() = listOf(headObjectBenchmark, putObjectBenchmark)

    override suspend fun tearDown(client: S3Client) {
        client.deleteBucket { bucket = bucketName }
    }

    private val headObjectBenchmark = object : AbstractOperationBenchmark<S3Client>("HeadObject") {
        override suspend fun setup(client: S3Client) {
            client.putObject {
                bucket = bucketName
                key = KEY
                body = ByteStream.fromString(CONTENTS)
            }
        }

        override suspend fun transact(client: S3Client) {
            client.headObject {
                bucket = bucketName
                key = KEY
            }
        }

        override suspend fun tearDown(client: S3Client) {
            client.deleteObject {
                bucket = bucketName
                key = KEY
            }
        }
    }

    private val putObjectBenchmark = object : AbstractOperationBenchmark<S3Client>("PutObject") {
        override suspend fun transact(client: S3Client) {
            client.putObject {
                bucket = bucketName
                key = KEY
                body = ByteStream.fromString(CONTENTS)
            }
        }

        override suspend fun tearDown(client: S3Client) {
            client.deleteObject {
                bucket = bucketName
                key = KEY
            }
        }
    }
}
