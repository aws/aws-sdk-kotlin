/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager

import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.fromInputStream
import aws.smithy.kotlin.runtime.testing.RandomTempFile
import kotlinx.coroutines.runBlocking
import kotlin.test.Ignore
import kotlin.test.Test

// TODO: Setup e2e test environment - can't run these every build and in CI
class UploadFileTest {
    @Ignore
    @Test
    fun singleObjectUpload(): Unit = runBlocking {
        val message = "Hello World"

        S3Client {
            region = "us-west-2"
        }.use { s3Client ->
            S3TransferManager {
                client = s3Client
            }.uploadFile {
                bucket = "aoperez"
                key = "k"
                body = ByteStream.fromString(message)
                contentLength = message.length.toLong()
            }
        }
    }

    @Ignore
    @Test
    fun multiplePartUpload(): Unit = runBlocking {
        val messageLength = 10L * 1024L * 1024L // 10 MB
        val file = RandomTempFile(messageLength)

        S3Client {
            region = "us-west-2"
        }.use { s3Client ->
            S3TransferManager {
                client = s3Client
                multipartUploadThreshold = 1
                targePartSize = 5L * 1024L * 1024L // 5 MB
            }.uploadFile {
                bucket = "aoperez"
                key = "mpuK"
                body = ByteStream.fromInputStream(file.inputStream(), messageLength)
                contentLength = messageLength
            }
        }
    }
}
