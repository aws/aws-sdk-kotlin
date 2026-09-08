/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.http.HttpException
import aws.smithy.kotlin.runtime.testing.AfterAll
import aws.smithy.kotlin.runtime.testing.BeforeAll
import aws.smithy.kotlin.runtime.testing.TestInstance
import aws.smithy.kotlin.runtime.testing.TestLifecycle
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Reproduces "unexpected end of stream" errors as seen in https://github.com/aws/aws-sdk-kotlin/issues/1214
 * and ensures they are resolved by OkHttp's retryOnConnectionFailure option
 */
@TestInstance(TestLifecycle.PER_CLASS)
class ConnectionResetTest {
    private val client = S3TestUtils.createClient()
    private lateinit var testBucket: String

    @BeforeAll
    fun createResources(): Unit = runBlocking {
        testBucket = S3TestUtils.createTestBucket(client, "conn-reset")
    }

    @AfterAll
    fun cleanup(): Unit = runBlocking {
        S3TestUtils.deleteBucket(client, testBucket)
        client.close()
    }

    @Test
    fun testConnectionResetDoesntThrow() = runBlocking {
        // Launch multiple coroutines to populate connection pool
        val jobs = (1..10).map {
            async { client.putTestObject() }
        }
        jobs.awaitAll()
        // Connections are now idle in the pool

        // Wait for S3 to close stale connections
        delay(7.seconds)

        // Try to re-use a connection
        client.putTestObject()
    }

    private suspend fun S3Client.putTestObject() {
        val putObjectRequest = PutObjectRequest {
            bucket = testBucket
            key = (0..Int.MAX_VALUE).random().toString()
            body = ByteStream.fromString("Content")
        }

        try {
            putObject(putObjectRequest)
        } catch (e: HttpException) {
            if (e.cause?.message?.contains("unexpected end of stream") == true) {
                throw RetryOnConnectionFailureException("SDK unexpectedly threw exception which should have been retried by HTTP engine's retry feature", e)
            }
        }
    }
}

class RetryOnConnectionFailureException(message: String, cause: Exception? = null) : Exception(message, cause)
