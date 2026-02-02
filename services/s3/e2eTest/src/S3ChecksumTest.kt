/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.e2etest.S3TestUtils.deleteBucketContents
import aws.sdk.kotlin.e2etest.S3TestUtils.deleteMultiPartUploads
import aws.sdk.kotlin.e2etest.S3TestUtils.getAccountId
import aws.sdk.kotlin.e2etest.S3TestUtils.getTestBucket
import aws.sdk.kotlin.e2etest.S3TestUtils.responseCodeFromPut
import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.model.*
import aws.sdk.kotlin.services.s3.presigners.presignPutObject
import aws.smithy.kotlin.runtime.content.*
import aws.smithy.kotlin.runtime.hashing.crc32
import aws.smithy.kotlin.runtime.testing.RandomTempFile
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3ChecksumTest {
    private val client = S3Client { region = "us-west-2" }
    private lateinit var testBucket: String
    private fun testKey(): String = "test-object" + UUID.randomUUID()

    @BeforeAll
    private fun setUp(): Unit = runBlocking {
        val accountId = getAccountId()
        testBucket = getTestBucket(client, "us-west-2", accountId)
    }

    @AfterAll
    private fun cleanUp(): Unit = runBlocking {
        deleteMultiPartUploads(client, testBucket)
        deleteBucketContents(client, testBucket)
        client.close()
    }

    @Test
    fun testPutObject(): Unit = runBlocking {
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
    fun testPutObjectWithEmptyBody(): Unit = runBlocking {
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
    fun testPutObjectAwsChunkedEncoded(): Unit = runBlocking {
        val testKey = testKey()
        val testBody = "Hello World"

        val tempFile = File.createTempFile("test", ".txt").also {
            it.writeText(testBody)
            it.deleteOnExit()
        }
        val inputStream = FileInputStream(tempFile)

        client.putObject {
            bucket = testBucket
            key = testKey
            body = ByteStream.fromInputStream(inputStream, testBody.length.toLong())
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
        val testKey = testKey()

        val partSize = 5 * 1024 * 1024 // 5 MB - min part size
        val contentSize: Long = 8 * 1024 * 1024 // 2 parts
        val file = RandomTempFile(sizeInBytes = contentSize)

        val expectedChecksum = file.readBytes().crc32()

        val testUploadId = client.createMultipartUpload {
            bucket = testBucket
            key = testKey
        }.uploadId

        val uploadedParts = file.chunk(partSize).mapIndexed { index, chunk ->
            val adjustedIndex = index + 1 // index starts from 0 but partNumber needs to start from 1

            runBlocking {
                client.uploadPart {
                    bucket = testBucket
                    key = testKey
                    partNumber = adjustedIndex
                    uploadId = testUploadId
                    body = file.asByteStream(chunk)
                }.let {
                    CompletedPart {
                        partNumber = adjustedIndex
                        eTag = it.eTag
                    }
                }
            }
        }.toList()

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
        assertTrue(responseCodeFromPut(presignedPutRequest, contents) in 200..299)
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
        assertTrue(responseCodeFromPut(presignedPutRequest, contents) in 200..299)
    }
}
