/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.express

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Attributes
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CreateSessionRequest
import aws.sdk.kotlin.services.s3.model.CreateSessionResponse
import aws.sdk.kotlin.services.s3.model.SessionCredentials
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.time.ManualClock
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

private val DEFAULT_BASE_CREDENTIALS = Credentials("accessKeyId", "secretAccessKey", "sessionToken")

class DefaultS3ExpressCredentialsProviderTest {
    @Test
    fun testCreateSessionCredentials() = runTest {
        val timeSource = TestTimeSource()
        val clock = ManualClock()

        val expectedCredentials = SessionCredentials {
            accessKeyId = "access"
            secretAccessKey = "secret"
            sessionToken = "session"
            expiration = clock.now() + 5.minutes
        }

        TestS3Client(expectedCredentials).use { client ->
            DefaultS3ExpressCredentialsProvider(timeSource, clock).use { provider ->
                val credentials = provider.createSessionCredentials(
                    S3ExpressCredentialsCacheKey("bucket", DEFAULT_BASE_CREDENTIALS),
                    client,
                )
                assertFalse(credentials.isExpired)
                assertEquals(timeSource.markNow() + 5.minutes, credentials.expiresAt)
            }
        }
    }

    @Test
    fun testSyncRefresh() = runTest {
        val timeSource = TestTimeSource()
        val clock = ManualClock()

        // Entry expired 30 seconds ago, next `resolve` call should trigger a sync refresh
        val cache = S3ExpressCredentialsCache()
        val entry = getCacheEntry(timeSource.markNow() - 30.seconds)
        cache.put(entry.key, entry.value)

        val expectedCredentials = SessionCredentials {
            accessKeyId = "access"
            secretAccessKey = "secret"
            sessionToken = "session"
            expiration = clock.now() + 5.minutes
        }

        TestS3Client(expectedCredentials).use { testClient ->
            DefaultS3ExpressCredentialsProvider(timeSource, clock, cache, refreshBuffer = 1.minutes).use { provider ->
                val attributes = ExecutionContext.build {
                    this.attributes[S3Attributes.ExpressClient] = testClient
                    this.attributes[S3Attributes.Bucket] = "bucket"
                }

                provider.resolve(attributes)
            }
            assertEquals(1, testClient.numCreateSession)
        }
    }

    @Test
    fun testAsyncRefresh() = runTest {
        val timeSource = TestTimeSource()
        val clock = ManualClock()

        // Entry expires in 30 seconds, refresh buffer is 1 minute. Next `resolve` call should trigger the async refresh
        val cache = S3ExpressCredentialsCache()
        val entry = getCacheEntry(timeSource.markNow() + 30.seconds)
        cache.put(entry.key, entry.value)

        val expectedCredentials = SessionCredentials {
            accessKeyId = "access"
            secretAccessKey = "secret"
            sessionToken = "session"
            expiration = clock.now() + 5.minutes
        }

        TestS3Client(expectedCredentials).use { testClient ->
            val provider = DefaultS3ExpressCredentialsProvider(timeSource, clock, cache, refreshBuffer = 1.minutes)

            val attributes = ExecutionContext.build {
                this.attributes[S3Attributes.ExpressClient] = testClient
                this.attributes[S3Attributes.Bucket] = "bucket"
            }
            provider.resolve(attributes)

            // allow the async refresh to initiate before closing the provider
            runBlocking { delay(500.milliseconds) }

            provider.close()
            provider.coroutineContext.job.join()

            assertEquals(1, testClient.numCreateSession)
        }
    }

    @Test
    fun testAsyncRefreshDebounce() = runTest {
        val timeSource = TestTimeSource()
        val clock = ManualClock()

        // Entry expires in 30 seconds, refresh buffer is 1 minute. Next `resolve` call should trigger the async refresh
        val cache = S3ExpressCredentialsCache()
        val entry = getCacheEntry(expiration = timeSource.markNow() + 30.seconds)
        cache.put(entry.key, entry.value)

        val expectedCredentials = SessionCredentials {
            accessKeyId = "access"
            secretAccessKey = "secret"
            sessionToken = "session"
            expiration = clock.now() + 5.minutes
        }

        TestS3Client(expectedCredentials).use { testClient ->
            val provider = DefaultS3ExpressCredentialsProvider(timeSource, clock, cache, refreshBuffer = 1.minutes)

            val attributes = ExecutionContext.build {
                this.attributes[S3Attributes.ExpressClient] = testClient
                this.attributes[S3Attributes.Bucket] = "bucket"
            }
            val calls = (1..5).map {
                async { provider.resolve(attributes) }
            }
            calls.awaitAll()

            // allow the async refresh to initiate before closing the provider
            runBlocking { delay(500.milliseconds) }

            provider.close()
            provider.coroutineContext.job.join()

            assertEquals(1, testClient.numCreateSession)
        }
    }

