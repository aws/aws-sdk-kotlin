/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.cinterop.*
import platform.posix.*

internal actual suspend fun executeCommand(
    command: String,
    platformProvider: PlatformProvider,
    maxOutputLengthBytes: Long,
    timeoutMillis: Long,
    clock: Clock,
): Pair<Int, String> = executeShellCommandPosix(command, maxOutputLengthBytes, timeoutMillis, ::forkExecShell)

// Kotlin/Native's Apple platform libraries don't expose posix_spawn, so fall back to fork + exec. Everything the child
// needs is allocated before forking so the child only calls dup2, execv and _exit.
@OptIn(ExperimentalForeignApi::class)
private fun forkExecShell(argv: List<String>, stdoutFd: Int, stderrFd: Int): pid_t = memScoped {
    val path = argv[0].cstr.ptr
    val cArgv = allocArrayOf(argv.map { it.cstr.ptr } + null)

    val pid = fork()
    if (pid == 0) {
        if (dup2(stdoutFd, STDOUT_FILENO) == -1 || dup2(stderrFd, STDERR_FILENO) == -1) _exit(127)
        execv(path, cArgv)
        _exit(127)
    }
    if (pid == -1) {
        throw CredentialsProviderException("Failed to start process: ${errnoMessage()}")
    }
    pid
}
