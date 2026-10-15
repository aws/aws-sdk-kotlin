/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsRefreshBehavior
import aws.smithy.kotlin.runtime.auth.awscredentials.copy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StaticCredentialsProviderTest {
    @Test
    fun testStaticProvider() = runTest {
        val expected = Credentials("access_key_id", "secret_access_key", "session_token")
        // Credentials handed in directly are returned exactly as given, including their attributes.
        val provider = StaticCredentialsProvider(expected)
        assertEquals(expected, provider.resolve())

        // Credentials this provider builds itself declare that they are not refreshable.
        val provider2 = StaticCredentialsProvider {
            accessKeyId = expected.accessKeyId
            secretAccessKey = expected.secretAccessKey
            sessionToken = expected.sessionToken
        }
        val actual = provider2.resolve().copy(providerName = null)
        assertEquals(expected.withRefreshBehavior(CredentialsRefreshBehavior.NonRefreshable), actual)
    }
}
