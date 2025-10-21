/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.businessmetrics.containsBusinessMetric
import aws.smithy.kotlin.runtime.client.ProtocolResponseInterceptorContext
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.TestEngine
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class S3TransferManagerBusinessMetricsTest {
    @Test
    fun s3Transfer(): Unit = runBlocking {
        val message = "Hello World"
        val testInterceptor = object : HttpInterceptor {
            override fun readAfterTransmit(context: ProtocolResponseInterceptorContext<Any, HttpRequest, HttpResponse>) {
                assert(context.executionContext.containsBusinessMetric(AwsBusinessMetric.S3_TRANSFER))
            }
        }

        S3Client {
            region = "us-west-2"
            httpClient = TestEngine()
            interceptors += testInterceptor
            credentialsProvider = StaticCredentialsProvider(Credentials("akid", "secret"))
        }.use { s3Client ->
            S3TransferManager {
                client = s3Client
            }.uploadFile {
                bucket = "b"
                key = "k"
                body = ByteStream.fromString(message)
            }
        }
    }
}
