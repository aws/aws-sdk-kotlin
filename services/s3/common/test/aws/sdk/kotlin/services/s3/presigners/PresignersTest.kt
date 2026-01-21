/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.presigners

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.http.HttpCall
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineBase
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineConfig
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class PresignersTest {
    @OptIn(InternalApi::class)
    @Test
    fun testPresignHeaders() = runTest {
        val req = PutObjectRequest {
            bucket = "foo"
            key = "bar"
            acl = ObjectCannedAcl.BucketOwnerFullControl
        }

        val presigned = S3Client {
            region = "us-west-2"
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = "AKID"
                secretAccessKey = "secret"
            }
            httpClient = NoHttpEngine
        }.use { s3 ->
            s3.presignPutObject(req, 1.days)
        }

        val aclHeader = presigned.headers["x-amz-acl"]
        assertNotNull(aclHeader, "Expected x-amz-acl header to be included in presigned request")
        assertEquals("bucket-owner-full-control", aclHeader)

        val signedHeadersString = presigned.url.parameters.decodedParameters["X-Amz-SignedHeaders"]?.single()
        assertNotNull(signedHeadersString, "Expected X-Amz-SignedHeaders query parameter in URL")

        val signedHeaders = signedHeadersString.split(';')
        assertTrue(signedHeaders.contains("x-amz-acl"), "Expected x-amz-acl to be signed but only found $signedHeaders")
    }
}

object NoHttpEngine : HttpClientEngineBase("no-http") {
    override val config: HttpClientEngineConfig = HttpClientEngineConfig.Default

    override suspend fun roundTrip(context: ExecutionContext, request: HttpRequest): HttpCall = error("Should not need HTTP round trip")
}
