/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject

import aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager
import aws.sdk.kotlin.hll.s3transfermanager.model.MultipartDownloadType
import aws.sdk.kotlin.hll.s3transfermanager.utils.S3TransferManagerException
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectResponse
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// TODO: Setup e2e test environment - can't run these every build and in CI
private const val TEST_BUCKET = "aoperez"
private const val TEST_REGION = "us-west-2"

class DownloadObjectTest {
    @Ignore
    @Test
    fun singlePartDownloadWithHandler(): Unit = runBlocking {
        S3Client { region = TEST_REGION }.use { s3Client ->
            val key = "download-test-small"

            s3Client.putObject {
                bucket = TEST_BUCKET
                this.key = key
                body = ByteStream.fromString("Hello World")
            }

            val receivedParts = mutableListOf<ByteArray>()
            S3TransferManager(s3Client).downloadObject({
                bucket = TEST_BUCKET
                this.key = key
           }) { response: GetObjectResponse ->
                receivedParts.add(response.body?.toByteArray() ?: byteArrayOf())
            }

            assertEquals(1, receivedParts.size)
            assertEquals("Hello World", receivedParts.first().decodeToString())
        }
    }

    @Ignore
    @Test
    fun multipartDownloadByPart(): Unit = runBlocking {
        S3Client { region = TEST_REGION }.use { s3Client ->
            val key = "download-test-multipart-part"
            val body = Random.nextBytes(10 * 1024 * 1024) // 10 MB

            s3Client.putObject {
                bucket = TEST_BUCKET
                this.key = key
                this.body = ByteStream.fromBytes(body)
            }

            val receivedParts = mutableListOf<ByteArray>()
            S3TransferManager(s3Client) {
                targetPartSizeBytes = 5L * 1024L * 1024L // 5 MB
                multipartDownloadType = MultipartDownloadType.Part
            }.downloadObject({
                bucket = TEST_BUCKET
                this.key = key
            }) { getObjectResponse: GetObjectResponse ->
                receivedParts.add(getObjectResponse.body?.toByteArray() ?: byteArrayOf())
            }

            assertEquals(2, receivedParts.size)
        }
    }

    @Ignore
    @Test
    fun multipartDownloadByRange(): Unit = runBlocking {
        S3Client { region = TEST_REGION }.use { s3Client ->
            val key = "download-test-multipart-range"
            val body = Random.nextBytes(10 * 1024 * 1024) // 10 MB

            s3Client.putObject {
                bucket = TEST_BUCKET
                this.key = key
                this.body = ByteStream.fromBytes(body)
            }

            val receivedParts = mutableListOf<ByteArray>()
            S3TransferManager(s3Client) {
                targetPartSizeBytes = 5L * 1024L * 1024L // 5 MB
                multipartDownloadType = MultipartDownloadType.Range
            }.downloadObject({
                bucket = TEST_BUCKET
                this.key = key
            }) { getObjectResponse: GetObjectResponse ->
                receivedParts.add(getObjectResponse.body?.toByteArray() ?: byteArrayOf())
            }

            assertEquals(2, receivedParts.size)
        }
    }

    @Ignore
    @Test
    fun downloadWithSmallLastPart(): Unit = runBlocking {
        S3Client { region = TEST_REGION }.use { s3Client ->
            val key = "download-test-small-last-part"
            val body = Random.nextBytes(12 * 1024 * 1024) // 12 MB (last part will only be 2MB with 5MB parts)

            s3Client.putObject {
                bucket = TEST_BUCKET
                this.key = key
                this.body = ByteStream.fromBytes(body)
            }

            val receivedParts = mutableListOf<ByteArray>()
            S3TransferManager(s3Client) {
                targetPartSizeBytes = 5L * 1024L * 1024L
                multipartDownloadType = MultipartDownloadType.Range
            }.downloadObject({
                bucket = TEST_BUCKET
                this.key = key
            }) { getObjectResponse: GetObjectResponse ->
                receivedParts.add(getObjectResponse.body?.toByteArray() ?: byteArrayOf())
            }

            assertEquals(3, receivedParts.size)
        }
    }

    @Ignore
    @Test
    fun singlePartDownloadContentLengthAndRange(): Unit = runBlocking {
        S3Client { region = TEST_REGION }.use { s3Client ->
            val key = "download-test-content-length"
            val content = "Hello World"

            s3Client.putObject {
                bucket = TEST_BUCKET
                this.key = key
                body = ByteStream.fromString(content)
            }

            val response = S3TransferManager(s3Client).downloadObject({
                bucket = TEST_BUCKET
                this.key = key
            }) { _: GetObjectResponse -> }

            assertEquals(content.length.toLong(), response.contentLength)
            assertEquals("bytes=0-${content.length - 1}/${content.length}", response.contentRange)
        }
    }

