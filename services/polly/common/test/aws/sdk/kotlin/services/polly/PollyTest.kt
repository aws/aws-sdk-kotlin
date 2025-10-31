/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.polly

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.polly.model.OutputFormat
import aws.sdk.kotlin.services.polly.model.SynthesizeSpeechRequest
import aws.sdk.kotlin.services.polly.model.VoiceId
import aws.sdk.kotlin.services.polly.presigners.presignSynthesizeSpeech
import aws.smithy.kotlin.runtime.http.HttpCall
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineBase
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineConfig
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class PollyPresignerTest {
    @Test
    fun itProducesExpectedUrlComponents() = runTest {
        val request = SynthesizeSpeechRequest {
            voiceId = VoiceId.Salli
            outputFormat = OutputFormat.Pcm
            text = "hello world"
        }

        val pollyClient = PollyClient {
            region = "us-east-2"
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = "AKID"
                secretAccessKey = "secret"
            }
            httpClient = NoHttpEngine
        }

        try {
            val presignedRequest = pollyClient.presignSynthesizeSpeech(request, 10.seconds)

            assertEquals(HttpMethod.GET, presignedRequest.method)
            assertTrue("Host".equals(presignedRequest.headers.entries().single().key, ignoreCase = true))
            assertEquals("polly.us-east-2.amazonaws.com", presignedRequest.headers["Host"])
            assertEquals("/v1/speech", presignedRequest.url.path.toString())
            val expectedQueryParameters = setOf("OutputFormat", "Text", "VoiceId", "X-Amz-Algorithm", "X-Amz-Credential", "X-Amz-Date", "X-Amz-SignedHeaders", "X-Amz-Expires", "X-Amz-Signature")
            assertEquals(expectedQueryParameters, presignedRequest.url.parameters.encodedParameters.keys)
        } finally {
            pollyClient.close()
        }
    }
}

object NoHttpEngine : HttpClientEngineBase("no-http") {
    override val config: HttpClientEngineConfig = HttpClientEngineConfig.Default

    override suspend fun roundTrip(context: ExecutionContext, request: HttpRequest): HttpCall =
        error("Should not need HTTP round trip")
}
