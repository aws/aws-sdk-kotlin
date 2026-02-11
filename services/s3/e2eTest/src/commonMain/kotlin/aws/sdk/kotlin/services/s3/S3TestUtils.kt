/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.ExpirationStatus
import aws.sdk.kotlin.services.s3.model.LifecycleRule
import aws.sdk.kotlin.services.s3.paginators.listObjectsV2Paginated
import aws.sdk.kotlin.services.s3.waiters.waitUntilBucketExists
import aws.sdk.kotlin.services.s3.waiters.waitUntilBucketNotExists
import aws.sdk.kotlin.services.sts.StsClient
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.SdkHttpClient
import aws.smithy.kotlin.runtime.http.complete
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.text.ensurePrefix
import aws.smithy.kotlin.runtime.util.Uuid
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

object S3TestUtils {

    const val DEFAULT_REGION = "us-west-2"

    // The E2E test account only has permission to operate on buckets with the prefix "s3-test-bucket-"
    private const val TEST_BUCKET_PREFIX = "s3-test-bucket-"

    private var sharedBucket: String? = null
    private val bucketMutex = Mutex()

    private val sharedDirectoryBuckets: MutableMap<String, String> = mutableMapOf()
    private val directoryBucketMutex = Mutex()

    suspend fun getOrCreateSharedBucket(client: S3Client, region: String = DEFAULT_REGION): String =
        sharedBucket ?: bucketMutex.withLock {
            sharedBucket ?: getTestBucket(client, region).also { sharedBucket = it }
        }

    suspend fun cleanupSharedBucket(client: S3Client) {
        sharedBucket?.let { bucket ->
            deleteBucketContents(client, bucket)
        }
    }

    suspend fun getOrCreateSharedDirectoryBuckets(client: S3Client, suffix: String): List<String> =
        directoryBucketMutex.withLock {
            (0 until 3).map { index ->
                val key = "$suffix:$index"
                sharedDirectoryBuckets[key] ?: getTestDirectoryBucket(client, suffix).also {
                    sharedDirectoryBuckets[key] = it
                }
            }
        }

    suspend fun cleanupSharedDirectoryBuckets(client: S3Client, suffix: String) {
        (0 until 3).forEach { index ->
            val key = "$suffix:$index"
            sharedDirectoryBuckets[key]?.let { bucket ->
                deleteBucketContents(client, bucket)
            }
        }
    }

    private const val S3_MAX_BUCKET_NAME_LENGTH = 63 // https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html
    private const val S3_EXPRESS_DIRECTORY_BUCKET_SUFFIX = "--x-s3"

    suspend fun getTestBucket(
        client: S3Client,
        region: String? = null,
        accountId: String? = null,
    ): String = getBucketWithPrefix(client, TEST_BUCKET_PREFIX, region, accountId)

    suspend fun getBucketWithPrefix(
        client: S3Client,
        prefix: String,
        region: String? = null,
        accountId: String? = null,
    ): String = withTimeout(60.seconds) {
        val buckets = client.listBuckets()
            .buckets
            ?.mapNotNull { it.name }

        var testBucket = buckets?.firstOrNull { bucketName ->
            bucketName.startsWith(prefix) &&
                region?.let {
                    client.getBucketLocation {
                        bucket = bucketName
                        expectedBucketOwner = accountId
                    }.locationConstraint?.value == region
                } ?: true
        }

        if (testBucket == null) {
            testBucket = prefix + Uuid.random().toString()
            println("Creating S3 bucket: $testBucket")

            client.createBucket {
                bucket = testBucket
                createBucketConfiguration {
                    locationConstraint = BucketLocationConstraint.fromValue(region ?: client.config.region!!)
                }
            }

            client.waitUntilBucketExists { bucket = testBucket }
        } else {
            println("Using existing S3 bucket: $testBucket")
        }

        client.putBucketLifecycleConfiguration {
            bucket = testBucket
            lifecycleConfiguration {
                rules = listOf(
                    LifecycleRule {
                        expiration { days = 1 }
                        filter { this.prefix = "" }
                        status = ExpirationStatus.Enabled
                        id = "delete-old"
                    },
                )
            }
        }

        testBucket
    }

