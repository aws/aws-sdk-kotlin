/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.uploadfile

import aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.testing.RandomTempFile
import kotlinx.coroutines.runBlocking
import kotlin.invoke
import kotlin.test.Test

// TODO: Setup e2e test environment - can't run these every build and in CI
class UploadFileTest {
    @Test
    fun singleObjectUpload(): Unit = runBlocking {
        S3Client {
            region = "us-west-2"
        }.use { s3Client ->
            S3TransferManager(s3Client) {}.uploadFile {
                bucket = "aoperez"
                key = "k"
                body = ByteStream.fromString("Hello World")
            }
        }
    }

    @Test
    fun emptyBody(): Unit = runBlocking {
        S3Client {
            region = "us-west-2"
        }.use { s3Client ->
            S3TransferManager(s3Client) {}.uploadFile {
                bucket = "aoperez"
                key = "k"
                body = ByteStream.fromString("")
            }
        }
    }

    @Test
    fun multipartUpload(): Unit = runBlocking {
        val messageLength = 10L * 1024L * 1024L // 10 MB
        val file = RandomTempFile(messageLength)

        S3Client {
            region = "us-west-2"
        }.use { s3Client ->
            S3TransferManager(s3Client) {
                multipartUploadThresholdBytes = 1
                partSizeBytes = 5L * 1024L * 1024L // 5 MB
            }.uploadFile {
                bucket = "aoperez"
                key = "mpuK"
                body = file.asByteStream()
            }
        }
    }

    @Test
    fun smallLastPart(): Unit = runBlocking {
        val messageLength = 12L * 1024L * 1024L // 12 MB (last part will only be 2MB)
        val file = RandomTempFile(messageLength)

        S3Client {
            region = "us-west-2"
        }.use { s3Client ->
            S3TransferManager(s3Client) {
                multipartUploadThresholdBytes = 1
                partSizeBytes = 5L * 1024L * 1024L // 5 MB
            }.uploadFile {
                bucket = "aoperez"
                key = "mpuK"
                body = file.asByteStream()
            }
        }
    }
}
