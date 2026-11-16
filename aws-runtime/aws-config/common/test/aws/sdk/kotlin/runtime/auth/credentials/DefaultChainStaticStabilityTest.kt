/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderChain
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsRefreshBehavior
import aws.smithy.kotlin.runtime.auth.awscredentials.resilientlyCached
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.time.ManualClock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Static stability through the default chain's provider order. The providers ahead of the credential source are not
 * configured, as on a typical host with no credentials in its environment, and their failures travel with the chain's
 * exception.
 */
class DefaultChainStaticStabilityTest {
    private class FlakySource(private val clock: ManualClock) : CredentialsProvider {
        var calls = 0
        var failing = false

        override suspend fun resolve(attributes: Attributes): Credentials {
            calls++
            if (failing) throw CredentialsProviderException("credential source timed out")
            return credentials(
                accessKeyId = "AKID$calls",
                secretAccessKey = "secret",
                expiration = clock.now() + 6.hours,
                refreshBehavior = CredentialsRefreshBehavior.RefreshableWithStaticStability,
            )
        }
    }

    @Test
    fun testUnconfiguredProvidersDoNotMakeAChainFailureNonRecoverable() = runTest {
        val clock = ManualClock()
        val source = FlakySource(clock)
        val chain = CredentialsProviderChain(
            SystemPropertyCredentialsProvider { null },
            EnvironmentCredentialsProvider { null },
            source,
        ).resilientlyCached(clock = clock)

        val cached = chain.resolve()

        // Inside the mandatory window, the source starts failing.
        clock.advance(6.hours - 30.seconds)
        source.failing = true

        assertEquals(cached.accessKeyId, chain.resolve().accessKeyId, "a failed refresh keeps the cached credentials")
        assertEquals(cached.accessKeyId, chain.resolve().accessKeyId)
        assertEquals(2, source.calls, "the refresh backoff, not the brief error cache, stops further source calls")
    }

    @Test
    fun testUnconfiguredProvidersThrowARecoverableError() = runTest {
        val env = assertFailsWith<ProviderConfigurationException> { EnvironmentCredentialsProvider { null }.resolve() }
        val props = assertFailsWith<ProviderConfigurationException> { SystemPropertyCredentialsProvider { null }.resolve() }

        assertFalse(env.sdkErrorMetadata.isNonRecoverable)
        assertFalse(props.sdkErrorMetadata.isNonRecoverable)
    }
}
