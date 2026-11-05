/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.cinterop.*
import platform.linux.*
import platform.posix.STDERR_FILENO
import platform.posix.STDOUT_FILENO
import platform.posix.__environ
import platform.posix.pid_t
import platform.posix.pid_tVar

internal actual suspend fun executeCommand(
    command: String,
    platformProvider: PlatformProvider,
    maxOutputLengthBytes: Long,
    timeoutMillis: Long,
    clock: Clock,
): Pair<Int, String> = executeShellCommandPosix(command, maxOutputLengthBytes, timeoutMillis, ::posixSpawnShell)

@OptIn(ExperimentalForeignApi::class)
private fun posixSpawnShell(argv: List<String>, stdoutFd: Int, stderrFd: Int): pid_t = memScoped {
    val actions = alloc<posix_spawn_file_actions_t>()
    check(posix_spawn_file_actions_init(actions.ptr) == 0) { "Failed to initialize posix_spawn file actions" }
    try {
        check(posix_spawn_file_actions_adddup2(actions.ptr, stdoutFd, STDOUT_FILENO) == 0) { "Failed to redirect stdout" }
        check(posix_spawn_file_actions_adddup2(actions.ptr, stderrFd, STDERR_FILENO) == 0) { "Failed to redirect stderr" }

        val cArgv = allocArrayOf(argv.map { it.cstr.ptr } + null)
        val pid = alloc<pid_tVar>()
        val rc = posix_spawnp(pid.ptr, argv[0], actions.ptr, null, cArgv, __environ)
        if (rc != 0) {
            throw CredentialsProviderException("Failed to start process: ${errnoMessage(rc)}")
        }
        pid.value
    } finally {
        posix_spawn_file_actions_destroy(actions.ptr)
    }
}
