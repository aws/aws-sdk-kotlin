/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.profile.SSO_REGION
import aws.sdk.kotlin.runtime.auth.credentials.profile.SSO_SESSION
import aws.sdk.kotlin.runtime.auth.credentials.profile.SSO_START_URL
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsSharedConfig
import aws.sdk.kotlin.runtime.config.profile.loadAwsSharedConfig
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.http.auth.BearerToken
import aws.smithy.kotlin.runtime.http.auth.BearerTokenProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy
import kotlin.coroutines.coroutineContext

/**
 * A [BearerTokenProvider] that gets tokens from a profile in `~/.aws/config`. The locations of these files are configurable
 * via environment or system property on the JVM (see [AwsSdkSetting.AwsConfigFile] and [AwsSdkSetting.AwsSharedCredentialsFile]).
 *
 * This provider is part of the [DefaultChainBearerTokenProvider] and usually consumed through that provider. However,
 * it can be instantiated and used standalone as well.
 *
 * NOTE: This provider does not implement any caching. It will reload and reparse the profile from the file system
 * when called.
 *
 * This provider currently only supports SSO tokens via [SsoTokenProvider]. It will look for a configured
 * `sso-session` on the active profile and use that to construct an [SsoTokenProvider].
 *
 * ### Token Provider via SSO
 * ```ini
 * [default]
 * sso_session = my-session
 *
 * [sso-session my-session]
 * sso_start_url = https://my-start-url
 * sso_region = us-east-1
 * ```
 *
 * See the [Configuration and credential file settings](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)
 * documentation provided by the AWS CLI.
 *
 * @param profileName Override the profile name to use. If not provided it will be resolved internally
 * via environment (see [AwsSdkSetting.AwsProfile]) or defaulted to `default` if not configured.
 * @param platformProvider The platform API provider
 * @param httpClient the [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and lifetime
 * are NOT managed by the provider. Caller is responsible for closing.
 * @param clock the source of time for the provider
 */
internal class ProfileBearerTokenProvider(
    private val profileName: String? = null,
    private val platformProvider: PlatformProvider = PlatformProvider.System,
    private val httpClient: HttpClientEngine? = null,
    private val clock: Clock = Clock.System,
) : BearerTokenProvider {
    // TODO - re-evaluate how often shared config is parsed and passed down through credential/token chains
    private val sharedConfig = asyncLazy { loadAwsSharedConfig(platformProvider, profileName) }

    // The shared config is loaded once, so the token provider it selects is created once and reused. The token
    // provider keeps state between resolutions, such as when it last attempted to refresh the token.
    private val tokenProvider = asyncLazy { sharedConfig.get().resolveTokenProviderOrThrow() }

    override suspend fun resolve(attributes: Attributes): BearerToken {
        val logger = coroutineContext.logger<ProfileBearerTokenProvider>()
        val config = sharedConfig.get()
        logger.debug { "Loading bearer token from profile `${config.activeProfile.name}`" }

        return tokenProvider.get().resolve(attributes)
    }

    private fun AwsSharedConfig.resolveTokenProviderOrThrow(): BearerTokenProvider {
        val sessionName = activeProfile.getOrNull(SSO_SESSION) ?: throw ProviderConfigurationException("no bearer token providers available for profile `${activeProfile.name}`")
        val session = ssoSessions[sessionName] ?: throw ProviderConfigurationException("profile (${activeProfile.name}) references non-existing sso_session = `$sessionName`")

        // if session is defined the profile MUST be resolved by the SSO token provider
        val startUrl = session.getOrNull(SSO_START_URL) ?: throw ProviderConfigurationException("sso-session ($sessionName) missing `$SSO_START_URL`")
        val ssoRegion = session.getOrNull(SSO_REGION) ?: throw ProviderConfigurationException("sso-session ($sessionName) missing `$SSO_REGION`")

        return SsoTokenProvider(
            sessionName,
            startUrl,
            ssoRegion,
            platformProvider = platformProvider,
            httpClient = httpClient,
            clock = clock,
        )
    }
}
