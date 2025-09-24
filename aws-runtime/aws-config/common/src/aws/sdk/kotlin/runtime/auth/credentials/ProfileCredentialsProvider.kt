/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.auth.credentials.profile.LeafProvider
import aws.sdk.kotlin.runtime.auth.credentials.profile.ProfileChain
import aws.sdk.kotlin.runtime.auth.credentials.profile.RoleArn
import aws.sdk.kotlin.runtime.auth.credentials.profile.RoleArnSource
import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.imds.ImdsClient
import aws.sdk.kotlin.runtime.config.profile.AwsConfigurationSource
import aws.sdk.kotlin.runtime.config.profile.loadAwsSharedConfig
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetrics
import aws.sdk.kotlin.runtime.region.resolveRegion
import aws.smithy.kotlin.runtime.auth.awscredentials.*
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetrics
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.io.closeIfCloseable
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy
import kotlin.coroutines.coroutineContext

/**
 * A [CredentialsProvider] that gets credentials from a profile in `~/.aws/config` or the shared credentials
 * file `~/.aws/credentials`. The locations of these files are configurable via environment or system property on
 * the JVM (see [AwsSdkSetting.AwsConfigFile] and [AwsSdkSetting.AwsSharedCredentialsFile]).
 *
 * This provider is part of the [DefaultChainCredentialsProvider] and usually consumed through that provider. However,
 * it can be instantiated and used standalone as well.
 *
 * NOTE: This provider does not implement any caching. It will reload and reparse the profile from the file system
 * when called. Use [CachedCredentialsProvider] to decorate the profile provider to get caching behavior.
 *
 * This provider supports several credentials formats:
 *
 * ### Credentials defined explicitly within the file
 * ```ini
 * [default]
 * aws_access_key_id = my-access-key
 * aws_secret_access_key = my-secret
 * ```
 *
 * ### Assumed role credentials loaded from a credential source
 * ```ini
 * [default]
 * role_arn = arn:aws:iam:123456789:role/RoleA
 * credential_source = Environment
 * ```
 *
 * ### Assumed role credentials from a source profile
 * ```ini
 * [default]
 * role_arn = arn:aws:iam:123456789:role/RoleA
 * source_profile = base
 *
 * [profile base]
 * aws_access_key_id = my-access-key
 * aws_secret_access_key = my-secret
 * ```
 *
 * Other more complex configurations are possible. See the [Configuration and credential file settings](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)
 * documentation provided by the AWS CLI.
 *
 * @param profileName Override the profile name to use. If not provided it will be resolved internally
 * via environment (see [AwsSdkSetting.AwsProfile]) or defaulted to `default` if not configured.
 * @param region The AWS region to use, this will be resolved internally if not provided.
 * @param platformProvider The platform API provider
 * @param httpClient the [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and lifetime
 * are NOT managed by the provider. Caller is responsible for closing.
 * @param configurationSource An optional configuration source to use for loading shared config. If not provided,
 * it will be resolved from the environment.
 */
