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
import aws.smithy.kotlin.runtime.testing.RandomTempFile
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class S3ChecksumTest {
    private val client = S3Client { region = "us-west-2" }
    private lateinit var testBucket: String
    private fun testKey(): String = "test-object${Random.nextInt()}"

    @BeforeTest
    fun setUp() = runBlocking {
        testBucket = S3TestUtils.getOrCreateSharedBucket(client, "us-west-2")
    }

    @AfterTest
    fun cleanUp() = runBlocking {
        S3TestUtils.cleanupSharedBucket(client)
    }

    @Test
    fun testPutObject() = runBlocking {
        val testBody = "Hello World"
        val testKey = testKey()

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
    fun testPutObjectWithEmptyBody() = runBlocking {
        val testKey = testKey()
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
    fun testPutObjectAwsChunkedEncoded() = runBlocking {
        val testKey = testKey()
        val testBody = "Hello World"

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
    fun testMultiPartUpload() = runBlocking<Unit> {
        val testKey = testKey()
        val partSize = 5 * 1024 * 1024
        val contentSize: Long = 8 * 1024 * 1024
        val file = RandomTempFile(sizeInBytes = contentSize)

        val expectedChecksum = file.readBytes().crc32()

        val testUploadId = client.createMultipartUpload {
            bucket = testBucket
            key = testKey
        }.uploadId

        val fileBytes = file.readBytes()
        val chunks = fileBytes.chunk(partSize).toList()
        val uploadedParts = chunks.mapIndexed { index, chunk ->
            val adjustedIndex = index + 1

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
            key = testKey()
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
            key = testKey()
            checksumCrc32 = "dBBx+Q=="
        }
        val presignedPutRequest = client.presignPutObject(unsignedPutRequest, 60.seconds)

        assertTrue(presignedPutRequest.url.toString().contains("x-amz-checksum-crc32"))
        assertTrue(responseCodeFromPut(client.config.httpClient, presignedPutRequest, contents) in 200..299)
    }
}
