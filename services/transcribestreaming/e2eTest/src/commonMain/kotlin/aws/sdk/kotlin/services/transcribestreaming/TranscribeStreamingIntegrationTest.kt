/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.transcribestreaming.TranscribeStreamingClient
import aws.sdk.kotlin.services.transcribestreaming.model.*
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.testing.IgnoreNative
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class TranscribeStreamingIntegrationTest {
    @IgnoreNative // FIXME Implement native bidirectional streaming in CRT
    @Test
    fun testTranscribeEventStream() = runTest {
        TranscribeStreamingClient { region = "us-west-2" }.use { client ->
            val transcript = getTranscript(client)
            assertTrue(transcript.startsWith("Hello from", true), "full transcript: $transcript")
        }
    }
}

private suspend fun getTranscript(client: TranscribeStreamingClient): String {
    val req = StartStreamTranscriptionRequest {
        languageCode = LanguageCode.EnUs
        mediaSampleRateHertz = 8000
        mediaEncoding = MediaEncoding.Pcm
        audioStream = loadAudioStream()
    }

    val transcript = client.startStreamTranscription(req) { resp ->
        val fullMessage = StringBuilder()
        resp.transcriptResultStream?.collect { event ->
            when (event) {
                is TranscriptResultStream.TranscriptEvent -> {
                    event.value.transcript?.results?.forEach { result ->
                        val transcript = result.alternatives?.firstOrNull()?.transcript
                        println("received TranscriptEvent: isPartial=${result.isPartial}; transcript=$transcript")
                        if (!result.isPartial) {
                            transcript?.let { fullMessage.append(it) }
                        }
                    }
                }
                else -> error("unknown event $event")
            }
        }
        fullMessage.toString()
    }

    return transcript
}

expect fun loadAudioStream(): Flow<AudioStream>
