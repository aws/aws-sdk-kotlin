/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.io.internal.SdkDispatchers
import kotlinx.cinterop.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import platform.posix.*

internal const val POSIX_SHELL = "/bin/sh"

// Upper bound on a single blocking poll() so cancellation (including the timeout) is observed promptly
private const val POLL_INTERVAL_MILLIS = 50
private const val EXIT_POLL_INTERVAL_MILLIS = 10L
private const val READ_BUFFER_SIZE = 4096

private val spawnLock = Mutex()

/**
 * Starts `/bin/sh -c <command>` with its stdout and stderr redirected to the given file descriptors and returns
 * the child's pid. Implementations must throw if the process cannot be started.
 */
internal typealias PosixShellSpawner = (argv: List<String>, stdoutFd: Int, stderrFd: Int) -> pid_t

/**
 * Run [command] through the POSIX shell, enforcing [timeoutMillis] over the whole run and [maxOutputLengthBytes] over
 * stdout. The child is always killed and reaped if this function throws.
 */
@OptIn(ExperimentalForeignApi::class)
internal suspend fun executeShellCommandPosix(
    command: String,
    maxOutputLengthBytes: Long,
    timeoutMillis: Long,
    spawn: PosixShellSpawner,
): Pair<Int, String> = withContext(SdkDispatchers.IO) {
    var stdoutRead = -1
    var stderrRead = -1
    var pid: pid_t = -1
    var reaped = false
    try {
        // FD_CLOEXEC is set after pipe() rather than atomically, so a spawn running concurrently could inherit these
        // descriptors and hold our pipes open until it exits. Serialize pipe creation through spawn to prevent that.
        spawnLock.withLock {
            val (stdoutPipeRead, stdoutWrite) = createPipe()
            stdoutRead = stdoutPipeRead
            val (stderrPipeRead, stderrWrite) = try {
                createPipe()
            } catch (ex: Throwable) {
                close(stdoutWrite)
                throw ex
            }
            stderrRead = stderrPipeRead
            try {
                pid = spawn(listOf(POSIX_SHELL, "-c", command), stdoutWrite, stderrWrite)
            } finally {
                close(stdoutWrite)
                close(stderrWrite)
            }
        }

        val stdout = ProcessOutputBuffer(maxOutputLengthBytes, failOnOverflow = true)
        val stderr = ProcessOutputBuffer(maxOutputLengthBytes, failOnOverflow = false)

        val status = withTimeout(timeoutMillis) {
            drainPipes(stdoutRead, stdout, stderrRead, stderr)
            awaitExit(pid).also { reaped = true }
        }

        processResult(exitCodeOf(status), stdout, stderr)
    } finally {
        if (stdoutRead >= 0) close(stdoutRead)
        if (stderrRead >= 0) close(stderrRead)
        if (pid > 0 && !reaped) {
            kill(pid, SIGKILL)
            waitpidRetrying(pid, 0)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun createPipe(): IntArray {
    val fds = IntArray(2)
    if (pipe(fds.refTo(0)) != 0) {
        throw CredentialsProviderException("Failed to create pipe: ${errnoMessage()}")
    }
    // Keep these descriptors out of any process spawned concurrently; dup2 onto stdout/stderr clears the flag in our child
    fds.forEach { fcntl(it, F_SETFD, FD_CLOEXEC) }
    return fds
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun drainPipes(
    stdoutFd: Int,
    stdout: ProcessOutputBuffer,
    stderrFd: Int,
    stderr: ProcessOutputBuffer,
) = memScoped {
    val fds = allocArray<pollfd>(2)
    fds[0].fd = stdoutFd
    fds[1].fd = stderrFd
    val sinks = arrayOf(stdout, stderr)
    for (i in 0..1) fds[i].events = POLLIN.toShort()

    val buffer = ByteArray(READ_BUFFER_SIZE)
    while (fds[0].fd >= 0 || fds[1].fd >= 0) {
        currentCoroutineContext().ensureActive()

        val ready = poll(fds, 2.convert(), POLL_INTERVAL_MILLIS)
        if (ready < 0) {
            if (errno == EINTR) continue
            throw CredentialsProviderException("Failed to poll process output: ${errnoMessage()}")
        }
        if (ready == 0) continue

        for (i in 0..1) {
            val entry = fds[i]
            if (entry.fd < 0 || entry.revents.toInt() == 0) continue

            val rc = read(entry.fd, buffer.refTo(0), buffer.size.convert()).toInt()
            when {
                rc > 0 -> sinks[i].append(buffer, rc)
                rc == 0 -> entry.fd = -1 // EOF; negative descriptors are ignored by poll()
                errno == EINTR || errno == EAGAIN -> Unit
                else -> throw CredentialsProviderException("Failed to read process output: ${errnoMessage()}")
            }
        }
    }
}

/**
 * Wait for [pid] to exit without blocking the thread, so the surrounding timeout still applies to a child that closed
 * its output streams but kept running.
 */
private suspend fun awaitExit(pid: pid_t): Int {
    while (true) {
        val (rc, status) = waitpidRetrying(pid, WNOHANG)
        when {
            rc == pid -> return status
            rc == 0 -> delay(EXIT_POLL_INTERVAL_MILLIS)
            else -> throw CredentialsProviderException("Failed to wait for process: ${errnoMessage()}")
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun waitpidRetrying(pid: pid_t, options: Int): Pair<pid_t, Int> = memScoped {
    val status = alloc<IntVar>()
    var rc: pid_t
    do {
        rc = waitpid(pid, status.ptr, options)
    } while (rc == -1 && errno == EINTR)
    rc to status.value
}

/**
 * Decode a wait status the way WIFEXITED/WEXITSTATUS/WTERMSIG would (they're C macros, so unavailable here). A child
 * killed by a signal reports `128 + signal`, the same non-zero code the JVM's `Process.exitValue()` gives. Without
 * WUNTRACED, waitpid only reports exited or signaled children.
 */
private fun exitCodeOf(status: Int): Int {
    val termSignal = status and 0x7f
    return if (termSignal == 0) (status shr 8) and 0xff else 128 + termSignal
}

@OptIn(ExperimentalForeignApi::class)
internal fun errnoMessage(error: Int = errno): String = strerror(error)?.toKString() ?: "errno $error"