    @Ignore
    @Test
    fun multipartDownloadContentLengthAndRange(): Unit = runBlocking {
        S3Client { region = TEST_REGION }.use { s3Client ->
            val key = "download-test-multipart-content-length"
            val bodySize = 10 * 1024 * 1024 // 10 MB

            s3Client.putObject {
                bucket = TEST_BUCKET
                this.key = key
                body = ByteStream.fromBytes(Random.nextBytes(bodySize))
            }

            val response = S3TransferManager(s3Client) {
                targetPartSizeBytes = 5L * 1024L * 1024L
                multipartDownloadType = MultipartDownloadType.Range
            }.downloadObject({
                bucket = TEST_BUCKET
                this.key = key
            }) { _: GetObjectResponse -> }

            assertEquals(bodySize.toLong(), response.contentLength)
            assertEquals("bytes=0-${bodySize - 1}/$bodySize", response.contentRange)
        }
    }

    @Ignore
    @Test
    fun singlePartDownloadToFile(): Unit = runBlocking {
        S3Client { region = TEST_REGION }.use { s3Client ->
            val key = "download-test-file-small"
            val content = "Hello World"
            val downloadPath = "/tmp/s3tm-test-single-${Random.nextInt()}"

            s3Client.putObject {
                bucket = TEST_BUCKET
                this.key = key
                body = ByteStream.fromString(content)
            }

            try {
                S3TransferManager(s3Client).downloadObject<Unit>(
                    downloadObjectRequest = {
                        bucket = TEST_BUCKET
                        this.key = key
                    },
                    downloadPath = downloadPath,
                )

                val fileContent = PlatformProvider.System.read(downloadPath, readAll = true)
                assertEquals(content, fileContent.decodeToString())
            } finally {
                PlatformProvider.System.delete(downloadPath, mustExist = false)
            }
        }
    }

    @Ignore
    @Test
    fun multipartDownloadToFile(): Unit = runBlocking {
        S3Client { region = TEST_REGION }.use { s3Client ->
            val key = "download-test-file-multipart"
            val bodySize = 10 * 1024 * 1024 // 10 MB
            val body = Random.nextBytes(bodySize)
            val downloadPath = "/tmp/s3tm-test-multipart-${Random.nextInt()}"

            s3Client.putObject {
                bucket = TEST_BUCKET
                this.key = key
                this.body = ByteStream.fromBytes(body)
            }

            try {
                S3TransferManager(s3Client) {
                    targetPartSizeBytes = 5L * 1024L * 1024L
                    multipartDownloadType = MultipartDownloadType.Range
                }.downloadObject<Unit>(
                    downloadObjectRequest = {
                        bucket = TEST_BUCKET
                        this.key = key
                    },
                    downloadPath = downloadPath,
                )

                val fileContent = PlatformProvider.System.read(downloadPath, readAll = true)
                assertEquals(bodySize, fileContent.size)
                assertEquals(true, body.contentEquals(fileContent))
            } finally {
                PlatformProvider.System.delete(downloadPath, mustExist = false)
            }
        }
    }

    @Ignore
    @Test
    fun downloadToFileWithSmallLastPart(): Unit = runBlocking {
        S3Client { region = TEST_REGION }.use { s3Client ->
            val key = "download-test-file-small-last"
            val bodySize = 12 * 1024 * 1024 // 12 MB
            val body = Random.nextBytes(bodySize)
            val downloadPath = "/tmp/s3tm-test-small-last-${Random.nextInt()}"

            s3Client.putObject {
                bucket = TEST_BUCKET
                this.key = key
                this.body = ByteStream.fromBytes(body)
            }

            try {
                S3TransferManager(s3Client) {
                    targetPartSizeBytes = 5L * 1024L * 1024L
                    multipartDownloadType = MultipartDownloadType.Range
                }.downloadObject<Unit>(
                    downloadObjectRequest = {
                        bucket = TEST_BUCKET
                        this.key = key
                    },
                    downloadPath = downloadPath,
                )

                val fileContent = PlatformProvider.System.read(downloadPath, readAll = true)
                assertEquals(bodySize, fileContent.size)
                assertEquals(true, body.contentEquals(fileContent))
            } finally {
                PlatformProvider.System.delete(downloadPath, mustExist = false)
            }
        }
    }

    @Ignore
    @Test
    fun downloadFailsWithNoPathOrHandler(): Unit = runBlocking {
        S3Client { region = TEST_REGION }.use { s3Client ->
            assertFailsWith<S3TransferManagerException> {
                S3TransferManager(s3Client) {}.downloadObject<Unit>(
                    downloadObjectRequest = {
                        bucket = TEST_BUCKET
                        key = "any-key"
                    }
                )
            }
        }
    }
}
