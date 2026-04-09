/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.express.S3_EXPRESS_SESSION_TOKEN_HEADER
import aws.sdk.kotlin.services.s3.model.*
import aws.sdk.kotlin.services.s3.presigners.presignPutObject
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.decodeToString
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes

/**
 * Tests for S3 Express operations
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3ExpressTest {
    companion object {
        private const val AVAILABILITY_ZONE = "usw2-az1" // us-west-2 availability zone 1
    }

    private val client = S3TestUtils.createClient()

    private lateinit var testBuckets: List<String>

    @BeforeAll
    fun setup(): Unit = runBlocking {
        // create a few test buckets to test the credentials cache
        testBuckets = (1..3).map { index ->
            S3TestUtils.createTestDirectoryBucket(client, AVAILABILITY_ZONE, index.toString())
        }
    }

    @AfterAll
    fun cleanup(): Unit = runBlocking {
        testBuckets.forEach { bucket -> S3TestUtils.deleteBucket(client, bucket) }
        client.close()
    }

    @Test
    fun testPutObject() = runBlocking {
        val content = "30 minutes, or it's free!"
        val keyName = "express.txt"

        testBuckets.forEach { bucketName ->
            val trackingInterceptor = S3ExpressInvocationTrackingInterceptor()
            client.withConfig {
                interceptors += trackingInterceptor
            }.use { trackingClient ->
                trackingClient.putObject {
                    bucket = bucketName
                    key = keyName
                    body = ByteStream.fromString(content)
                }

                val req = GetObjectRequest {
                    bucket = bucketName
                    key = keyName
                }

                val respContent = client.getObject(req) {
                    it.body?.decodeToString()
                }

                assertEquals(content, respContent)
                assertEquals(1, trackingInterceptor.s3ExpressInvocations)
            }
        }
    }

    @Ignore
    @Test
    fun testPresignedPutObject() = runBlocking {
        val content = "Presign this!"
        val keyName = "express-presigned.txt"

        testBuckets.forEach { bucketName ->
            val presigned = client.presignPutObject(
                PutObjectRequest {
                    bucket = bucketName
                    key = keyName
                    body = ByteStream.fromString(content)
                },
                5.minutes,
            )

            // FIXME Presigned requests should use S3 Express Auth Scheme resulting in `X-Amz-S3session-Token`
            // https://github.com/awslabs/aws-sdk-kotlin/issues/1236
            assertTrue(presigned.url.parameters.decodedParameters.contains(S3_EXPRESS_SESSION_TOKEN_HEADER))
        }
    }

    @Test
    fun testChecksums() = runBlocking {
        val bucketName = testBuckets.first() // only need one bucket for this test

        val keysToDelete = listOf("checksums.txt", "delete-me.txt", "dont-forget-about-me.txt")
        keysToDelete.forEach {
            client.putObject {
                bucket = bucketName
                key = it
                body = ByteStream.fromString("Check out these sums!")
            }
        }

        client.withConfig {
            interceptors += CRC32ChecksumValidatingInterceptor()
        }.use { validatingClient ->
            // s3:DeleteObjects requires a checksum, even if the user doesn't specify one.
            // normally the SDK would default to MD5, but S3 Express must default to CRC32 instead.
            val req = DeleteObjectsRequest {
                bucket = bucketName
                delete = Delete {
                    objects = keysToDelete.map {
                        ObjectIdentifier { key = it }
                    }
                }
            }

            validatingClient.deleteObjects(req)
        }
    }

    @Test
    fun testUploadPartContainsCRC32Checksum() = runBlocking {
        val testBucket = testBuckets.first()
        val testObject = "I-will-be-uploaded-in-parts-!"

        // Parts need to be at least 5 MB
        val partOne = "Hello".repeat(1_048_576)
        val partTwo = "World".repeat(1_048_576)

        val testUploadId = client.createMultipartUpload {
            bucket = testBucket
            key = testObject
        }.uploadId

        var eTagPartOne: String?
        var eTagPartTwo: String?

        client.withConfig {
            interceptors += CRC32ChecksumValidatingInterceptor()
        }.use { validatingClient ->
            eTagPartOne = validatingClient.uploadPart {
                bucket = testBucket
                key = testObject
                partNumber = 1
                uploadId = testUploadId
                body = ByteStream.fromString(partOne)
            }.eTag

            eTagPartTwo = validatingClient.uploadPart {
                bucket = testBucket
                key = testObject
                partNumber = 2
                uploadId = testUploadId
                body = ByteStream.fromString(partTwo)
            }.eTag
        }

        client.completeMultipartUpload {
            bucket = testBucket
            key = testObject
            uploadId = testUploadId
            multipartUpload = CompletedMultipartUpload {
                parts = listOf(
                    CompletedPart {
                        partNumber = 1
                        eTag = eTagPartOne
                    },
                    CompletedPart {
                        partNumber = 2
                        eTag = eTagPartTwo
                    },
                )
            }
        }
    }

    private class S3ExpressInvocationTrackingInterceptor : HttpInterceptor {
        var s3ExpressInvocations = 0

        override fun readAfterSigning(context: ProtocolRequestInterceptorContext<Any, HttpRequest>) {
            if (context.protocolRequest.headers.contains(S3_EXPRESS_SESSION_TOKEN_HEADER)) {
                s3ExpressInvocations += 1
            }
        }
    }

    private class CRC32ChecksumValidatingInterceptor : HttpInterceptor {
        override fun readAfterSigning(context: ProtocolRequestInterceptorContext<Any, HttpRequest>) {
            val headers = context.protocolRequest.headers
            if (headers.contains(S3_EXPRESS_SESSION_TOKEN_HEADER)) {
                assertTrue(headers.contains("x-amz-checksum-crc32"), "Failed to find x-amz-checksum-crc32 header")
                assertFalse(headers.contains("Content-MD5"), "Unexpectedly found Content-MD5 header")
            }
        }
    }
}
