/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials.internal

import aws.smithy.kotlin.runtime.auth.awscredentials.CloseableCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.RefreshAwareCredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.identity.Identity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ManagedCredentialsProviderTest {
    private class RefreshAwareSource :
        CloseableCredentialsProvider,
        RefreshAwareCredentialsProvider {
        var invalidated: Identity? = null

        override suspend fun resolve(attributes: Attributes): Credentials = credentials("AKID", "secret", "token")
        override suspend fun invalidate(rejectedIdentity: Identity) {
            invalidated = rejectedIdentity
        }
        override fun close() {}
    }

    @Test
    fun testARefreshAwareProviderStaysRefreshAware() = runTest {
        val source = RefreshAwareSource()
        val managed = source.manage()

        // The invalidation interceptor marks a rejected request for retry only for a refresh-aware provider.
        assertTrue(managed is RefreshAwareCredentialsProvider)

        val rejected = managed.resolve()
        managed.invalidate(rejected)
        assertEquals(rejected, source.invalidated, "invalidate still reaches the delegate")
    }

    @Test
    fun testAProviderThatIsNotRefreshAwareStaysThatWay() {
        val plain = object : CloseableCredentialsProvider {
            override suspend fun resolve(attributes: Attributes): Credentials = credentials("AKID", "secret", "token")
            override fun close() {}
        }
        val managed = plain.manage()

        assertFalse(managed is RefreshAwareCredentialsProvider)
    }

    @Test
    fun testManagingTwiceReturnsTheSameWrapper() {
        val managed = RefreshAwareSource().manage()

        assertSame(managed, (managed as CloseableCredentialsProvider).manage())
    }
}
