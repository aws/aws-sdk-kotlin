/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CompleteMultipartUploadRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.httptest.TestEngine
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class TransferInterceptorTest {
    @Test
    fun interceptorsCanReadAndModify(): Unit = runBlocking {
        val message = "Hello World"

        S3Client {
            region = "us-west-2"
            httpClient = TestEngine()
        }.use { s3Client ->
            S3TransferManager {
                client = s3Client
                interceptors += object : TransferInterceptor {
                    // Test reads
                    override fun readBeforeTransferInitiated(context: TransferContext) {
                        assert(context.transferredBytes == 0L)
                        assert(context.request is PutObjectRequest)
                    }
                    override fun readBeforeTransferCompleted(context: TransferContext) {
                        assert(context.transferredBytes == message.length.toLong())
                        assert(context.response is PutObjectResponse)
                    }

                    // Test modifications
                    override fun modifyBeforeTransferCompleted(context: TransferContext): TransferContext {
                        val newContext = context.copy()
                        newContext.request = CompleteMultipartUploadRequest {}
                        newContext.transferredBytes = message.length.toLong() * 10
                        return newContext
                    }
                    override fun readAfterTransferCompleted(context: TransferContext) {
                        assert(context.request is CompleteMultipartUploadRequest)
                        assert(context.transferredBytes == message.length.toLong() * 10)
                    }
                }
            }.uploadFile {
                bucket = "b"
                key = "k"
                body = ByteStream.fromString(message)
                contentLength = message.length.toLong()
            }
        }
    }
}
