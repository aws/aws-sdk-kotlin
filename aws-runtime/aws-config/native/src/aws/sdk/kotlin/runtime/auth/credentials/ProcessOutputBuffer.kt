/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.io.SdkBuffer

/**
 * Accumulates raw bytes from a child process stream, up to [limit] bytes.
 * @param failOnOverflow whether exceeding [limit] throws. When false, bytes past the limit are discarded.
 */
internal class ProcessOutputBuffer(
    private val limit: Long,
    private val failOnOverflow: Boolean,
) {
    private val buffer = SdkBuffer()

    fun append(bytes: ByteArray, count: Int) {
        val remaining = limit - buffer.size
        if (count > remaining) {
            if (failOnOverflow) {
                throw CredentialsProviderException("Process output exceeded limit of $limit bytes")
            }
            if (remaining > 0) buffer.write(bytes, 0, remaining.toInt())
            return
        }
        buffer.write(bytes, 0, count)
    }

    fun decode(): String = buffer.readUtf8()
}

/**
 * Select the output to return for [exitCode], matching the JVM behavior: stdout on success, stderr otherwise.
 */
internal fun processResult(
    exitCode: Int,
    stdout: ProcessOutputBuffer,
    stderr: ProcessOutputBuffer,
): Pair<Int, String> = exitCode to if (exitCode == 0) stdout.decode() else stderr.decode()
