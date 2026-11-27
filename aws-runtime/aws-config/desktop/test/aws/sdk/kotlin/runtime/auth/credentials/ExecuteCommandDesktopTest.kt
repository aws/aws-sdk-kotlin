/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class ExecuteCommandDesktopTest {
    @Test
    fun testTimeoutInterruptsRunningProcess() = runTest {
        val provider = PlatformProvider.System
        val command = when (provider.osInfo().family) {
            OsFamily.Windows -> "ping -n 6 127.0.0.1 >NUL"
            else -> "sleep 5"
        }

        val start = TimeSource.Monotonic.markNow()
        assertFailsWith<TimeoutCancellationException> {
            executeCommand(
                command = command,
                platformProvider = provider,
                maxOutputLengthBytes = 1024L,
                timeoutMillis = 1000,
            )
        }

        val elapsed = start.elapsedNow()
        assertTrue(elapsed in 0.5.seconds..4.seconds, "Expected the timeout to fire after ~1s but it took $elapsed")
    }

    @Test
    fun testConcurrentCommandsDontWaitOnEachOther() = runBlocking {
        val provider = PlatformProvider.System
        val (slowCommand, fastCommand) = when (provider.osInfo().family) {
            OsFamily.Windows -> "ping -n 9 127.0.0.1 >NUL" to "echo fast"
            else -> "sleep 8" to "echo fast"
        }

        suspend fun run(command: String) = executeCommand(
            command = command,
            platformProvider = provider,
            maxOutputLengthBytes = 1024L,
            timeoutMillis = 30_000,
        )

        withContext(Dispatchers.Default) {
            val slow = async { run(slowCommand) }
            val fastDurations = List(8) {
                async {
                    val start = TimeSource.Monotonic.markNow()
                    run(fastCommand)
                    start.elapsedNow()
                }
            }.awaitAll()
            slow.await()

            val slowest = fastDurations.max()
            assertTrue(slowest < 4.seconds, "Expected fast commands to finish without waiting on the slow one; slowest took $slowest")
        }
    }
}
