/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.runTest
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
}
