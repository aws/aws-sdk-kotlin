/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.config.profile.loadAwsSharedConfig
import aws.sdk.kotlin.runtime.config.useragent.resolveUserAgentAppId
import aws.sdk.kotlin.runtime.config.utils.mockPlatform
import aws.sdk.kotlin.runtime.region.DefaultRegionProviderChain
import aws.smithy.kotlin.runtime.client.*
import aws.smithy.kotlin.runtime.client.region.RegionProvider
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.testing.withTempDir
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy
import io.kotest.extensions.system.withEnvironment
import io.kotest.extensions.system.withSystemProperties
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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

    // FIXME java.lang.reflect.InaccessibleObjectException: Unable to make field private final java.util.Map java.util.Collections$UnmodifiableMap.m accessible: module java.base does not "opens java.util" to unnamed module @7ee7980d
    @Ignore
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

        withEnvironment(
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
}

private interface TestClient : SdkClient {
    override val config: Config

    // refactored: mostly in an abstract base now
    companion object : AbstractAwsSdkClientFactory<Config, Config.Builder, TestClient, Builder>() {
        override fun builder(): Builder = Builder()
    }

    class Builder internal constructor() : AbstractSdkClientBuilder<Config, Config.Builder, TestClient>() {
        override val config: Config.Builder = Config.Builder()
        override fun newClient(config: Config): TestClient = DefaultTestClient(config)
    }

    class Config private constructor(builder: Builder) :
        SdkClientConfig,
        AwsSdkClientConfig,
        RetryStrategyClientConfig by builder.buildRetryStrategyClientConfig() {
        override val clientName: String = builder.clientName
        override val logMode: LogMode = builder.logMode ?: LogMode.Default
        override val region: String? = builder.region
        override var regionProvider: RegionProvider = builder.regionProvider ?: DefaultRegionProviderChain()
        override var useFips: Boolean = builder.useFips ?: false
        override var useDualStack: Boolean = builder.useDualStack ?: false
        override val applicationId: String? = builder.applicationId

        // new: inherits builder equivalents for Config base classes
        class Builder :
            AwsSdkClientConfig.Builder,
            SdkClientConfig.Builder<Config>,
            RetryStrategyClientConfig.Builder by RetryStrategyClientConfigImpl.BuilderImpl() {
            override var clientName: String = "Test"
            override var logMode: LogMode? = LogMode.Default
            override var region: String? = null
            override var regionProvider: RegionProvider? = null
            override var useFips: Boolean? = null
            override var useDualStack: Boolean? = null
            override var applicationId: String? = null
            override fun build(): Config = Config(this)
        }
    }
}

private class DefaultTestClient(
    override val config: TestClient.Config,
) : TestClient {
    override fun close() { }
}
