/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.transcribestreaming.model.AudioEvent
import aws.sdk.kotlin.services.transcribestreaming.model.AudioStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.nio.file.Paths
import javax.sound.sampled.AudioSystem

private const val FRAMES_PER_CHUNK = 4096

actual fun loadAudioStream(): Flow<AudioStream> {
    val url = object {}.javaClass.classLoader.getResource("hello-kotlin-8000.wav") ?: error("failed to load test resource")
    val audioFile = Paths.get(url.toURI()).toFile()

    val format = AudioSystem.getAudioFileFormat(audioFile)
    val ais = AudioSystem.getAudioInputStream(audioFile)
    val bytesPerFrame = ais.format.frameSize
    println("audio stream format of $audioFile: $format; bytesPerFrame=$bytesPerFrame")

    return flow {
        while (true) {
            val frameBuffer = ByteArray(FRAMES_PER_CHUNK * bytesPerFrame)
            val rc = ais.read(frameBuffer)
            if (rc <= 0) {
                break
            }

            val chunk = if (rc < frameBuffer.size) frameBuffer.sliceArray(0 until rc) else frameBuffer
            val event = AudioStream.AudioEvent(
                AudioEvent {
                    audioChunk = chunk
                },
            )

            println("emitting event")
            emit(event)
        }
    }.flowOn(Dispatchers.IO)
}
