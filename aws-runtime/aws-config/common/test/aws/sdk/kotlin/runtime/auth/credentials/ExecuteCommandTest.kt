/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Whether the current target can spawn child processes at all (iOS apps cannot).
 */
internal expect val processSpawningSupported: Boolean

class ExecuteCommandTest {
    val provider = PlatformProvider.System

    val isWindows = provider.osInfo().family == OsFamily.Windows

    val platformNewline = if (isWindows) "\r\n" else "\n"

    private fun spawningTest(block: suspend () -> Unit) = runTest {
        if (processSpawningSupported) block()
    }

    @Test
    fun testExecuteCommand() = spawningTest {
        val command = "echo Hello, World!"

        val (exitCode, output) = executeCommand(
            command = command,
            platformProvider = provider,
            maxOutputLengthBytes = 1024L,
            timeoutMillis = 1000,
        )

        assertEquals(0, exitCode)
        assertEquals("Hello, World!$platformNewline", output)
    }

    @Test
    fun testExecutionTimedOut() = spawningTest {
        assertFailsWith<TimeoutCancellationException> {
            executeCommand(
                command = "this won't be executed",
                platformProvider = provider,
                maxOutputLengthBytes = 1024L,
                timeoutMillis = 0,
            )
        }
    }

    @Test
    fun testTooManyBytes() = spawningTest {
        val command = "echo ${"Hello! ".repeat(500)}"

        val ex = assertFailsWith<CredentialsProviderException> {
            executeCommand(
                command = command,
                platformProvider = provider,
                maxOutputLengthBytes = 1024L,
                timeoutMillis = 1000,
            )
        }

        assertEquals("Process output exceeded limit of 1024 bytes", ex.message)
    }

    @Test
    fun testOutputExactlyAtLimit() = spawningTest {
        val limit = 1024
        val payload = "a".repeat(limit - platformNewline.length)

        val (exitCode, output) = executeCommand(
            command = "echo $payload",
            platformProvider = provider,
            maxOutputLengthBytes = limit.toLong(),
            timeoutMillis = 1000,
        )

        assertEquals(0, exitCode)
        assertEquals("$payload$platformNewline", output)
        assertEquals(limit, output.encodeToByteArray().size)
    }

    @Test
    fun testMultiByteOutput() = spawningTest {
        // cmd.exe writes in the console code page rather than UTF-8
        if (isWindows) return@spawningTest

        val (exitCode, output) = executeCommand(
            command = "printf 'h\u00e9llo \u2603'",
            platformProvider = provider,
            maxOutputLengthBytes = 1024L,
            timeoutMillis = 1000,
        )

        assertEquals(0, exitCode)
        assertEquals("h\u00e9llo \u2603", output)
    }

    @Test
    fun testStderrNotMixedIntoStdout() = spawningTest {
        val command = if (isWindows) {
            """echo {"Version": 1}& echo noise 1>&2"""
        } else {
            """echo '{"Version": 1}'; echo noise >&2"""
        }

        val (exitCode, output) = executeCommand(
            command = command,
            platformProvider = provider,
            maxOutputLengthBytes = 1024L,
            timeoutMillis = 1000,
        )

        assertEquals(0, exitCode)
        assertEquals("""{"Version": 1}$platformNewline""", output)
    }

    @Test
    fun testCommandHasQuotes() = spawningTest {
        val command = if (isWindows) {
            """echo "Hello, in quotes!""""
        } else {
            """echo \"Hello, in quotes!\""""
        }

        val (exitCode, output) = executeCommand(
            command = command,
            platformProvider = provider,
            maxOutputLengthBytes = 1024L,
            timeoutMillis = 1000,
        )

        assertEquals(0, exitCode)
        assertEquals(""""Hello, in quotes!"$platformNewline""", output)
    }

    @Test
    fun testMultipleArgumentCommand() = spawningTest {
        val command = if (isWindows) {
            "powershell -Command \"& {Write-Host 'Arg1'; Write-Host 'Arg2'; Write-Host 'Arg3'}\""
        } else {
            "printf '%s\\n%s\\n%s\\n' 'Arg1' 'Arg2' 'Arg3'"
        }

        val (exitCode, output) = executeCommand(
            command = command,
            platformProvider = provider,
            maxOutputLengthBytes = 1024L,
            timeoutMillis = 1000,
        )

        assertEquals(0, exitCode)
        assertEquals("Arg1\nArg2\nArg3\n", output)
    }

    @Test
    fun testErrorReturnsStderr() = spawningTest {
        val errorCommand = if (isWindows) {
            "echo Error message 1>&2 & exit /b 13"
        } else {
            "echo 'Error message' >&2; exit 13"
        }

        // Windows command output has an extra space at the end
        // Can't wrap it in quotes because Windows just echoes them back
        // Can't use `<nul set /p` because that doesn't terminate with CRLF
        val expectedOutput = if (isWindows) "Error message  " else "Error message"

        val (exitCode, output) = executeCommand(
            command = errorCommand,
            platformProvider = provider,
            maxOutputLengthBytes = 1024L,
            timeoutMillis = 1000,
        )

        assertEquals(13, exitCode)
        assertEquals("$expectedOutput$platformNewline", output)
    }

    @Test
    fun testKilledBySignalIsFailure() = spawningTest {
        if (isWindows) return@spawningTest

        val (exitCode, _) = executeCommand(
            command = "kill -9 $$",
            platformProvider = provider,
            maxOutputLengthBytes = 1024L,
            timeoutMillis = 1000,
        )

        assertEquals(128 + 9, exitCode)
    }
}
