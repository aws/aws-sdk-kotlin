/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.polly

import aws.sdk.kotlin.services.polly.model.OutputFormat
import aws.sdk.kotlin.services.polly.model.SynthesizeSpeechRequest
import aws.sdk.kotlin.services.polly.model.VoiceId
import aws.sdk.kotlin.services.polly.presigners.presignSynthesizeSpeech
import aws.sdk.kotlin.testing.withAllEngines
import aws.smithy.kotlin.runtime.http.SdkHttpClient
import aws.smithy.kotlin.runtime.http.complete
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for presigner
 */
class PollyPresignerTest {
    @Test
    fun clientBasedPresign() = runTest {
        val unsignedRequest = SynthesizeSpeechRequest {
            voiceId = VoiceId.Salli
            outputFormat = OutputFormat.Pcm
            text = "hello world"
        }

        val client = PollyClient { region = "us-east-1" }
        val presignedRequest = client.presignSynthesizeSpeech(unsignedRequest, 10.seconds)

        withAllEngines { engine ->
            val httpClient = SdkHttpClient(engine)

            val call = httpClient.call(presignedRequest)
            call.complete()

            assertEquals(200, call.response.status.value, "presigned polly request failed for engine: $engine")
        }
    }
}