    suspend fun getTestDirectoryBucket(client: S3Client, suffix: String) = withTimeout(60.seconds) {
        var testBucket = client.listBuckets()
            .buckets
            ?.mapNotNull { it.name }
            ?.firstOrNull { it.startsWith(TEST_BUCKET_PREFIX) && it.endsWith(S3_EXPRESS_DIRECTORY_BUCKET_SUFFIX) }

        if (testBucket == null) {
            // Adding S3 Express suffix surpasses the bucket name length limit... trim the UUID if needed
            testBucket = TEST_BUCKET_PREFIX +
                Uuid.random().toString().subSequence(0 until (S3_MAX_BUCKET_NAME_LENGTH - TEST_BUCKET_PREFIX.length - suffix.ensurePrefix("--").length)) +
                suffix.ensurePrefix("--")

            println("Creating S3 Express directory bucket: $testBucket")

            val availabilityZone = testBucket // s3-test-bucket-UUID--use1-az4--x-s3
                .removeSuffix(S3_EXPRESS_DIRECTORY_BUCKET_SUFFIX) // s3-test-bucket-UUID--use1-az4
                .substringAfterLast("--") // use1-az4

            client.createBucket {
                bucket = testBucket
                createBucketConfiguration {
                    location = LocationInfo {
                        type = LocationType.AvailabilityZone
                        name = availabilityZone
                    }
                    bucket = BucketInfo {
                        type = BucketType.Directory
                        dataRedundancy = DataRedundancy.SingleAvailabilityZone
                    }
                }
            }
        } else {
            println("Using existing S3 Express directory bucket: $testBucket")
        }

        client.putBucketLifecycleConfiguration {
            bucket = testBucket
            lifecycleConfiguration {
                rules = listOf(
                    LifecycleRule {
                        expiration { days = 1 }
                        filter { this.prefix = "" }
                        status = ExpirationStatus.Enabled
                        id = "delete-old"
                    },
                )
            }
        }

        testBucket
    }

    suspend fun deleteBucketAndAllContents(client: S3Client, bucketName: String): Unit = coroutineScope {
        deleteBucketContents(client, bucketName)

        try {
            println("Deleting S3 bucket: $bucketName")
            client.deleteBucket { bucket = bucketName }

            client.waitUntilBucketNotExists {
                bucket = bucketName
            }
        } catch (ex: Exception) {
            println("Failed to delete bucket: $bucketName")
            throw ex
        }
    }

    suspend fun deleteBucketContents(client: S3Client, bucketName: String) = coroutineScope {
        val scope = this

        try {
            println("Deleting contents of S3 buckets: $bucketName")
            val dispatcher = Dispatchers.Default.limitedParallelism(64)
            val jobs = mutableListOf<Job>()

            client.listObjectsV2Paginated { bucket = bucketName }
                .mapNotNull { it.contents }
                .collect { contents ->
                    val job = scope.launch(dispatcher) {
                        client.deleteObjects {
                            bucket = bucketName
                            delete {
                                objects = contents.mapNotNull(Object::key).map { ObjectIdentifier { key = it } }
                            }
                        }
                    }
                    jobs.add(job)
                }

            jobs.joinAll()
        } catch (ex: Exception) {
            println("Failed to delete buckets contents: $bucketName")
            throw ex
        }
    }

    suspend fun responseCodeFromPut(engine: HttpClientEngine, presignedRequest: HttpRequest, content: String): Int {
        val request = HttpRequest(
            method = HttpMethod.PUT,
            url = presignedRequest.url,
            headers = presignedRequest.headers,
            body = HttpBody.fromBytes(content.encodeToByteArray())
        )

        val call = SdkHttpClient(engine).call(request)
        val statusCode = call.response.status.value
        call.complete()
        return statusCode
    }

    internal suspend fun getAccountId(): String {
        val accountId = StsClient {
            region = "us-west-2"
        }.use {
            it.getCallerIdentity().account
        }

        return checkNotNull(accountId) { "Unable to get AWS account ID" }
    }

    internal suspend fun deleteMultiPartUploads(client: S3Client, bucketName: String) {
        client.listMultipartUploads {
            bucket = bucketName
        }.uploads?.forEach { upload ->
            client.abortMultipartUpload {
                bucket = bucketName
                key = upload.key
                uploadId = upload.uploadId
            }
        }
    }
}


