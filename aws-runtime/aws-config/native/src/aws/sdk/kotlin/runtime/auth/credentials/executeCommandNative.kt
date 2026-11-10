/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.io.internal.SdkDispatchers
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.cinterop.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun executeCommand(
    command: String,
    platformProvider: PlatformProvider,
    maxOutputLengthBytes: Long,
    timeoutMillis: Long,
    clock: Clock,
): Pair<Int, String> = withContext(SdkDispatchers.IO) {
    val pipeFds = IntArray(2)
    if (pipe(pipeFds.refTo(0)) != 0) {
        error("Failed to create pipe")
    }
    val (readFd, writeFd) = pipeFds

    val pid = fork()
    if (pid == -1) {
        close(readFd)
        close(writeFd)
        error("Failed to fork")
    }

    if (pid == 0) {
        // Child process
        close(readFd) // Close read end

        // Pass stdout and stderr back to parent
        try {
            dup2(writeFd, STDOUT_FILENO)
            dup2(writeFd, STDERR_FILENO)
        } finally {
            close(writeFd)
        }

        val shell = when (platformProvider.osInfo().family) {
            OsFamily.Windows -> "C:\\Windows\\System32\\cmd.exe"
            else -> "/bin/sh"
        }

        val shellArg = when (platformProvider.osInfo().family) {
            OsFamily.Windows -> "/C"
            else -> "-c"
        }

        memScoped {
            val argv = (arrayOf(shell, shellArg, command).map { it.cstr.ptr } + null).toCValues()
            execvp(shell, argv)
        }
        _exit(127) // If exec fails
    }

    // Parent process
    close(writeFd) // Close write end

    val output = try {
        buildString {
            val buffer = ByteArray(1024)
            var totalBytesRead = 0L

            withTimeout(timeoutMillis) {
                while (true) {
                    val nBytes = minOf(maxOutputLengthBytes - totalBytesRead, buffer.size.toLong())
                    if (nBytes == 0L) {
                        throw CredentialsProviderException("Process output exceeded limit of $maxOutputLengthBytes bytes")
                    }

                    val rc = read(readFd, buffer.refTo(0), nBytes.toULong()).toInt()
                    if (rc <= 0) break
                    totalBytesRead += rc
                    append(buffer.decodeToString(0, rc))
                }
            }
        }
    } finally {
        close(readFd)
    }

    memScoped {
        val status = alloc<IntVar>()
        waitpid(pid, status.ptr, 0)
        val exitCode = when (platformProvider.osInfo().family) {
            OsFamily.Windows -> status.value
            else -> (status.value shr 8) and 0xFF
        }

        exitCode to output
    }
}
