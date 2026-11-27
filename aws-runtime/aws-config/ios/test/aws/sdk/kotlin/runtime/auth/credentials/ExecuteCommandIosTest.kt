/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal actual val processSpawningSupported: Boolean = false

class ExecuteCommandIosTest {
    @Test
    fun testCredentialProcessUnsupported() = runTest {
        val ex = assertFailsWith<CredentialsProviderException> {
            executeCommand(
                command = "echo Hello",
                platformProvider = PlatformProvider.System,
                maxOutputLengthBytes = 1024L,
                timeoutMillis = 1000,
            )
        }

        assertEquals("credential_process is not supported on iOS because apps cannot spawn child processes", ex.message)
    }
}
