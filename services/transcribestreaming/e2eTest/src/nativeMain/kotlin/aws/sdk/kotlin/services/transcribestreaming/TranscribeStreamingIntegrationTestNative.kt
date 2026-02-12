/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.transcribestreaming.model.AudioEvent
import aws.sdk.kotlin.services.transcribestreaming.model.AudioStream
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val FRAMES_PER_CHUNK = 4096

actual fun loadAudioStream(): Flow<AudioStream> = flow {
    // Read the WAV file from test resources
    val audioData = PlatformProvider.System.readFileOrNull("e2eTest/test-resources/hello-kotlin-8000.wav")
        ?: error("failed to load test resource: hello-kotlin-8000.wav")

    // Parse WAV header to find data chunk
    // WAV format: RIFF header (12 bytes) + format chunk + data chunk
    var offset = 12 // Skip "RIFF" + size + "WAVE"
    var dataOffset = -1
    var dataSize = 0

    while (offset < audioData.size - 8) {
        val chunkId = audioData.sliceArray(offset until offset + 4).decodeToString()
        val chunkSize = audioData[offset + 4].toInt() and 0xFF or
            ((audioData[offset + 5].toInt() and 0xFF) shl 8) or
            ((audioData[offset + 6].toInt() and 0xFF) shl 16) or
            ((audioData[offset + 7].toInt() and 0xFF) shl 24)

        if (chunkId == "data") {
            dataOffset = offset + 8
            dataSize = chunkSize
            break
        }
        offset += 8 + chunkSize
    }

    if (dataOffset == -1) {
        error("Could not find data chunk in WAV file")
    }

    val pcmData = audioData.sliceArray(dataOffset until dataOffset + dataSize)
    println("audio stream loaded: ${pcmData.size} bytes of PCM data, dataOffset=$dataOffset, dataSize=$dataSize")

    // Use same chunk size as JVM (FRAMES_PER_CHUNK * bytesPerFrame)
    // For 8kHz 16-bit mono: bytesPerFrame = 2
    val bytesPerFrame = 2
    val chunkSize = FRAMES_PER_CHUNK * bytesPerFrame
    println("Will emit chunks of size $chunkSize bytes")

    var pos = 0
    var eventCount = 0
    while (pos < pcmData.size) {
        val size = minOf(chunkSize, pcmData.size - pos)
        val chunk = pcmData.sliceArray(pos until pos + size)

        val event = AudioStream.AudioEvent(
            AudioEvent {
                audioChunk = chunk
            },
        )

        eventCount++
        println("emitting event #$eventCount (${chunk.size} bytes)")
        emit(event)
        pos += size
    }

    println("Total events emitted: $eventCount")
}
