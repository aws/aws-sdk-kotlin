/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.http.retries.AwsRetryPolicy
import aws.sdk.kotlin.runtime.region.DefaultRegionProviderChain
import aws.smithy.kotlin.runtime.client.*
import aws.smithy.kotlin.runtime.client.region.RegionProvider
import aws.smithy.kotlin.runtime.retries.policy.RetryPolicy
import kotlin.time.Duration.Companion.milliseconds

internal object ServiceDefaultsTestClientFactory :
    AbstractAwsSdkClientFactory<TestClient.Config, TestClient.Config.Builder, TestClient, TestClient.Builder>() {
    override fun builder(): TestClient.Builder = TestClient.Builder()
    override val defaultMaxAttempts: Int = 4
    override val defaultInitialDelay = 25.milliseconds
}

internal interface TestClient : SdkClient {
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
        LogRedactionConfig,
        RetryClientConfig,
        RetryStrategyClientConfig by builder.buildRetryStrategyClientConfig() {
        override val retryPolicy: RetryPolicy<Any?> = builder.retryPolicy ?: AwsRetryPolicy.Default
        override val clientName: String = builder.clientName
        override val logMode: LogMode = builder.logMode ?: LogMode.Default
        override val logRedactedHeaders: Set<String> = builder.logRedactedHeaders
        override val region: String? = builder.region
        override var regionProvider: RegionProvider = builder.regionProvider ?: DefaultRegionProviderChain()
        override var useFips: Boolean = builder.useFips ?: false
        override var useDualStack: Boolean = builder.useDualStack ?: false
        override val applicationId: String? = builder.applicationId

        // new: inherits builder equivalents for Config base classes
        class Builder :
            AwsSdkClientConfig.Builder,
            LogRedactionConfig.Builder,
            SdkClientConfig.Builder<Config>,
            RetryClientConfig.Builder,
            RetryStrategyClientConfig.Builder by RetryStrategyClientConfigImpl.BuilderImpl() {
            override var retryPolicy: RetryPolicy<Any?>? = null
            override var clientName: String = "Test"
            override var logMode: LogMode? = LogMode.Default
            override var logRedactedHeaders: MutableSet<String> = mutableSetOf()
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
