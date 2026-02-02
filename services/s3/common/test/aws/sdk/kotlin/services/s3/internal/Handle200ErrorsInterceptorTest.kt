/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.deleteObjects
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.io.use
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private const val REQUEST_ID = "K2H6N7ZGQT6WHCEG"
private const val EXT_REQUEST_ID = "WWoZlnK4pTjKCYn6eNV7GgOurabfqLkjbSyqTvDMGBaI9uwzyNhSaDhOCPs8paFGye7S6b/AB3A="

class Handle200ErrorsInterceptorTest {

    object TestCredentialsProvider : CredentialsProvider {
        override suspend fun resolve(attributes: Attributes): Credentials = Credentials("AKID", "SECRET")
    }
    private val errorResponsePayload = """
            <Error>
                <Code>SlowDown</Code>
                <Message>Please reduce your request rate.</Message>
                <RequestId>$REQUEST_ID</RequestId>
                <HostId>$EXT_REQUEST_ID</HostId>
            </Error>
    """.trimIndent().encodeToByteArray()

    private fun newTestClient(
        status: HttpStatusCode = HttpStatusCode.OK,
        payload: ByteArray = errorResponsePayload,
    ): S3Client = S3Client {
        region = "us-east-1"
        credentialsProvider = TestCredentialsProvider
        retryStrategy {
            maxAttempts = 1
        }
        httpClient = buildTestConnection {
            expect(HttpResponse(status, body = HttpBody.fromBytes(payload)))
        }
    }

    fun assertException(ex: S3Exception) {
        val expectedMessage = "Please reduce your request rate."
        assertEquals("SlowDown", ex.sdkErrorMetadata.errorCode)
        assertEquals(expectedMessage, ex.sdkErrorMetadata.errorMessage)
        assertEquals("$expectedMessage, Request ID: $REQUEST_ID, Extended request ID: $EXT_REQUEST_ID", ex.message)
        assertEquals(REQUEST_ID, ex.sdkErrorMetadata.requestId)
        assertEquals(EXT_REQUEST_ID, ex.sdkErrorMetadata.requestId2)
    }

    @Test
    fun testHandle200ErrorsWithNoExpectedBody() = runTest {
        newTestClient().use { s3 ->
            val ex = assertFailsWith<S3Exception> {
                s3.deleteObject {
                    bucket = "test"
                    key = "key"
                }
            }
            assertException(ex)
        }
    }

    @Test
    fun testHandle200ErrorsWithExpectedBody() = runTest {
        newTestClient().use { s3 ->
            val ex = assertFailsWith<S3Exception> {
                s3.deleteObjects { bucket = "test" }
            }
            assertException(ex)
        }
    }

    @Test
    fun testNonErrorPayload() = runTest {
        val payload = """
           <?xml version="1.0" encoding="UTF-8"?>
           <DeleteResult>
              <Deleted>
                 <Key>my-key</Key>
              </Deleted>
           </DeleteResult>
        """.trimIndent().encodeToByteArray()
        val s3 = newTestClient(payload = payload)
        val response = s3.deleteObjects { bucket = "test" }
        assertEquals("my-key", response.deleted?.first()?.key)
    }

    @Test
    fun testErrorPayloadUnmodified() = runTest {
        val payload = """
           <?xml version="1.0" encoding="UTF-8"?>
            <Error>
                <Code>FooError</Code>
                <Message>Please use less foos.</Message>
                <RequestId>rid</RequestId>
                <HostId>rid2</HostId>
            </Error>
        """.trimIndent().encodeToByteArray()
        newTestClient(HttpStatusCode.BadRequest, payload).use { s3 ->
            val ex = assertFailsWith<S3Exception> {
                s3.deleteObjects { bucket = "test" }
            }
            val expectedMessage = "Please use less foos."
            assertEquals("$expectedMessage, Request ID: rid, Extended request ID: rid2", ex.message)
            assertEquals(expectedMessage, ex.sdkErrorMetadata.errorMessage)
            assertEquals("FooError", ex.sdkErrorMetadata.errorCode)
            assertEquals("rid", ex.sdkErrorMetadata.requestId)
            assertEquals("rid2", ex.sdkErrorMetadata.requestId2)
        }
    }
}
