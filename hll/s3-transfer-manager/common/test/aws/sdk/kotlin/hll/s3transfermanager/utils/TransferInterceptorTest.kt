/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.utils

import aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.MutableTransferContext
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferContext
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferInterceptor
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CompleteMultipartUploadRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.httptest.TestEngine
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertThrows
import kotlin.collections.plusAssign
import kotlin.invoke
import kotlin.test.Test
import kotlin.test.assertEquals

class TransferInterceptorTest {
    @Test
    fun interceptorsCanReadAndModify(): Unit = runBlocking {
        val message = "Hello World"

        S3Client {
            region = "us-west-2"
            httpClient = TestEngine()
            credentialsProvider = StaticCredentialsProvider(Credentials("akid", "secret"))
        }.use { s3Client ->
            S3TransferManager(s3Client) {
                interceptors += object : TransferInterceptor {
                    // Test reads
                    override fun readBeforeTransferInitiated(context: TransferContext) {
                        assert(context.transferredBytes == 0L)
                        assert(context.s3Request is PutObjectRequest)
                    }
                    override fun readBeforeTransferCompleted(context: TransferContext) {
                        assert(context.transferredBytes == message.length.toLong())
                        assert(context.s3Response is PutObjectResponse)
                    }

                    // Test modifications
                    override fun modifyBeforeTransferCompleted(context: MutableTransferContext) {
                        context.s3Request = CompleteMultipartUploadRequest {}
                        context.transferredBytes = message.length.toLong() * 10
                    }
                    override fun readAfterTransferCompleted(context: TransferContext) {
                        assert(context.s3Request is CompleteMultipartUploadRequest)
                        assert(context.transferredBytes == message.length.toLong() * 10)
                    }
                }
            }.uploadObject {
                bucket = "b"
                key = "k"
                body = ByteStream.fromString(message)
            }
        }
    }

    @Test
    fun interceptorsExceptionsAreSuppressed(): Unit = runBlocking {
        val message = "Hello World"

        val exception = assertThrows<Exception> {
            S3Client {
                region = "us-west-2"
                httpClient = TestEngine()
                credentialsProvider = StaticCredentialsProvider(Credentials("akid", "secret"))
            }.use { s3Client ->
                S3TransferManager(s3Client) {
                    interceptors += listOf(
                        object : TransferInterceptor {
                            override fun readBeforeTransferInitiated(context: TransferContext): Unit = throw Exception("1")
                        },
                        object : TransferInterceptor {
                            override fun readBeforeTransferInitiated(context: TransferContext): Unit = throw Exception("2")
                        },
                        object : TransferInterceptor {
                            override fun readBeforeTransferInitiated(context: TransferContext): Unit = throw Exception("3")
                        },
                    )
                }.uploadObject {
                    bucket = "b"
                    key = "k"
                    body = ByteStream.fromString(message)
                }
            }
        }

        assertEquals(exception.message, "1")
        assertEquals(exception.cause!!.suppressed[0].message, "2")
        assertEquals(exception.cause!!.suppressed[1].message, "3")
    }
}
