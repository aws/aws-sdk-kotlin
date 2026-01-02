/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.http.HttpException
import aws.smithy.kotlin.runtime.util.Uuid
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.invoke
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

/**
 * Reproduces "unexpected end of stream" errors as seen in https://github.com/aws/aws-sdk-kotlin/issues/1214
 * and ensures they are resolved by OkHttp's retryOnConnectionFailure option
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConnectionResetTest {
    private val client = S3Client {
        region = S3TestUtils.DEFAULT_REGION
    }

    private lateinit var testBucket: String

    @BeforeAll
    fun createResources(): Unit = runBlocking {
        testBucket = S3TestUtils.getTestBucket(client)
    }

    @AfterAll
    fun cleanup() = runBlocking {
        S3TestUtils.deleteBucketAndAllContents(client, testBucket)
    }

    @Test
    fun testConnectionResetDoesntThrow(): Unit = runBlocking {
        // Launch multiple coroutines to populate connection pool
        val jobs = (1..10).map {
            async { client.putTestObject() }
        }
        jobs.awaitAll()
        // Connections are now idle in the pool

        // Wait for S3 to close stale connections
        delay(7.seconds)

        // Try to re-use a connection TODO Update this to assert success after enabling retryOnConnectionFailure by default
        assertFailsWith<RetryOnConnectionFailureException> {
            client.putTestObject()
        }
    }

    suspend fun S3Client.putTestObject() {
        val putObjectRequest = PutObjectRequest {
            bucket = testBucket
            key = Uuid.random().toString()
            body = ByteStream.fromString("Content")
        }

        try {
            client.putObject(putObjectRequest)
        } catch (e: HttpException) {
            if (e.cause is IOException && e.cause?.message?.contains("unexpected end of stream") == true) {
                throw RetryOnConnectionFailureException("SDK unexpectedly threw java.io.IOException which should have been retried by OkHttp's retryOnConnectionFailure feature", e)
            }
        }
    }
}

class RetryOnConnectionFailureException(message: String, cause: Exception? = null) : Exception(message, cause)