public class ProfileCredentialsProvider @InternalSdkApi constructor(
    public val profileName: String? = null,
    public val region: String? = null,
    public val platformProvider: PlatformProvider = PlatformProvider.System,
    public val httpClient: HttpClientEngine? = null,
    public val configurationSource: AwsConfigurationSource? = null,
) : CloseableCredentialsProvider {
    private val credentialsBusinessMetrics: MutableSet<BusinessMetric> = mutableSetOf()

    public constructor(
        profileName: String? = null,
        region: String? = null,
        platformProvider: PlatformProvider = PlatformProvider.System,
        httpClient: HttpClientEngine? = null,
    ) : this (
        profileName,
        region,
        platformProvider,
        httpClient,
        null,
    )

    private val namedProviders = mapOf(
        "Environment" to EnvironmentCredentialsProvider(platformProvider::getenv),
        "Ec2InstanceMetadata" to ImdsCredentialsProvider(
            client = lazy {
                ImdsClient {
                    platformProvider = this@ProfileCredentialsProvider.platformProvider
                    engine = httpClient
                }
            },
            platformProvider = platformProvider,
        ),
        "EcsContainer" to EcsCredentialsProvider(platformProvider, httpClient),
    )

    override suspend fun resolve(attributes: Attributes): Credentials {
        val logger = coroutineContext.logger<ProfileCredentialsProvider>()
        val sharedConfig = loadAwsSharedConfig(platformProvider, profileName, configurationSource)
        logger.debug { "Loading credentials from profile `${sharedConfig.activeProfile.name}`" }
        val chain = ProfileChain.resolve(sharedConfig)

        // if profile is overridden for this provider, attempt to resolve it from there first
        val profileOverride = profileName?.let { sharedConfig.profiles[it] }
        val region = asyncLazy { region ?: profileOverride?.getOrNull("region") ?: attributes.getOrNull(AwsClientOption.Region) ?: resolveRegion(platformProvider) }

        val leaf = chain.leaf.toCredentialsProvider(region)
        logger.debug { "Resolving credentials from ${chain.leaf.description()}" }
        var creds = leaf.resolve(attributes)

        chain.roles.forEach { roleArn ->
            logger.debug { "Assuming role `${roleArn.roleArn}`" }
            if (roleArn.source == RoleArnSource.SOURCE_PROFILE) {
                credentialsBusinessMetrics.add(AwsBusinessMetric.Credentials.CREDENTIALS_PROFILE_SOURCE_PROFILE)
            }

            val assumeProvider = roleArn.toCredentialsProvider(creds, region)

            creds.attributes.getOrNull(BusinessMetrics)?.forEach { metric ->
                credentialsBusinessMetrics.add(metric)
            }

            creds = assumeProvider.resolve(attributes)
        }

        logger.debug { "Obtained credentials from profile; expiration=${creds.expiration?.format(TimestampFormat.ISO_8601)}" }
        return creds.withBusinessMetrics(credentialsBusinessMetrics)
    }

    override fun close() {
        namedProviders.forEach { entry ->
            entry.value.closeIfCloseable()
        }
    }

    private suspend fun LeafProvider.toCredentialsProvider(region: LazyAsyncValue<String?>): CredentialsProvider =
        when (this) {
            is LeafProvider.NamedSource -> namedProviders[name].also {
                credentialsBusinessMetrics.add(AwsBusinessMetric.Credentials.CREDENTIALS_PROFILE_NAMED_PROVIDER)
            } ?: throw ProviderConfigurationException("unknown credentials source: $name")

            is LeafProvider.AccessKey -> StaticCredentialsProvider(credentials).also {
                credentialsBusinessMetrics.add(AwsBusinessMetric.Credentials.CREDENTIALS_PROFILE)
            }

            is LeafProvider.WebIdentityTokenRole -> StsWebIdentityCredentialsProvider(
                roleArn,
                webIdentityTokenFile,
                region = region.get(),
                roleSessionName = sessionName,
                platformProvider = platformProvider,
                httpClient = httpClient,
            ).also {
                credentialsBusinessMetrics.add(AwsBusinessMetric.Credentials.CREDENTIALS_PROFILE_STS_WEB_ID_TOKEN)
            }

            is LeafProvider.SsoSession -> SsoCredentialsProvider(
                accountId = ssoAccountId,
                roleName = ssoRoleName,
                startUrl = ssoStartUrl,
                ssoRegion = ssoRegion,
                ssoSessionName = ssoSessionName,
                httpClient = httpClient,
                platformProvider = platformProvider,
            ).also {
                credentialsBusinessMetrics.add(AwsBusinessMetric.Credentials.CREDENTIALS_PROFILE_SSO)
            }

            is LeafProvider.LegacySso -> SsoCredentialsProvider(
                accountId = ssoAccountId,
                roleName = ssoRoleName,
                startUrl = ssoStartUrl,
                ssoRegion = ssoRegion,
                httpClient = httpClient,
                platformProvider = platformProvider,
            ).also {
                credentialsBusinessMetrics.add(AwsBusinessMetric.Credentials.CREDENTIALS_PROFILE_SSO_LEGACY)
            }

            is LeafProvider.Process -> ProcessCredentialsProvider(command).also {
                credentialsBusinessMetrics.add(AwsBusinessMetric.Credentials.CREDENTIALS_PROFILE_PROCESS)
            }
        }

    private suspend fun RoleArn.toCredentialsProvider(
        creds: Credentials,
        region: LazyAsyncValue<String?>,
    ): CredentialsProvider = StsAssumeRoleCredentialsProvider(
        bootstrapCredentialsProvider = StaticCredentialsProvider(creds),
        roleArn = roleArn,
        region = region.get(),
        roleSessionName = sessionName,
        externalId = externalId,
        httpClient = httpClient,
    )

    private fun LeafProvider.description(): String = when (this) {
        is LeafProvider.NamedSource -> "named source $name"
        is LeafProvider.AccessKey -> "static credentials"
        is LeafProvider.WebIdentityTokenRole -> "web identity token"
        is LeafProvider.SsoSession -> "single sign-on (session)"
        is LeafProvider.LegacySso -> "single sign-on (legacy)"
        is LeafProvider.Process -> "process"
    }

    override fun toString(): String = this.simpleClassName
}
