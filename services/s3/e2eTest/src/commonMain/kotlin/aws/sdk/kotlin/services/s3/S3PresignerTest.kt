/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

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
import aws.smithy.kotlin.runtime.testing.AfterAll
import aws.smithy.kotlin.runtime.testing.BeforeAll
import aws.smithy.kotlin.runtime.testing.TestInstance
import aws.smithy.kotlin.runtime.testing.TestLifecycle
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestLifecycle.PER_CLASS)
class S3PresignerTest {
    private val client = S3TestUtils.createClient()

    private lateinit var testBucket: String

    @BeforeAll
    fun createResources(): Unit = runBlocking {
        testBucket = S3TestUtils.createTestBucket(client, "presigning")
    }

    @AfterAll
    fun cleanup(): Unit = runBlocking {
        S3TestUtils.deleteBucket(client, testBucket)
        client.close()
    }

    private suspend fun testPresign(client: S3Client) {
        val contents = "presign-test"
        val keyName = "foo$PRINTABLE_CHARS"

        withAllEngines { context ->
            val httpClient = SdkHttpClient(context.engine)

            // PUT
            val unsignedPutRequest = PutObjectRequest {
                bucket = testBucket
                key = keyName
            }
            val presignedPutRequest = client.presignPutObject(unsignedPutRequest, 60.seconds)

            S3TestUtils.responseCodeFromPut(presignedPutRequest, contents)

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
        S3TestUtils.createClient().use { testPresign(it) }
    }

    @Test
    fun testPresignWithForcePathStyle() = runBlocking {
        S3TestUtils.createClient { forcePathStyle = true }.use { testPresign(it) }
    }
}
