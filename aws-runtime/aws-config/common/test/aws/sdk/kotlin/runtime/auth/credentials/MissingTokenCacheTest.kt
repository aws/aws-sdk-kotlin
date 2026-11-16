/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * A missing SSO or Login token cache needs the customer to sign in again, so it is non-recoverable.
 */
class MissingTokenCacheTest {
    @Test
    fun testMissingSsoTokenCacheIsNonRecoverable() = runTest {
        val ex = assertFailsWith<ProviderConfigurationException> { readTokenFromCache("start-url", TestPlatformProvider()) }
        assertTrue(ex.sdkErrorMetadata.isNonRecoverable)
    }

    @Test
    fun testMissingLoginTokenCacheIsNonRecoverable() = runTest {
        val ex = assertFailsWith<ProviderConfigurationException> {
            readLoginTokenFromCache("arn:aws:iam::123456789012:user/test", TestPlatformProvider(), "/home/.aws/login/cache")
        }
        assertTrue(ex.sdkErrorMetadata.isNonRecoverable)
    }
}
