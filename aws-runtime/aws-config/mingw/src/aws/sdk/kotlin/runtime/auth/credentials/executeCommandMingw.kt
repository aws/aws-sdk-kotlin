/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.smithy.kotlin.runtime.client.util

import kotlinx.cinterop.*
import kotlinx.coroutines.withContext
import aws.sdk.kotlin.runtime.util.SdkDispatchers // adjust import
import platform.windows.*
import platform.posix._wunlink // to delete the temp file afterwards

@OptIn(ExperimentalForeignApi::class)
private fun String.wideCString(mem: MemScope) = wcstr.getPointer(mem)

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun executeCommand(
    command: String,
    platformProvider: PlatformProvider,
    maxOutputLengthBytes: Long,
    timeoutMillis: Long,
    clock: Clock,
): Pair<Int, String> = withContext(SdkDispatchers.IO) { memScoped {
        // 1) Make a temp file to capture stdout+stderr
        val tmpDirBuf = allocArray<UShortVar>(MAX_PATH)
        val tmpNameBuf = allocArray<UShortVar>(MAX_PATH)
        val gotTmp = GetTempPathW(MAX_PATH, tmpDirBuf) != 0u
        if (!gotTmp) error("GetTempPathW failed")

        val gotName = GetTempFileNameW(tmpDirBuf, "KNR".wideCString(this), 0u, tmpNameBuf) != 0u
        if (!gotName) error("GetTempFileNameW failed")
        val outPath = tmpNameBuf

        // Create the file for the child to write into (inherit handle)
        val sa = alloc<SECURITY_ATTRIBUTES>().apply {
            nLength = sizeOf<SECURITY_ATTRIBUTES>().toUInt()
            bInheritHandle = TRUE
            lpSecurityDescriptor = null
        }
        val hOut: HANDLE = CreateFileW(
            outPath,
            (GENERIC_WRITE or FILE_GENERIC_WRITE).toUInt(),
            FILE_SHARE_READ or FILE_SHARE_WRITE,
            sa.ptr,
            CREATE_ALWAYS,
            FILE_ATTRIBUTE_NORMAL.toUInt(),
            null
        )
        if (hOut == INVALID_HANDLE_VALUE) error("CreateFileW failed for temp output")

        try {
            // 2) Build command: use cmd.exe /C "<command>"
            // Prefer %ComSpec% if present, else fallback.
            val comspecBuf = allocArray<WCHARVar>(MAX_PATH)
            val comspecLen = GetEnvironmentVariableW("ComSpec".wideCString(this), comspecBuf, MAX_PATH)
            val cmdExe = if (comspecLen > 0u) { comspecBuf } else { "C:\\Windows\\System32\\cmd.exe".wideCString(this) }

            val cmdLine = (" /C " + command).wideCString(this)

            // 3) Launch child with redirected stdout/stderr
            val si = alloc<STARTUPINFOW>().apply {
                cb = sizeOf<STARTUPINFOW>().toUInt()
                dwFlags = STARTF_USESTDHANDLES.toUInt()
                hStdOutput = hOut
                hStdError = hOut
                hStdInput = GetStdHandle(STD_INPUT_HANDLE) // leave as-is
            }
            val pi = alloc<PROCESS_INFORMATION>()

            val created = CreateProcessW(
                cmdExe,
                cmdLine,        // mutable buffer OK; wcstr gives writable copy here
                null,
                null,
                TRUE,           // inherit handles (so child gets hOut)
                CREATE_NO_WINDOW.toUInt(),
                null,
                null,
                si.ptr,
                pi.ptr
            )
            if (created == 0) error("CreateProcessW failed: ${GetLastError()}")

            try {
                // 4) Wait up to timeout; if it times out, terminate
                val waitRc = WaitForSingleObject(pi.hProcess, timeoutMillis.toUInt())
                if (waitRc == WAIT_TIMEOUT) {
                    TerminateProcess(pi.hProcess, 124u)
                    // close I/O + process handles and delete temp before throwing
                    CloseHandle(hOut)
                    CloseHandle(pi.hThread)
                    CloseHandle(pi.hProcess)
                    _wunlink(outPath)
                    error("Process timed out after ${timeoutMillis}ms")
                }


                // 5) Get exit code
                val exitCodeVar = alloc<DWORDVar>()
                GetExitCodeProcess(pi.hProcess, exitCodeVar.ptr)
                val exitCode = exitCodeVar.value.toInt()

                // 6) Read the file (up to maxOutputLengthBytes)
                //    Re-open for reading (child still closed hOut on exit)
                val hIn: HANDLE = CreateFileW(
                    outPath,
                    GENERIC_READ.toUInt(),
                    FILE_SHARE_READ or FILE_SHARE_WRITE,
                    null,
                    OPEN_EXISTING,
                    FILE_ATTRIBUTE_NORMAL.toUInt(),
                    null
                )
                if (hIn == INVALID_HANDLE_VALUE) {
                    // Clean up and bail
                    _wunlink(outPath)
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
                            _wunlink(outPath)
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
                    _wunlink(outPath)
                }
                exitCode to sb.toString()
            } finally {
                CloseHandle(pi.hThread)
                CloseHandle(pi.hProcess)
            }
        } finally {
            CloseHandle(hOut)
        }
} }
