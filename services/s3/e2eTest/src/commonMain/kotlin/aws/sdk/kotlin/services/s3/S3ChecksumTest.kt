/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.e2etest.S3TestUtils.responseCodeFromPut
import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.model.*
import aws.sdk.kotlin.services.s3.presigners.presignPutObject
import aws.smithy.kotlin.runtime.content.*
import aws.smithy.kotlin.runtime.hashing.crc32
import aws.smithy.kotlin.runtime.io.SdkSource
import aws.smithy.kotlin.runtime.io.source
import aws.smithy.kotlin.runtime.testing.AfterAll
import aws.smithy.kotlin.runtime.testing.BeforeAll
import aws.smithy.kotlin.runtime.testing.RandomTempFile
import aws.smithy.kotlin.runtime.testing.TestInstance
import aws.smithy.kotlin.runtime.testing.TestLifecycle
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestLifecycle.PER_CLASS)
class S3ChecksumTest {
    private val client = S3TestUtils.createClient()
    private lateinit var testBucket: String
    private fun testKey(suffix: String): String = "test-object-$suffix"

    @BeforeAll
    fun setUp(): Unit = runBlocking {
        testBucket = S3TestUtils.createTestBucket(client, "checksums")
    }

    @AfterAll
    fun cleanUp(): Unit = runBlocking {
        S3TestUtils.deleteBucket(client, testBucket)
        client.close()
    }

    @Test
    fun testPutObject() = runBlocking {
        val testBody = "Hello World"
        val testKey = testKey("basic")

        client.putObject {
            bucket = testBucket
            key = testKey
            body = ByteStream.fromString(testBody)
        }

        client.getObject(
            GetObjectRequest {
                bucket = testBucket
                key = testKey
            },
        ) { actual ->
            assertEquals(testBody, actual.body?.decodeToString() ?: "")
        }
    }

    @Test
    fun testPutObjectWithEmptyBody(): Unit = runBlocking {
        val testKey = testKey("empty")
        val testBody = ""

        client.putObject {
            bucket = testBucket
            key = testKey
        }

        client.getObject(
            GetObjectRequest {
                bucket = testBucket
                key = testKey
            },
        ) { actual ->
            assertEquals(testBody, actual.body?.decodeToString() ?: "")
        }
    }

    @Test
    fun testPutObjectAwsChunkedEncoded(): Unit = runBlocking {
        val testKey = testKey("chunked")
        val testBody = "Hello World"
        val testBodyBytes = testBody.encodeToByteArray()

        // Exercise aws-chunked streaming-payload signing + trailing checksums.
        // isEligibleForAwsChunkedStreaming = SourceContent/ChannelContent && contentLength != null &&
        // (isOneShot || contentLength > AWS_CHUNKED_THRESHOLD). A one-shot source with a known
        // content length satisfies the isOneShot disjunct, so it is eligible at any size and
        // FlexibleChecksumsRequestInterceptor routes it to calculateAwsChunkedStreamingChecksum
        // (checksum computed during transmission as a trailer).
        client.putObject {
            bucket = testBucket
            key = testKey
            body = object : ByteStream.SourceStream() {
                override fun readFrom(): SdkSource = testBodyBytes.source()
                override val contentLength: Long = testBodyBytes.size.toLong()
                override val isOneShot: Boolean = true
            }
        }

        client.getObject(
            GetObjectRequest {
                bucket = testBucket
                key = testKey
            },
        ) { actual ->
            assertEquals(testBody, actual.body?.decodeToString() ?: "")
        }
    }

    @Test
    fun testMultiPartUpload(): Unit = runBlocking {
        val testKey = testKey("multipart")

        val partSize = 5 * 1024 * 1024 // 5 MB - min part size
        val contentSize: Long = 8 * 1024 * 1024 // 2 parts
        val file = RandomTempFile(sizeInBytes = contentSize)

        val expectedChecksum = file.readBytes().crc32()

        val testUploadId = client.createMultipartUpload {
            bucket = testBucket
            key = testKey
        }.uploadId

        val fileBytes = file.readBytes()
        val chunks = fileBytes.chunk(partSize).toList()
        val uploadedParts = chunks.mapIndexed { index, chunk ->
            val adjustedIndex = index + 1 // index starts from 0 but partNumber needs to start from 1

            async {
                client.uploadPart {
                    bucket = testBucket
                    key = testKey
                    partNumber = adjustedIndex
                    uploadId = testUploadId
                    body = ByteStream.fromBytes(chunk)
                }.let {
                    CompletedPart {
                        partNumber = adjustedIndex
                        eTag = it.eTag
                    }
                }
            }
        }.awaitAll()

        client.completeMultipartUpload {
            bucket = testBucket
            key = testKey
            uploadId = testUploadId
            multipartUpload = CompletedMultipartUpload {
                parts = uploadedParts
            }
        }

        client.getObject(
            GetObjectRequest {
                bucket = testBucket
                key = testKey
            },
        ) { actual ->
            val actualChecksum = actual.body!!.toByteArray().crc32()
            assertEquals(actualChecksum, expectedChecksum)
        }

        file.delete()
    }

    @Test
    fun testPresignedUrlNoDefault() = runBlocking {
        val contents = "presign-test"

        val unsignedPutRequest = PutObjectRequest {
            bucket = testBucket
            key = testKey("presigned-auto-checksum")
        }
        val presignedPutRequest = client.presignPutObject(unsignedPutRequest, 60.seconds)

        assertFalse(presignedPutRequest.url.toString().contains("x-amz-checksum-crc32"))
        assertTrue(responseCodeFromPut(client.config.httpClient, presignedPutRequest, contents) in 200..299)
    }

    @Test
    fun testPresignedUrlChecksumValue() = runBlocking {
        val contents = "presign-test"

        val unsignedPutRequest = PutObjectRequest {
            bucket = testBucket
            key = testKey("presigned-provided-checksum")
            checksumCrc32 = "dBBx+Q=="
        }
        val presignedPutRequest = client.presignPutObject(unsignedPutRequest, 60.seconds)

        assertTrue(presignedPutRequest.url.toString().contains("x-amz-checksum-crc32"))
        assertTrue(responseCodeFromPut(client.config.httpClient, presignedPutRequest, contents) in 200..299)
    }
}
