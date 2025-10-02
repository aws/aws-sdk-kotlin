/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.smithy.kotlin.runtime.client.util

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.io.internal.SdkDispatchers
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.cinterop.*
import kotlinx.coroutines.withContext
import platform.posix._wunlink // to delete the temp file afterwards
import platform.windows.*

@OptIn(ExperimentalForeignApi::class)
private fun String.wideCString(mem: MemScope) = wcstr.getPointer(mem)

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun executeCommand(
    command: String,
    platformProvider: PlatformProvider,
    maxOutputLengthBytes: Long,
    timeoutMillis: Long,
    clock: Clock,
): Pair<Int, String> = withContext(SdkDispatchers.IO) {
    memScoped {
        // 1) Make a temp file to capture stdout+stderr
        val tmpDirBuf = allocArray<UShortVar>(MAX_PATH)
        val gotTmp = GetTempPathW(MAX_PATH.toUInt(), tmpDirBuf)
        if (gotTmp == 0u) error("GetTempPathW failed")

        val tmpNameBuf = allocArray<UShortVar>(MAX_PATH)
        val tmpDirStr = tmpDirBuf.toKString()
        val gotName = GetTempFileNameW(tmpDirStr, "KNR", 0u, tmpNameBuf)
        if (gotName == 0u) error("GetTempFileNameW failed")
        val outPath: String = tmpNameBuf.toKString()

        // 2) Create the output file (child inherits this handle)
        val sa = alloc<SECURITY_ATTRIBUTES>().apply {
            nLength = sizeOf<SECURITY_ATTRIBUTES>().toUInt()
            bInheritHandle = TRUE
            lpSecurityDescriptor = null
        }

        val hOut: HANDLE = CreateFileW(
            /* lpFileName        = */ outPath,
            /* dwDesiredAccess   = */ GENERIC_WRITE.toUInt(),
            /* dwShareMode       = */ (FILE_SHARE_READ or FILE_SHARE_WRITE),
            /* lpSecurityAttributes = */ sa.ptr,
            /* dwCreationDisposition = */ CREATE_ALWAYS,
            /* dwFlagsAndAttributes  = */ FILE_ATTRIBUTE_NORMAL.toUInt(),
            /* hTemplateFile     = */ null,
        )
        if (hOut == INVALID_HANDLE_VALUE) error("CreateFileW failed for temp output (GetLastError=${GetLastError()})")

        try {
            // 3) Resolve the shell (prefer %ComSpec%)
            val comspecBuf = allocArray<UShortVar>(MAX_PATH)
            val comspecLen = GetEnvironmentVariableW("ComSpec", comspecBuf, MAX_PATH.toUInt())
            val cmdExe: String = if (comspecLen > 0u) {
                comspecBuf.toKString()
            } else {
                "C:\\Windows\\System32\\cmd.exe"
            }

            // Build mutable command line buffer for CreateProcessW
            // CreateProcessW expects the *command line* buffer to be mutable (it may write into it)
            val cmdLineStr = "/C $command"
            val cmdLineBuf = cmdLineStr.wideCString(this) // writable buffer from MemScope

            // 4) Launch child with redirected stdout/stderr
            val si = alloc<STARTUPINFOW>().apply {
                cb = sizeOf<STARTUPINFOW>().toUInt()
                dwFlags = STARTF_USESTDHANDLES.toUInt()
                hStdOutput = hOut
                hStdError = hOut
                hStdInput = GetStdHandle(STD_INPUT_HANDLE)
            }
            val pi = alloc<PROCESS_INFORMATION>()

            val created = CreateProcessW(
                /* lpApplicationName   = */ cmdExe,
                /* lpCommandLine       = */ cmdLineBuf, // mutable
                /* lpProcessAttributes = */ null,
                /* lpThreadAttributes  = */ null,
                /* bInheritHandles     = */ TRUE,
                /* dwCreationFlags     = */ CREATE_NO_WINDOW.toUInt(),
                /* lpEnvironment       = */ null,
                /* lpCurrentDirectory  = */ null,
                /* lpStartupInfo       = */ si.ptr,
                /* lpProcessInformation= */ pi.ptr,
            )
            if (created == 0) error("CreateProcessW failed (GetLastError=${GetLastError()})")

            try {
                // 5) Wait up to timeout; if it times out, terminate
                val waitRc: UInt = WaitForSingleObject(pi.hProcess, timeoutMillis.toUInt())
                if (waitRc == WAIT_TIMEOUT.toUInt()) {
                    TerminateProcess(pi.hProcess, 124u)
                    CloseHandle(hOut)
                    CloseHandle(pi.hThread)
                    CloseHandle(pi.hProcess)
                    _wunlink(outPath.wideCString(this))
                    error("Process timed out after ${timeoutMillis}ms")
                }

                // 6) Get exit code
                val exitCodeVar = alloc<DWORDVar>()
                if (GetExitCodeProcess(pi.hProcess, exitCodeVar.ptr) == 0) {
                    // If this fails, propagate a generic error but still try to read output
                    // (fall through with exitCode = -1)
                    exitCodeVar.value = 0xFFFFFFFFu
                }
                val exitCode = exitCodeVar.value.toInt()

                // 7) Read back the temp file (bounded)
                val hIn: HANDLE = CreateFileW(
                    /* lpFileName      = */ outPath,
                    /* dwDesiredAccess = */ GENERIC_READ.toUInt(),
                    /* dwShareMode     = */ (FILE_SHARE_READ or FILE_SHARE_WRITE),
                    /* lpSecurityAttributes = */ null,
                    /* dwCreationDisposition = */ OPEN_EXISTING,
                    /* dwFlagsAndAttributes  = */ FILE_ATTRIBUTE_NORMAL.toUInt(),
                    /* hTemplateFile   = */ null,
                )
                if (hIn == INVALID_HANDLE_VALUE) {
                    _wunlink(outPath.wideCString(this))
                    CloseHandle(pi.hThread)
                    CloseHandle(pi.hProcess)
                    return@memScoped exitCode to ""
                }

                val sb = StringBuilder()
                val buf = ByteArray(4096)
                val bytesReadVar = alloc<DWORDVar>()
                var total = 0L
                try {
                    while (true) {
                        val toRead = minOf(buf.size.toLong(), maxOutputLengthBytes - total).toInt()
                        if (toRead <= 0) {
                            // ensure cleanup before throwing
                            CloseHandle(hIn)
                            _wunlink(outPath.wideCString(this))
                            throw CredentialsProviderException("Process output exceeded limit of $maxOutputLengthBytes bytes")
                        }
                        val n = buf.usePinned {
                            val ok = ReadFile(hIn, it.addressOf(0), toRead.toUInt(), bytesReadVar.ptr, null)
                            if (ok == 0 || bytesReadVar.value == 0u) 0 else bytesReadVar.value.toInt()
                        }
                        if (n <= 0) break
                        total += n
                        sb.append(buf.decodeToString(0, n))
                    }
                } finally {
                    CloseHandle(hIn)
                    _wunlink(outPath.wideCString(this))
                }

                exitCode to sb.toString()
            } finally {
                CloseHandle(pi.hThread)
                CloseHandle(pi.hProcess)
            }
        } finally {
            CloseHandle(hOut)
        }
    }
}
