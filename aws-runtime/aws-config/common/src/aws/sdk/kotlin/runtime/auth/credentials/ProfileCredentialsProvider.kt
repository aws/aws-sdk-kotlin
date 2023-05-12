/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.profile.LeafProvider
import aws.sdk.kotlin.runtime.auth.credentials.profile.ProfileChain
import aws.sdk.kotlin.runtime.auth.credentials.profile.RoleArn
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.imds.ImdsClient
import aws.sdk.kotlin.runtime.config.profile.loadAwsSharedConfig
import aws.sdk.kotlin.runtime.region.resolveRegion
import aws.smithy.kotlin.runtime.auth.awscredentials.CloseableCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.http.operation.getLogger
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.util.Attributes
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
 */
public class ProfileCredentialsProvider(
    private val profileName: String? = null,
    private val region: String? = null,
    private val platformProvider: PlatformProvider = PlatformProvider.System,
    private val httpClient: HttpClientEngine? = null,
) : CloseableCredentialsProvider {
    private val namedProviders = mapOf(
        "Environment" to EnvironmentCredentialsProvider(platformProvider::getenv),
        "Ec2InstanceMetadata" to ImdsCredentialsProvider(
            profileOverride = profileName,
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
        val logger = coroutineContext.getLogger<ProfileCredentialsProvider>()
        val sharedConfig = loadAwsSharedConfig(platformProvider, profileName)
        logger.debug { "Loading credentials from profile `${sharedConfig.activeProfile.name}`" }
        val chain = ProfileChain.resolve(sharedConfig)

        // if profile is overridden for this provider, attempt to resolve it from there first
        val profileOverride = profileName?.let { sharedConfig.profiles[it] }
        val region = asyncLazy { region ?: profileOverride?.getOrNull("region") ?: resolveRegion(platformProvider) }

        val leaf = chain.leaf.toCredentialsProvider(region)
        logger.debug { "Resolving credentials from ${chain.leaf.description()}" }
        var creds = leaf.resolve(attributes)

        chain.roles.forEach { roleArn ->
            logger.debug { "Assuming role `${roleArn.roleArn}`" }
            val assumeProvider = roleArn.toCredentialsProvider(creds, region)
            creds = assumeProvider.resolve(attributes)
        }

        logger.debug { "Obtained credentials from profile; expiration=${creds.expiration?.format(TimestampFormat.ISO_8601)}" }
        return creds
    }

    override fun close() {
        namedProviders.forEach { entry ->
            (entry.value as? Closeable)?.close()
        }
    }

    private suspend fun LeafProvider.toCredentialsProvider(region: LazyAsyncValue<String>): CredentialsProvider =
        when (this) {
            is LeafProvider.NamedSource -> namedProviders[name]
                ?: throw ProviderConfigurationException("unknown credentials source: $name")

            is LeafProvider.AccessKey -> StaticCredentialsProvider(credentials)

            is LeafProvider.WebIdentityTokenRole -> StsWebIdentityCredentialsProvider(
                roleArn,
                webIdentityTokenFile,
                region = region.get(),
                roleSessionName = sessionName,
                platformProvider = platformProvider,
                httpClient = httpClient,
            )

            is LeafProvider.SsoSession -> SsoCredentialsProvider(
                accountId = ssoAccountId,
                roleName = ssoRoleName,
                startUrl = ssoStartUrl,
                ssoRegion = ssoRegion,
                ssoSessionName = ssoSessionName,
                httpClient = httpClient,
                platformProvider = platformProvider,
            )

            is LeafProvider.LegacySso -> SsoCredentialsProvider(
                accountId = ssoAccountId,
                roleName = ssoRoleName,
                startUrl = ssoStartUrl,
                ssoRegion = ssoRegion,
                httpClient = httpClient,
                platformProvider = platformProvider,
            )

            is LeafProvider.Process -> ProcessCredentialsProvider(command)
        }

    private suspend fun RoleArn.toCredentialsProvider(
        creds: Credentials,
        region: LazyAsyncValue<String>,
    ): CredentialsProvider = StsAssumeRoleCredentialsProvider(
        credentialsProvider = StaticCredentialsProvider(creds),
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
}
