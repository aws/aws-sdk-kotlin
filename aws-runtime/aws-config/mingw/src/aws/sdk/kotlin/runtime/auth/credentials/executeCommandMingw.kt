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
            val stdin = openNullDevice()
            try {
                startShell(command, stdin, stdoutWrite, stderrWrite)
            } finally {
                CloseHandle(stdin)
            }
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
 * Open the null device as an inheritable handle to use as the child's stdin.
 */
@OptIn(ExperimentalForeignApi::class)
private fun openNullDevice(): HANDLE? = memScoped {
    val attributes = alloc<SECURITY_ATTRIBUTES>().apply {
        nLength = sizeOf<SECURITY_ATTRIBUTES>().convert()
        bInheritHandle = TRUE
        lpSecurityDescriptor = null
    }
    val handle = CreateFileW(
        "NUL",
        GENERIC_READ,
        (FILE_SHARE_READ or FILE_SHARE_WRITE).convert(),
        attributes.ptr,
        OPEN_EXISTING.convert(),
        0u,
        null,
    )
    if (handle == INVALID_HANDLE_VALUE) {
        throw CredentialsProviderException("Failed to open the null device: error ${GetLastError()}")
    }
    handle
}

/**
 * Start `cmd.exe /S /C "<command>"` with its standard streams redirected and return the process handle. Only the
 * given handles are inherited, so a process started concurrently by another call can't receive this call's pipe
 * handles and hold them open (see the remarks on handle inheritance in the `CreateProcessW` documentation).
 */
@OptIn(ExperimentalForeignApi::class)
private fun startShell(command: String, stdin: HANDLE?, stdoutWrite: HANDLE?, stderrWrite: HANDLE?): HANDLE? = memScoped {
    // Resolve cmd.exe from the system directory rather than letting CreateProcessW search the working directory
    val systemDir = allocArray<WCHARVar>(MAX_PATH)
    val length = GetSystemDirectoryW(systemDir, MAX_PATH.convert()).toInt()
    if (length == 0 || length >= MAX_PATH) {
        throw CredentialsProviderException("Failed to locate the system directory: error ${GetLastError()}")
    }
    val shell = systemDir.toKString() + "\\cmd.exe"

    // /S strips exactly the outer quotes, so the command runs as written regardless of its own quoting
    val commandLine = "\"$shell\" /S /C \"$command\""

    val inherited = allocArrayOf(stdin, stdoutWrite, stderrWrite)
    val attributeListSize = alloc<SIZE_TVar>()
    InitializeProcThreadAttributeList(null, 1u, 0u, attributeListSize.ptr)
    val attributeList: LPPROC_THREAD_ATTRIBUTE_LIST =
        allocArray<ByteVar>(attributeListSize.value.toLong()).reinterpret()
    if (InitializeProcThreadAttributeList(attributeList, 1u, 0u, attributeListSize.ptr) == 0) {
        throw CredentialsProviderException("Failed to initialize process attributes: error ${GetLastError()}")
    }

    try {
        val updated = UpdateProcThreadAttribute(
            attributeList,
            0u,
            PROC_THREAD_ATTRIBUTE_HANDLE_LIST.convert(),
            inherited,
            (3 * sizeOf<HANDLEVar>()).convert(),
            null,
            null,
        )
        if (updated == 0) {
            throw CredentialsProviderException("Failed to set inherited handles: error ${GetLastError()}")
        }

        val startupInfo = alloc<STARTUPINFOEXW>().apply {
            StartupInfo.cb = sizeOf<STARTUPINFOEXW>().convert()
            StartupInfo.dwFlags = STARTF_USESTDHANDLES.convert()
            StartupInfo.hStdInput = stdin
            StartupInfo.hStdOutput = stdoutWrite
            StartupInfo.hStdError = stderrWrite
            lpAttributeList = attributeList
        }
        val processInfo = alloc<PROCESS_INFORMATION>()

        val created = CreateProcessW(
            shell,
            commandLine.wcstr.ptr,
            null,
            null,
            TRUE,
            (CREATE_NO_WINDOW or EXTENDED_STARTUPINFO_PRESENT).convert(),
            null,
            null,
            startupInfo.StartupInfo.ptr,
            processInfo.ptr,
        )
        if (created == 0) {
            throw CredentialsProviderException("Failed to start process: error ${GetLastError()}")
        }
        CloseHandle(processInfo.hThread)
        processInfo.hProcess
    } finally {
        DeleteProcThreadAttributeList(attributeList)
    }
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
