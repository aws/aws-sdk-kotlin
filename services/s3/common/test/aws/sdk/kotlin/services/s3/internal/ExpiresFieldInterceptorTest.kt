/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HeadersBuilder
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExpiresFieldInterceptorTest {
    private fun newTestClient(
        status: HttpStatusCode = HttpStatusCode.OK,
        headers: Headers = Headers.Empty,
    ): S3Client = S3Client {
        region = "us-east-1"
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = "accessKeyId"
            secretAccessKey = "secretAccessKey"
        }
        httpClient = buildTestConnection {
            expect(HttpResponse(status, headers, body = HttpBody.Empty))
        }
    }

    @Test
    fun testHandlesParsableExpiresField() = runTest {
        val expectedHeaders = HeadersBuilder().apply {
            append("Expires", "Mon, 1 Apr 2024 00:00:00 +0000")
        }.build()

        newTestClient(headers = expectedHeaders).use { s3 ->
            s3.getObject(
                GetObjectRequest {
                    bucket = "test"
                    key = "key"
                },
            ) {
                assertEquals(Instant.fromEpochSeconds(1711929600), it.expires)
                assertEquals("Mon, 1 Apr 2024 00:00:00 +0000", it.expiresString)
            }
        }
    }

    @Test
    fun testHandlesUnparsableExpiresField() = runTest {
        val invalidExpiresField = "Tomorrow or maybe the day after?"

        val expectedHeaders = HeadersBuilder().apply {
            append("Expires", invalidExpiresField)
        }.build()

        newTestClient(headers = expectedHeaders).use { s3 ->
            s3.getObject(
                GetObjectRequest {
                    bucket = "test"
                    key = "key"
                },
            ) {
                assertNull(it.expires)
                assertEquals(invalidExpiresField, it.expiresString)
            }
        }
    }
}