    @Test
    fun testAsyncRefreshHandlesFailures() = runTest {
        val timeSource = TestTimeSource()
        val clock = ManualClock()

        // Entry expires in 30 seconds, refresh buffer is 1 minute. Next `resolve` call should trigger the async refresh
        val cache = S3ExpressCredentialsCache()
        val successEntry = getCacheEntry(timeSource.markNow() + 30.seconds, bucket = "SuccessfulBucket")
        val failedEntry = getCacheEntry(timeSource.markNow() + 30.seconds, bucket = "ExceptionBucket")
        cache.put(successEntry.key, successEntry.value)
        cache.put(failedEntry.key, failedEntry.value)

        val expectedCredentials = SessionCredentials {
            accessKeyId = "access"
            secretAccessKey = "secret"
            sessionToken = "session"
            expiration = clock.now() + 5.minutes
        }

        // client should throw an exception when `ExceptionBucket` credentials are fetched, but it should be caught
        TestS3Client(expectedCredentials, throwExceptionOnBucketNamed = "ExceptionBucket").use { testClient ->
            val provider = DefaultS3ExpressCredentialsProvider(timeSource, clock, cache, refreshBuffer = 1.minutes)
            val attributes = ExecutionContext.build {
                this.attributes[S3Attributes.ExpressClient] = testClient
                this.attributes[S3Attributes.Bucket] = "ExceptionBucket"
            }
            provider.resolve(attributes)

            withTimeout(5.seconds) {
                while (testClient.numCreateSession != 1) {
                    yield()
                }
            }
            assertEquals(1, testClient.numCreateSession)

            attributes[S3Attributes.Bucket] = "SuccessfulBucket"
            provider.resolve(attributes)

            withTimeout(5.seconds) {
                while (testClient.numCreateSession != 2) {
                    yield()
                }
            }

            provider.close()
            provider.coroutineContext.job.join()

            assertEquals(2, testClient.numCreateSession)
        }
    }

    @Test
    fun testAsyncRefreshClosesImmediately() = runTest {
        val timeSource = TestTimeSource()
        val clock = ManualClock()

        // Entry expires in 30 seconds, refresh buffer is 1 minute. Next `resolve` call should trigger the async refresh
        val cache = S3ExpressCredentialsCache()
        val entry = getCacheEntry(timeSource.markNow() + 30.seconds)
        cache.put(entry.key, entry.value)

        val expectedCredentials = SessionCredentials {
            accessKeyId = "access"
            secretAccessKey = "secret"
            sessionToken = "session"
            expiration = clock.now() + 5.minutes
        }

        val provider = DefaultS3ExpressCredentialsProvider(timeSource, clock, cache, refreshBuffer = 1.minutes)

        class BlockingTestS3Client : TestS3Client(expectedCredentials) {
            override suspend fun createSession(input: CreateSessionRequest): CreateSessionResponse {
                delay(10.seconds)
                numCreateSession += 1
                return CreateSessionResponse { credentials = expectedCredentials }
            }
        }

        BlockingTestS3Client().use { blockingTestS3Client ->
            val attributes = ExecutionContext.build {
                this.attributes[S3Attributes.ExpressClient] = blockingTestS3Client
                this.attributes[S3Attributes.Bucket] = "bucket"
            }

            withTimeout(5.seconds) {
                provider.resolve(attributes)
                provider.close()
            }
            assertEquals(0, blockingTestS3Client.numCreateSession)
        }
    }

    /**
     * Get an instance of [Map.Entry<S3ExpressCredentialsCacheKey, S3ExpressCredentialsCacheValue>] using the given [expiration],
     * [bucket], and optional [bootstrapCredentials] and [sessionCredentials].
     */
    private fun getCacheEntry(
        expiration: ComparableTimeMark,
        bucket: String = "bucket",
        bootstrapCredentials: Credentials = Credentials(accessKeyId = "accessKeyId", secretAccessKey = "secretAccessKey", sessionToken = "sessionToken"),
        sessionCredentials: Credentials = Credentials(accessKeyId = "s3AccessKeyId", secretAccessKey = "s3SecretAccessKey", sessionToken = "s3SessionToken"),
    ): S3ExpressCredentialsCacheEntry = mapOf(
        S3ExpressCredentialsCacheKey(bucket, bootstrapCredentials) to S3ExpressCredentialsCacheValue(ExpiringValue(sessionCredentials, expiration)),
    ).entries.first()

    /**
     * A test S3Client used to mock calls to s3:CreateSession.
     * @param expectedCredentials the expected session credentials returned from s3:CreateSession
     * @param client the base S3 client used to implement other operations, though they are unused.
     * @param throwExceptionOnBucketNamed an optional bucket name, which when specified and present in the [CreateSessionRequest], will
     * cause the client to throw an exception instead of returning credentials. Used for testing s3:CreateSession failures.
     */
    private open inner class TestS3Client(
        val expectedCredentials: SessionCredentials,
        val baseCredentials: Credentials = DEFAULT_BASE_CREDENTIALS,
        val client: S3Client = S3Client { credentialsProvider = StaticCredentialsProvider(baseCredentials) },
        val throwExceptionOnBucketNamed: String? = null,
    ) : S3Client by client {
        var numCreateSession = 0

        override suspend fun createSession(input: CreateSessionRequest): CreateSessionResponse {
            numCreateSession += 1
            throwExceptionOnBucketNamed?.let {
                if (input.bucket == it) {
                    throw Exception("Failed to create session credentials for bucket: $throwExceptionOnBucketNamed")
                }
            }
            return CreateSessionResponse { credentials = expectedCredentials }
        }

        override fun close() {
            client.close()
        }
    }
}
