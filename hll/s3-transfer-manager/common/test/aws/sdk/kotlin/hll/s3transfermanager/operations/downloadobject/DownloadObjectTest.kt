/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject

import aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager
import aws.sdk.kotlin.hll.s3transfermanager.model.MultipartDownloadType
import aws.sdk.kotlin.hll.s3transfermanager.utils.S3TransferManagerException
import aws.sdk.kotlin.runtime.auth.credentials.ProcessCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.testing.AfterAll
import aws.smithy.kotlin.runtime.testing.BeforeAll
import aws.smithy.kotlin.runtime.testing.TestInstance
import aws.smithy.kotlin.runtime.testing.TestLifecycle
import aws.smithy.kotlin.runtime.util.PlatformProvider
import io.ktor.http.invoke
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// TODO: Setup e2e test environment - can't run these every build and in CI
private const val TEST_BUCKET = "aoperez"
private const val TEST_REGION = "us-west-2"
private const val SMALL_CONTENT = "Hello World"
private const val MULTIPART_SIZE = 10 * 1024 * 1024 // 10 MB
private const val UNEVEN_SIZE = 12 * 1024 * 1024 // 12 MB
private const val TARGET_PART_SIZE = 5L * 1024L * 1024L // 5 MB

// @Ignore // TODO: Setup e2e test environment
@TestInstance(TestLifecycle.PER_CLASS)
class DownloadObjectTest {
    private val testSuffix = Random.nextInt(0, 10_000_000)
    private val smallKey = "download-test-small-$testSuffix"
    private val multipartKey = "download-test-multipart-$testSuffix"
    private val unevenKey = "download-test-uneven-$testSuffix"

    private lateinit var s3Client: S3Client
    private lateinit var multipartBody: ByteArray
    private lateinit var unevenBody: ByteArray
    private val system = PlatformProvider.System
    private val downloadPaths = mutableListOf<String>()

    @BeforeAll
    fun setup(): Unit = runBlocking {
        s3Client = S3Client {
            region = TEST_REGION
            credentialsProvider = ProcessCredentialsProvider("isengardcli credentials --awscli aoperez@amazon.com --role Admin")
        }
        multipartBody = Random.nextBytes(MULTIPART_SIZE)
        unevenBody = Random.nextBytes(UNEVEN_SIZE)

        s3Client.putObject {
            bucket = TEST_BUCKET
            key = smallKey
            body = ByteStream.fromString(SMALL_CONTENT)
        }

        // Upload using multipart so S3 stores part metadata. Objects uploaded via putObject have no
        // part structure, so S3 returns the entire object for partNumber=1 and partsCount is null.
        // Multipart-uploaded objects support part-based downloads (MultipartDownloadType.Part).
        val tm = S3TransferManager(s3Client) {
            multipartUploadThresholdBytes = 1
            targetPartSizeBytes = TARGET_PART_SIZE
        }
        tm.uploadObject {
            bucket = TEST_BUCKET
            key = multipartKey
            body = ByteStream.fromBytes(multipartBody)
        }
        tm.uploadObject {
            bucket = TEST_BUCKET
            key = unevenKey
            body = ByteStream.fromBytes(unevenBody)
        }
    }

    @AfterAll
    fun cleanup(): Unit = runBlocking {
        listOf(smallKey, multipartKey, unevenKey).forEach { key ->
            s3Client.deleteObject {
                bucket = TEST_BUCKET
                this.key = key
            }
        }
        downloadPaths.forEach { system.delete(it, mustExist = false) }
        s3Client.close()
    }

    private fun tempPath(): String {
        val path = "/tmp/s3tm-test-${Random.nextInt()}"
        downloadPaths.add(path)
        return path
    }

    // Single part - handler

    @Test
    fun singlePartDownloadWithHandler(): Unit = runBlocking {
        val receivedParts = mutableListOf<ByteArray>()
        S3TransferManager(s3Client).downloadObject({
            bucket = TEST_BUCKET
            key = smallKey
        }) { bytes: ByteArray ->
            receivedParts.add(bytes)
        }

        assertEquals(1, receivedParts.size)
        assertEquals(SMALL_CONTENT, receivedParts.first().decodeToString())
    }

    @Test
    fun singlePartDownloadContentLengthAndRange(): Unit = runBlocking {
        val response = S3TransferManager(s3Client).downloadObject({
            bucket = TEST_BUCKET
            key = smallKey
        }) { _: ByteArray -> }

        assertEquals(SMALL_CONTENT.length.toLong(), response.contentLength)
        assertEquals("bytes=0-${SMALL_CONTENT.length - 1}/${SMALL_CONTENT.length}", response.contentRange)
    }

    @Test
    fun singlePartDownloadToFile(): Unit = runBlocking {
        val downloadPath = tempPath()
        S3TransferManager(s3Client).downloadObject<Unit>(
            downloadObjectRequest = {
                bucket = TEST_BUCKET
                key = smallKey
            },
            downloadPath = downloadPath,
        )

        val fileContent = system.read(downloadPath, readAll = true)
        assertEquals(SMALL_CONTENT, fileContent.decodeToString())
    }

