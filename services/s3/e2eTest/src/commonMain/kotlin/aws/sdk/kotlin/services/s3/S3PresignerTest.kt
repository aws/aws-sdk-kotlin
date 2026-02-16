/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.e2etest.S3TestUtils.responseCodeFromPut
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.presigners.presignDeleteObject
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import aws.sdk.kotlin.services.s3.presigners.presignPutObject
import aws.sdk.kotlin.testing.PRINTABLE_CHARS
import aws.sdk.kotlin.testing.withAllEngines
import aws.smithy.kotlin.runtime.content.decodeToString
import aws.smithy.kotlin.runtime.http.SdkHttpClient
import aws.smithy.kotlin.runtime.http.complete
import aws.smithy.kotlin.runtime.http.toByteStream
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.testing.AfterAll
import aws.smithy.kotlin.runtime.testing.BeforeAll
import kotlinx.coroutines.runBlocking
import kotlin.jvm.JvmStatic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class S3PresignerTest {
    companion object {
        private lateinit var client: S3Client
        private lateinit var testBucket: String

        @BeforeAll
        @JvmStatic
        fun setup() = runBlocking {
            client = S3Client {
                region = S3TestUtils.DEFAULT_REGION
            }
            testBucket = S3TestUtils.getOrCreateSharedBucket(client)
        }

        @AfterAll
        @JvmStatic
        fun cleanup(): Unit = runBlocking {
            S3TestUtils.deleteSharedBucket(client)
            client.close()
        }
    }

    private suspend fun testPresign(client: S3Client) {
        val contents = "presign-test"
        val keyName = "foo$PRINTABLE_CHARS"

        withAllEngines { engine ->
            val httpClient = SdkHttpClient(engine)

            // PUT
            val unsignedPutRequest = PutObjectRequest {
                bucket = testBucket
                key = keyName
            }
            val presignedPutRequest = client.presignPutObject(unsignedPutRequest, 60.seconds)

            responseCodeFromPut(engine, presignedPutRequest, contents)

            // GET
            val unsignedGetRequest = GetObjectRequest {
                bucket = testBucket
                key = keyName
            }
            val presignedGetRequest = client.presignGetObject(unsignedGetRequest, 60.seconds)

            val call = httpClient.call(presignedGetRequest)
            val body = call.response.body.toByteStream()?.decodeToString()
            call.complete()
            assertEquals(200, call.response.status.value)
            assertEquals(contents, body)

            // DELETE
            val unsignedDeleteRequest = DeleteObjectRequest {
                bucket = testBucket
                key = keyName
            }
            val presignedDeleteObject = client.presignDeleteObject(unsignedDeleteRequest, 60.seconds)

            val deleteCall = httpClient.call(presignedDeleteObject)
            deleteCall.complete()
            assertEquals(204, deleteCall.response.status.value)
        }
    }

    @Test
    fun testPresignNormal() = runBlocking {
        S3Client {
            region = S3TestUtils.DEFAULT_REGION
        }.use { testPresign(it) }
    }

    @Test
    fun testPresignWithForcePathStyle() = runBlocking {
        S3Client {
            region = S3TestUtils.DEFAULT_REGION
            forcePathStyle = true
        }.use { testPresign(it) }
    }
}
