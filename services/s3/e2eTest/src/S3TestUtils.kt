/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.model.*
import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.ExpirationStatus
import aws.sdk.kotlin.services.s3.model.LifecycleRule
import aws.sdk.kotlin.services.s3.paginators.listObjectsV2Paginated
import aws.sdk.kotlin.services.s3.waiters.waitUntilBucketExists
import aws.sdk.kotlin.services.s3.waiters.waitUntilBucketNotExists
import aws.sdk.kotlin.services.s3control.*
import aws.sdk.kotlin.services.s3control.model.*
import aws.sdk.kotlin.services.sts.StsClient
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.util.asyncLazy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.OutputStreamWriter
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.time.Duration.Companion.seconds

object S3TestUtils {
    const val DEFAULT_REGION = "us-west-2"

    // The E2E test account only has permission to operate on buckets with the prefix "s3-test-bucket-"
    private const val TEST_BUCKET_PREFIX = "s3-test-bucket"

    private const val S3_EXPRESS_DIRECTORY_BUCKET_SUFFIX = "x-s3"

    val testRunId by lazy {
        Instant
            .now()
            .format(TimestampFormat.ISO_8601_CONDENSED)
            .lowercase()
            .also { println("Starting test run ID $it") }
    }

    suspend fun createTestBucket(
        client: S3Client,
        suffix: String,
        region: String = client.config.region!!,
    ): String = createBucket(client, TEST_BUCKET_PREFIX, suffix, region)

    suspend fun createBucket(
        client: S3Client,
        prefix: String,
        suffix: String,
        region: String = client.config.region!!,
    ): String = withTimeout(60.seconds) {
        val bucketName = "$prefix-$testRunId-$suffix"
        println("Creating S3 bucket: $bucketName")

        client.createBucket {
            bucket = bucketName
            createBucketConfiguration {
                locationConstraint = BucketLocationConstraint.fromValue(region)
            }
        }

        client.waitUntilBucketExists { bucket = bucketName }

        client.putBucketLifecycleConfiguration {
            bucket = bucketName
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

        bucketName
    }

    suspend fun createTestDirectoryBucket(
        client: S3Client,
        availabilityZone: String,
        suffix: String,
    ) = createDirectoryBucket(client, TEST_BUCKET_PREFIX, availabilityZone, suffix)

    suspend fun createDirectoryBucket(
        client: S3Client,
        prefix: String,
        availabilityZone: String,
        suffix: String,
    ) = withTimeout(60.seconds) {
        val bucketName = "$prefix-$testRunId-$suffix--$availabilityZone--$S3_EXPRESS_DIRECTORY_BUCKET_SUFFIX"
        println("Creating S3 Express directory bucket: $bucketName")

        client.createBucket {
            bucket = bucketName
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

        bucketName
    }

    suspend fun deleteBucket(client: S3Client, bucketName: String): Unit = coroutineScope {
        deleteBucketContents(client, bucketName)
        deleteMultiPartUploads(client, bucketName)

        try {
            client.deleteBucket { bucket = bucketName }

            client.waitUntilBucketNotExists {
                bucket = bucketName
            }
        } catch (ex: Exception) {
            println("Failed to delete bucket: $bucketName")
            throw ex
        }
    }

    private suspend fun deleteBucketContents(client: S3Client, bucketName: String): Unit = coroutineScope {
        val scope = this

        try {
            println("Deleting S3 buckets contents: $bucketName")
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

    private suspend fun deleteMultiPartUploads(client: S3Client, bucketName: String) {
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

    fun responseCodeFromPut(presignedRequest: HttpRequest, content: String): Int {
        val url = URL(presignedRequest.url.toString())
        val connection: HttpsURLConnection = url.openConnection() as HttpsURLConnection
        presignedRequest.headers.forEach { key, values ->
            connection.setRequestProperty(key, values.first())
        }

        connection.doOutput = true
        connection.requestMethod = "PUT"
        val out = OutputStreamWriter(connection.outputStream)
        out.write(content)
        out.close()

        if (connection.errorStream != null) {
            error("request failed: ${connection.errorStream?.bufferedReader()?.readText()}")
        }

        return connection.responseCode
    }

    private val accountId = asyncLazy {
        println("Getting account ID")

        val accountId = StsClient { region = DEFAULT_REGION }.use { sts ->
            sts.getCallerIdentity().account
        }

        checkNotNull(accountId) { "Unable to get AWS account ID" }
    }

    internal suspend fun getAccountId(): String = accountId.get()

    fun createClient(builder: S3Client.Config.Builder.() -> Unit = { }): S3Client = S3Client {
        region = DEFAULT_REGION

        // Apply builder block after setting default region in case of overrides
        builder()
    }
}
