/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.io.internal.SdkDispatchers
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.cinterop.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import platform.windows.*

private const val IDLE_INTERVAL_MILLIS = 10L
private const val READ_BUFFER_SIZE = 4096
private const val END_OF_STREAM = -1

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun executeCommand(
    command: String,
    platformProvider: PlatformProvider,
    maxOutputLengthBytes: Long,
    timeoutMillis: Long,
    clock: Clock,
): Pair<Int, String> = withContext(SdkDispatchers.IO) {
    val (stdoutRead, stdoutWrite) = createPipe()
    val (stderrRead, stderrWrite) = try {
        createPipe()
    } catch (ex: Throwable) {
        CloseHandle(stdoutRead)
        CloseHandle(stdoutWrite)
        throw ex
    }

    var process: HANDLE? = null
    var exited = false
    try {
        process = try {
            startShell(command, stdoutWrite, stderrWrite)
        } finally {
            CloseHandle(stdoutWrite)
            CloseHandle(stderrWrite)
        }

        val stdout = ProcessOutputBuffer(maxOutputLengthBytes, failOnOverflow = true)
        val stderr = ProcessOutputBuffer(maxOutputLengthBytes, failOnOverflow = false)

        withTimeout(timeoutMillis) {
            drainPipes(arrayOf(stdoutRead, stderrRead), arrayOf(stdout, stderr))
            while (WaitForSingleObject(process, 0u) == WAIT_TIMEOUT.convert<DWORD>()) {
                delay(IDLE_INTERVAL_MILLIS)
            }
            exited = true
        }

        val exitCode = memScoped {
            val code = alloc<DWORDVar>()
            if (GetExitCodeProcess(process, code.ptr) == 0) {
                throw CredentialsProviderException("Failed to get process exit code: error ${GetLastError()}")
            }
            code.value.toInt()
        }

        processResult(exitCode, stdout, stderr)
    } finally {
        CloseHandle(stdoutRead)
        CloseHandle(stderrRead)
        if (process != null) {
            if (!exited) {
                TerminateProcess(process, 1u)
                WaitForSingleObject(process, INFINITE)
            }
            CloseHandle(process)
        }
    }
}

/**
 * Create an anonymous pipe whose write end is inheritable by a child process and whose read end is not.
 */
@OptIn(ExperimentalForeignApi::class)
private fun createPipe(): Pair<HANDLE?, HANDLE?> = memScoped {
    val attributes = alloc<SECURITY_ATTRIBUTES>().apply {
        nLength = sizeOf<SECURITY_ATTRIBUTES>().convert()
        bInheritHandle = TRUE
        lpSecurityDescriptor = null
    }
    val read = alloc<HANDLEVar>()
    val write = alloc<HANDLEVar>()
    if (CreatePipe(read.ptr, write.ptr, attributes.ptr, 0u) == 0) {
        throw CredentialsProviderException("Failed to create pipe: error ${GetLastError()}")
    }
    if (SetHandleInformation(read.value, HANDLE_FLAG_INHERIT.convert(), 0u) == 0) {
        val error = GetLastError()
        CloseHandle(read.value)
        CloseHandle(write.value)
        throw CredentialsProviderException("Failed to configure pipe: error $error")
    }
    read.value to write.value
}

/**
 * Start `cmd.exe /S /C "<command>"` with stdout and stderr redirected and return the process handle.
 */
@OptIn(ExperimentalForeignApi::class)
private fun startShell(command: String, stdoutWrite: HANDLE?, stderrWrite: HANDLE?): HANDLE? = memScoped {
    // Resolve cmd.exe from the system directory rather than letting CreateProcessW search the working directory
    val systemDir = allocArray<WCHARVar>(MAX_PATH)
    val length = GetSystemDirectoryW(systemDir, MAX_PATH.convert()).toInt()
    if (length == 0 || length >= MAX_PATH) {
        throw CredentialsProviderException("Failed to locate the system directory: error ${GetLastError()}")
    }
    val shell = systemDir.toKString() + "\\cmd.exe"

    // /S strips exactly the outer quotes, so the command runs as written regardless of its own quoting
    val commandLine = "\"$shell\" /S /C \"$command\""

    val startupInfo = alloc<STARTUPINFOW>().apply {
        cb = sizeOf<STARTUPINFOW>().convert()
        dwFlags = STARTF_USESTDHANDLES.convert()
        hStdInput = GetStdHandle(STD_INPUT_HANDLE)
        hStdOutput = stdoutWrite
        hStdError = stderrWrite
    }
    val processInfo = alloc<PROCESS_INFORMATION>()

    val created = CreateProcessW(
        shell,
        commandLine.wcstr.ptr,
        null,
        null,
        TRUE,
        CREATE_NO_WINDOW.convert(),
        null,
        null,
        startupInfo.ptr,
        processInfo.ptr,
    )
    if (created == 0) {
        throw CredentialsProviderException("Failed to start process: error ${GetLastError()}")
    }
    CloseHandle(processInfo.hThread)
    processInfo.hProcess
}

/**
 * Read every pipe until it reaches end-of-stream. Anonymous pipes don't support overlapped I/O, so poll with
 * PeekNamedPipe to avoid a blocking ReadFile that would ignore cancellation and the timeout.
 */
@OptIn(ExperimentalForeignApi::class)
private suspend fun drainPipes(pipes: Array<HANDLE?>, sinks: Array<ProcessOutputBuffer>) {
    val open = BooleanArray(pipes.size) { true }
    val buffer = ByteArray(READ_BUFFER_SIZE)

    while (open.any { it }) {
        currentCoroutineContext().ensureActive()

        var readAny = false
        for (i in pipes.indices) {
            if (!open[i]) continue
            when (val count = readAvailable(pipes[i], buffer)) {
                END_OF_STREAM -> open[i] = false
                0 -> Unit
                else -> {
                    sinks[i].append(buffer, count)
                    readAny = true
                }
            }
        }
        if (!readAny) delay(IDLE_INTERVAL_MILLIS)
    }
}

/**
 * Read whatever is immediately available from [pipe] without blocking.
 * @return the number of bytes read, or [END_OF_STREAM] once the writer has closed the pipe
 */
@OptIn(ExperimentalForeignApi::class)
private fun readAvailable(pipe: HANDLE?, buffer: ByteArray): Int = memScoped {
    val available = alloc<DWORDVar>()
    if (PeekNamedPipe(pipe, null, 0u, null, available.ptr, null) == 0) {
        return endOfStreamOrThrow()
    }
    if (available.value == 0u) return 0

    val bytesRead = alloc<DWORDVar>()
    val toRead = minOf(available.value, buffer.size.toUInt())
    val succeeded = buffer.usePinned { ReadFile(pipe, it.addressOf(0), toRead, bytesRead.ptr, null) }
    if (succeeded == 0) {
        return endOfStreamOrThrow()
    }
    bytesRead.value.toInt()
}

@OptIn(ExperimentalForeignApi::class)
private fun endOfStreamOrThrow(): Int {
    val error = GetLastError()
    if (error == ERROR_BROKEN_PIPE.convert<DWORD>()) return END_OF_STREAM
    throw CredentialsProviderException("Failed to read process output: error $error")
}