    // Multipart by part

    @Test
    fun multipartDownloadByPart(): Unit = runBlocking {
        val receivedParts = mutableListOf<ByteArray>()
        S3TransferManager(s3Client) {
            targetPartSizeBytes = TARGET_PART_SIZE
            multipartDownloadType = MultipartDownloadType.Part
        }.downloadObject({
            bucket = TEST_BUCKET
            key = multipartKey
        }) { bytes: ByteArray ->
            receivedParts.add(bytes)
        }

        assertEquals(2, receivedParts.size)
    }

    // Multipart by range

    @Test
    fun multipartDownloadByRange(): Unit = runBlocking {
        val receivedParts = mutableListOf<ByteArray>()
        S3TransferManager(s3Client) {
            targetPartSizeBytes = TARGET_PART_SIZE
            multipartDownloadType = MultipartDownloadType.Range
        }.downloadObject({
            bucket = TEST_BUCKET
            key = multipartKey
        }) { bytes: ByteArray ->
            receivedParts.add(bytes)
        }

        assertEquals(2, receivedParts.size)
    }

    @Test
    fun multipartDownloadContentLengthAndRange(): Unit = runBlocking {
        val response = S3TransferManager(s3Client) {
            targetPartSizeBytes = TARGET_PART_SIZE
            multipartDownloadType = MultipartDownloadType.Range
        }.downloadObject({
            bucket = TEST_BUCKET
            key = multipartKey
        }) { _: ByteArray -> }

        assertEquals(MULTIPART_SIZE.toLong(), response.contentLength)
        assertEquals("bytes=0-${MULTIPART_SIZE - 1}/$MULTIPART_SIZE", response.contentRange)
    }

    @Test
    fun multipartDownloadToFile(): Unit = runBlocking {
        val downloadPath = tempPath()
        S3TransferManager(s3Client) {
            targetPartSizeBytes = TARGET_PART_SIZE
            multipartDownloadType = MultipartDownloadType.Range
        }.downloadObject<Unit>(
            downloadObjectRequest = {
                bucket = TEST_BUCKET
                key = multipartKey
            },
            downloadPath = downloadPath,
        )

        val fileContent = system.read(downloadPath, readAll = true)
        assertEquals(MULTIPART_SIZE, fileContent.size)
        assertTrue(multipartBody.contentEquals(fileContent), "Downloaded file content does not match uploaded content")
    }

    // Uneven parts (12 MB with 5 MB parts = 3 parts, last is 2 MB)

    @Test
    fun downloadWithSmallLastPart(): Unit = runBlocking {
        val receivedParts = mutableListOf<ByteArray>()
        S3TransferManager(s3Client) {
            targetPartSizeBytes = TARGET_PART_SIZE
            multipartDownloadType = MultipartDownloadType.Range
        }.downloadObject({
            bucket = TEST_BUCKET
            key = unevenKey
        }) { bytes: ByteArray ->
            receivedParts.add(bytes)
        }

        assertEquals(3, receivedParts.size)
    }

    @Test
    fun downloadToFileWithSmallLastPart(): Unit = runBlocking {
        val downloadPath = tempPath()
        S3TransferManager(s3Client) {
            targetPartSizeBytes = TARGET_PART_SIZE
            multipartDownloadType = MultipartDownloadType.Range
        }.downloadObject<Unit>(
            downloadObjectRequest = {
                bucket = TEST_BUCKET
                key = unevenKey
            },
            downloadPath = downloadPath,
        )

        val fileContent = system.read(downloadPath, readAll = true)
        assertEquals(UNEVEN_SIZE, fileContent.size)
        assertTrue(unevenBody.contentEquals(fileContent), "Downloaded file content does not match uploaded content")
    }

    // Both handler and file path

    @Test
    fun downloadToFileWithHandler(): Unit = runBlocking {
        val downloadPath = tempPath()
        val receivedParts = mutableListOf<ByteArray>()
        S3TransferManager(s3Client).downloadObject(
            downloadObjectRequest = {
                bucket = TEST_BUCKET
                key = smallKey
            },
            downloadPath = downloadPath,
        ) { bytes: ByteArray ->
            receivedParts.add(bytes)
        }

        assertEquals(1, receivedParts.size)
        val fileContent = system.read(downloadPath, readAll = true)
        assertEquals(SMALL_CONTENT, fileContent.decodeToString())
    }

    // Error case

    @Test
    fun downloadFailsWithNoPathOrHandler(): Unit = runBlocking {
        assertFailsWith<S3TransferManagerException> {
            S3TransferManager(s3Client).downloadObject<Unit>(
                downloadObjectRequest = {
                    bucket = TEST_BUCKET
                    key = "any-key"
                },
            )
        }
    }
}
