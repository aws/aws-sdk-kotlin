/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.config.profile.loadAwsSharedConfig
import aws.sdk.kotlin.runtime.config.useragent.resolveUserAgentAppId
import aws.sdk.kotlin.runtime.config.utils.mockPlatform
import aws.sdk.kotlin.runtime.http.retries.AwsRetryPolicy
import aws.sdk.kotlin.runtime.http.retries.LegacyAwsRetryPolicy
import aws.sdk.kotlin.runtime.region.DefaultRegionProviderChain
import aws.smithy.kotlin.runtime.client.*
import aws.smithy.kotlin.runtime.client.region.RegionProvider
import aws.smithy.kotlin.runtime.retries.AdaptiveRetryStrategy
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.retries.delay.ExponentialBackoffWithJitter
import aws.smithy.kotlin.runtime.retries.delay.StandardRetryTokenBucket
import aws.smithy.kotlin.runtime.retries.policy.RetryPolicy
import aws.smithy.kotlin.runtime.testing.withEnvVars
import aws.smithy.kotlin.runtime.testing.withSystemProperties
import aws.smithy.kotlin.runtime.testing.withTempDir
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AbstractAwsSdkClientFactoryTest {

    @Test
    fun testFromEnvironmentFavorsExplicitConfig() = runTest {
        val explicitRegion = "explicit-region"
        val explicitRetryStrategy = StandardRetryStrategy()
        val client = TestClient.fromEnvironment {
            region = explicitRegion
            retryStrategy = explicitRetryStrategy
        }

        assertEquals(explicitRegion, client.config.region)
        assertEquals(explicitRetryStrategy, client.config.retryStrategy)
    }

    @Test
    fun testFromEnvironmentResolvesDefaults() = runTest {
        withSystemProperties(
            mapOf(
                AwsSdkSetting.AwsRegion.sysProp to "resolved-region",
                AwsSdkSetting.AwsRetryMode.sysProp to "standard",
            ),
        ) {
            val client = TestClient.fromEnvironment()
            assertEquals("resolved-region", client.config.region)
            assertIs<StandardRetryStrategy>(client.config.retryStrategy)
        }
    }

    @Test
    fun testFromEnvironmentResolvesAppId() = runTest(
        timeout = 20.seconds,
    ) {
        withTempDir { dir ->
            val credentialsFile = Path(dir, "credentials")
            val configFile = Path(dir, "config")

            SystemFileSystem.sink(configFile).buffered().use { it.writeString("[profile foo]\nsdk_ua_app_id = profile-app-id") }

            val testPlatform = mockPlatform(
                pathSegment = PlatformProvider.System.filePathSeparator,
                awsProfileEnv = "foo",
                homeEnv = "/home/user",
                awsConfigFileEnv = configFile.toString(),
                awsSharedCredentialsFileEnv = credentialsFile.toString(),
                os = PlatformProvider.System.osInfo(),
            )

            val sharedConfig = asyncLazy { loadAwsSharedConfig(testPlatform) }
            val profile = asyncLazy { sharedConfig.get().activeProfile }

            assertEquals("profile-app-id", resolveUserAgentAppId(testPlatform, profile))
        }

        withEnvVars(
            mapOf(
                AwsSdkSetting.AwsAppId.envVars.first() to "env-app-id",
            ),
        ) {
            assertEquals("env-app-id", TestClient.fromEnvironment().config.applicationId)

            withSystemProperties(
                mapOf(
                    AwsSdkSetting.AwsAppId.sysProps.first() to "system-properties-app-id",
                ),
            ) {
                assertEquals("system-properties-app-id", TestClient.fromEnvironment().config.applicationId)
                assertEquals(
                    "explicit-app-id",
                    TestClient.fromEnvironment { applicationId = "explicit-app-id" }.config.applicationId,
                )
            }
        }
    }

    // Point the shared config and credentials files at a missing path so the host's profile can't affect the result
    private suspend fun withIsolatedEnvironment(props: Map<String, String> = emptyMap(), block: suspend () -> Unit) {
        withTempDir { dir ->
            withSystemProperties(
                mapOf(
                    AwsSdkSetting.AwsConfigFile.sysProp to Path(dir, "missing-config").toString(),
                    AwsSdkSetting.AwsSharedCredentialsFile.sysProp to Path(dir, "missing-credentials").toString(),
                ) + props,
            ) { block() }
        }
    }

    private val legacyMode = mapOf(AwsSdkSetting.AwsRetryMode.sysProp to "legacy")

    private val TestClient.standardStrategy: StandardRetryStrategy
        get() = assertIs<StandardRetryStrategy>(config.retryStrategy)

    private val TestClient.bucketConfig: StandardRetryTokenBucket.Config
        get() = assertIs<StandardRetryTokenBucket.Config>(standardStrategy.config.tokenBucket.config)

    private val TestClient.delayConfig: ExponentialBackoffWithJitter.Config
        get() = assertIs<ExponentialBackoffWithJitter.Config>(standardStrategy.config.delayProvider.config)

    @Test
    fun testFromEnvironmentUsesDefaultRetryPolicy() = runTest {
        withIsolatedEnvironment {
            assertSame(AwsRetryPolicy.Default, TestClient.fromEnvironment().config.retryPolicy)
        }
    }

    @Test
    fun testFromEnvironmentUsesLegacyRetryPolicyInLegacyMode() = runTest {
        withIsolatedEnvironment(legacyMode) {
            assertSame(LegacyAwsRetryPolicy, TestClient.fromEnvironment().config.retryPolicy)
        }
    }

    @Test
    fun testFromEnvironmentFavorsExplicitRetryPolicyInLegacyMode() = runTest {
        withIsolatedEnvironment(legacyMode) {
            val client = TestClient.fromEnvironment { retryPolicy = AwsRetryPolicy.Default }
            assertSame(AwsRetryPolicy.Default, client.config.retryPolicy)
        }
    }

    @Test
    fun testFromEnvironmentLegacyModeIgnoresServiceDefaults() = runTest {
        withIsolatedEnvironment(legacyMode) {
            val client = ServiceDefaultsTestClientFactory.fromEnvironment()
            assertEquals(StandardRetryStrategy.Config.DEFAULT_MAX_ATTEMPTS, client.standardStrategy.config.maxAttempts)
            assertEquals(10.milliseconds, client.delayConfig.initialDelay)
            assertEquals(5, client.bucketConfig.retryCost)
        }
    }

    @Test
    fun testFromEnvironmentAppliesRetryStrategyDslOnTopOfResolvedAdaptiveStrategy() = runTest {
        withIsolatedEnvironment(mapOf(AwsSdkSetting.AwsRetryMode.sysProp to "adaptive")) {
            val client = TestClient.fromEnvironment { retryStrategy { maxAttempts = 2 } }
            assertIs<AdaptiveRetryStrategy>(client.config.retryStrategy)
            assertEquals(2, client.standardStrategy.config.maxAttempts)
            assertEquals(14, client.bucketConfig.retryCost)
        }
    }
}
