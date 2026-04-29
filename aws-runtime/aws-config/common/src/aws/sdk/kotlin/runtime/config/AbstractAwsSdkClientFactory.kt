/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.config.auth.resolveAuthSchemePreference
import aws.sdk.kotlin.runtime.config.checksums.resolveRequestChecksumCalculation
import aws.sdk.kotlin.runtime.config.checksums.resolveResponseChecksumValidation
import aws.sdk.kotlin.runtime.config.compression.resolveDisableRequestCompression
import aws.sdk.kotlin.runtime.config.compression.resolveRequestMinCompressionSizeBytes
import aws.sdk.kotlin.runtime.config.endpoints.resolveUseDualStack
import aws.sdk.kotlin.runtime.config.endpoints.resolveUseFips
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.AwsSharedConfig
import aws.sdk.kotlin.runtime.config.profile.loadAwsSharedConfig
import aws.sdk.kotlin.runtime.config.retries.resolveRetryStrategy
import aws.sdk.kotlin.runtime.config.useragent.resolveUserAgentAppId
import aws.sdk.kotlin.runtime.region.resolveRegion
import aws.sdk.kotlin.runtime.region.resolveSigV4aSigningRegionSet
import aws.smithy.kotlin.runtime.ExperimentalApi
import aws.smithy.kotlin.runtime.auth.awscredentials.SigV4aClientConfig
import aws.smithy.kotlin.runtime.client.AbstractSdkClientFactory
import aws.smithy.kotlin.runtime.client.RetryStrategyClientConfig
import aws.smithy.kotlin.runtime.client.SdkClient
import aws.smithy.kotlin.runtime.client.SdkClientConfig
import aws.smithy.kotlin.runtime.client.config.ClientSettings
import aws.smithy.kotlin.runtime.client.config.CompressionClientConfig
import aws.smithy.kotlin.runtime.client.config.HttpChecksumConfig
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.http.auth.HttpAuthConfig
import aws.smithy.kotlin.runtime.telemetry.TelemetryConfig
import aws.smithy.kotlin.runtime.telemetry.TelemetryProvider
import aws.smithy.kotlin.runtime.telemetry.trace.withSpan
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy
import kotlin.time.Duration

/**
 * Abstract base class all AWS client companion objects inherit from
 *
 * @param TConfig the type of the service client configuration
 * @param TConfigBuilder the type of builder that creates [TConfig] instances
 * @param TClient the type of service client
 * @param TClientBuilder the type of builder that creates [TClient] instances
 */
public abstract class AbstractAwsSdkClientFactory<
    TConfig,
    TConfigBuilder,
    TClient : SdkClient,
    TClientBuilder : SdkClient.Builder<TConfig, TConfigBuilder, TClient>,
    > : AbstractSdkClientFactory<TConfig, TConfigBuilder, TClient, TClientBuilder>()
    where TConfig : SdkClientConfig,
          TConfig : AwsSdkClientConfig,
          TConfigBuilder : SdkClientConfig.Builder<TConfig>,
          TConfigBuilder : AwsSdkClientConfig.Builder {

    /**
     * Service-specific default max attempts for the retry strategy.
     * Override in generated code for services that require non-standard defaults (e.g., DynamoDB uses 4).
     */
    protected open val defaultMaxAttempts: Int? = null

    /**
     * Service-specific default initial delay for the retry strategy.
     * Override in generated code for services that require non-standard defaults (e.g., DynamoDB uses 25ms).
     */
    protected open val defaultInitialDelay: Duration? = null

    /**
     * Construct a [TClient] by resolving the configuration from the current environment.
     */
    @OptIn(ExperimentalApi::class)
    public suspend fun fromEnvironment(block: (TConfigBuilder.() -> Unit)? = null): TClient {
        val builder = builder()
        val config = builder.config

        // FIXME - use a default telemetry provider that at least wires up SLF4j logging
        val telemetryProvider = (builder as? TelemetryConfig.Builder)?.telemetryProvider ?: TelemetryProvider.None
        val tracer = telemetryProvider.tracerProvider.getOrCreateTracer("AwsSdkClientFactory")

        tracer.withSpan("fromEnvironment") {
            val platform = PlatformProvider.System
            val sharedConfig = asyncLazy { loadAwsSharedConfig(platform) }
            val profile = asyncLazy { sharedConfig.get().activeProfile }

            // As a DslBuilderProperty, the value of retryStrategy cannot be checked for nullability because it may have
            // been set using a DSL. Thus, set the resolved strategy _first_ to ensure it's used as the fallback.
            if (config is RetryStrategyClientConfig.Builder) {
                config.retryStrategy = resolveRetryStrategy(profile = profile, defaultMaxAttempts = defaultMaxAttempts, defaultInitialDelay = defaultInitialDelay)
            }

            block?.let(config::apply)

            config.logMode = config.logMode ?: ClientSettings.LogMode.resolve(platform = platform)
            config.region = config.region ?: config.regionProvider?.getRegion() ?: resolveRegion(profile = profile)
            config.useFips = config.useFips ?: resolveUseFips(profile = profile)
            config.useDualStack = config.useDualStack ?: resolveUseDualStack(profile = profile)
            config.applicationId = config.applicationId ?: resolveUserAgentAppId(platform, profile)

            if (config is CompressionClientConfig.Builder) {
                config.requestCompression.disableRequestCompression =
                    config.requestCompression.disableRequestCompression
                        ?: resolveDisableRequestCompression(platform, profile)

                config.requestCompression.requestMinCompressionSizeBytes =
                    config.requestCompression.requestMinCompressionSizeBytes
                        ?: resolveRequestMinCompressionSizeBytes(platform, profile)
            }

            if (config is SigV4aClientConfig.Builder) {
                config.sigV4aSigningRegionSet =
                    config.sigV4aSigningRegionSet ?: resolveSigV4aSigningRegionSet(platform, profile)
            }

            if (config is HttpChecksumConfig.Builder) {
                config.requestChecksumCalculation =
                    config.requestChecksumCalculation ?: resolveRequestChecksumCalculation(platform, profile)

                config.responseChecksumValidation =
                    config.responseChecksumValidation ?: resolveResponseChecksumValidation(platform, profile)
            }

            if (config is HttpAuthConfig.Builder) {
                config.authSchemePreference = config.authSchemePreference ?: resolveAuthSchemePreference(platform, profile)
            }

            finalizeConfig(builder)
            finalizeEnvironmentalConfig(builder, sharedConfig, profile)
        }
        return builder.build()
    }

    /**
     * Inject any client-specific config.
     */
    protected open suspend fun finalizeEnvironmentalConfig(
        builder: TClientBuilder,
        sharedConfig: LazyAsyncValue<AwsSharedConfig>,
        activeProfile: LazyAsyncValue<AwsProfile>,
    ) {
    }
}
