/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.identity.IdentityProviderException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DefaultChainBearerTokenProviderTest {
    @Test
    fun testCheckpointAndRestoreHooksArePairedAndIdempotent() = runTest {
        val provider = DefaultChainBearerTokenProvider()

        provider.afterRestore()
        provider.beforeCheckpoint()
        provider.beforeCheckpoint()
        assertFailsWith<IdentityProviderException> { provider.resolve() }

        provider.afterRestore()
        provider.afterRestore()
        provider.close()
    }

    @Test
    fun testCloseDuringCheckpointPreventsRestore() = runTest {
        val provider = DefaultChainBearerTokenProvider()

        provider.beforeCheckpoint()
        provider.close()
        provider.afterRestore()

        assertFailsWith<IdentityProviderException> { provider.resolve() }
    }
}
