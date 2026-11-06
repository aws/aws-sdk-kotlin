/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.smithy.kotlin.runtime.retries.AdaptiveRetryStrategy
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.retries.delay.ExponentialBackoffWithJitter
import aws.smithy.kotlin.runtime.retries.delay.StandardRetryTokenBucket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class AbstractAwsSdkClientFactoryInvokeTest {
    private val TestClient.standardStrategy: StandardRetryStrategy
        get() = assertIs<StandardRetryStrategy>(config.retryStrategy)

    private val TestClient.bucketConfig: StandardRetryTokenBucket.Config
        get() = assertIs<StandardRetryTokenBucket.Config>(standardStrategy.config.tokenBucket.config)

    private val TestClient.delayConfig: ExponentialBackoffWithJitter.Config
        get() = assertIs<ExponentialBackoffWithJitter.Config>(standardStrategy.config.delayProvider.config)

    @Test
    fun testUsesServiceRetryDefaults() {
        val client = ServiceDefaultsTestClientFactory { }
        assertTrue(client.config.retryStrategy !is AdaptiveRetryStrategy)
        assertEquals(4, client.standardStrategy.config.maxAttempts)
        assertEquals(25.milliseconds, client.delayConfig.initialDelay)
        assertEquals(2.0, client.delayConfig.scaleFactor)
        assertEquals(14, client.bucketConfig.retryCost)
        assertEquals(5, client.bucketConfig.timeoutRetryCost)
        assertTrue(client.standardStrategy.config.enableLongPollingBackoff)
    }

    @Test
    fun testRetryStrategyDslOverridesServiceRetryDefaults() {
        val client = ServiceDefaultsTestClientFactory { retryStrategy { maxAttempts = 2 } }
        assertEquals(2, client.standardStrategy.config.maxAttempts)
        assertEquals(25.milliseconds, client.delayConfig.initialDelay)
    }

    @Test
    fun testExplicitRetryStrategyReplacesServiceRetryDefaults() {
        val explicitRetryStrategy = StandardRetryStrategy()
        val client = ServiceDefaultsTestClientFactory { retryStrategy = explicitRetryStrategy }
        assertSame(explicitRetryStrategy, client.config.retryStrategy)
    }

    @Test
    fun testExplicitAdaptiveRetryStrategyReplacesServiceRetryDefaults() {
        val client = ServiceDefaultsTestClientFactory { retryStrategy(AdaptiveRetryStrategy) { maxAttempts = 2 } }
        assertIs<AdaptiveRetryStrategy>(client.config.retryStrategy)
        assertEquals(2, client.standardStrategy.config.maxAttempts)
    }

    @Test
    fun testNoServiceRetryDefaultsUsesRuntimeDefaults() {
        val client = TestClient { }
        assertEquals(StandardRetryStrategy.Config.DEFAULT_MAX_ATTEMPTS, client.standardStrategy.config.maxAttempts)
        assertEquals(50.milliseconds, client.delayConfig.initialDelay)
    }
}
