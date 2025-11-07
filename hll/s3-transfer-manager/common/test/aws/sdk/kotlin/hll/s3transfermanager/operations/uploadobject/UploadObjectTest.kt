/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.uploadobject

import aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.content.ByteStream
import kotlinx.coroutines.runBlocking
import kotlin.invoke
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test

// TODO: Setup e2e test environment - can't run these every build and in CI
class UploadObjectTest {
    @Ignore
    @Test
    fun singleObjectUpload(): Unit = runBlocking {
        S3Client {
            region = "us-west-2"
        }.use { s3Client ->
            S3TransferManager(s3Client) {}.uploadObject {
                bucket = "aoperez"
                key = "k"
                body = ByteStream.fromString("Hello World")
            }
        }
    }

    @Ignore
    @Test
    fun emptyBody(): Unit = runBlocking {
        S3Client {
            region = "us-west-2"
        }.use { s3Client ->
            S3TransferManager(s3Client) {}.uploadObject {
                bucket = "aoperez"
                key = "k"
                body = ByteStream.fromString("")
            }
        }
    }

    @Ignore
    @Test
    fun multipartUpload(): Unit = runBlocking {
        val messageLength = 10L * 1024L * 1024L // 10 MB

        S3Client {
            region = "us-west-2"
        }.use { s3Client ->
            S3TransferManager(s3Client) {
                multipartUploadThresholdBytes = 1
                targetPartSizeBytes = 5L * 1024L * 1024L // 5 MB
            }.uploadObject {
                bucket = "aoperez"
                key = "mpuK"
                body = randomBody(messageLength)
            }
        }
    }

    @Ignore
    @Test
    fun smallLastPart(): Unit = runBlocking {
        val messageLength = 12L * 1024L * 1024L // 12 MB (last part will only be 2MB)

        S3Client {
            region = "us-west-2"
        }.use { s3Client ->
            S3TransferManager(s3Client) {
                multipartUploadThresholdBytes = 1
                targetPartSizeBytes = 5L * 1024L * 1024L // 5 MB
            }.uploadObject {
                bucket = "aoperez"
                key = "mpuK"
                body = randomBody(messageLength)
            }
        }
    }
}

private fun randomBody(sizeInBytes: Long): ByteStream =
    ByteStream.fromBytes(
        Random.nextBytes(
            ByteArray(
                sizeInBytes.toInt(),
            ),
        ),
    )
