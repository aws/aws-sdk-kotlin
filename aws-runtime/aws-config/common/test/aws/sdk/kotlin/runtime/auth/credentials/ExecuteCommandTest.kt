/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.runtime.auth.credentials.executeCommand
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

class ExecuteCommandTest {
    val provider = PlatformProvider.System

    val platformNewline = when (provider.osInfo().family) {
        OsFamily.Windows -> "\r\n"
        else -> "\n"
    }

    @Test
    fun testExecuteCommand() = runTest {
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
    fun testExecutionTimedOut() = runTest {
        assertFails {
            executeCommand(
                command = "this won't be executed",
                platformProvider = provider,
                maxOutputLengthBytes = 1024L,
                timeoutMillis = 0,
            )
        }
    }

    @Test
    fun testTooManyBytes() = runTest {
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
    fun testCommandHasQuotes() = runTest {
        val command = when (provider.osInfo().family) {
            OsFamily.Windows -> """echo "Hello, in quotes!""""
            else -> """echo \"Hello, in quotes!\""""
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
    fun testMultipleArgumentCommand() = runTest {
        val command = when (provider.osInfo().family) {
            OsFamily.Windows -> "powershell -Command \"& {Write-Host 'Arg1'; Write-Host 'Arg2'; Write-Host 'Arg3'}\""
            else -> "printf '%s\\n%s\\n%s\\n' 'Arg1' 'Arg2' 'Arg3'"
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
    fun testErrorReturnsStderr() = runTest {
        val errorCommand = when (provider.osInfo().family) {
            OsFamily.Windows -> "echo Error message 1>&2 & exit /b 13"
            else -> "echo 'Error message' >&2; exit 13"
        }

        // Windows command output has an extra space at the end
        // Can't wrap it in quotes because Windows just echoes them back
        // Can't use `<nul set /p` because that doesn't terminate with CRLF
        val expectedOutput = when (provider.osInfo().family) {
            OsFamily.Windows -> "Error message  "
            else -> "Error message"
        }

        val (exitCode, output) = executeCommand(
            command = errorCommand,
            platformProvider = provider,
            maxOutputLengthBytes = 1024L,
            timeoutMillis = 1000,
        )

        assertEquals(13, exitCode)
        assertEquals("$expectedOutput$platformNewline", output)
    }
}
