/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials.profile

import aws.sdk.kotlin.runtime.auth.credentials.ProviderConfigurationException
import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.runtime.auth.credentials.profile.LeafProviderResult.Err
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.AwsSharedConfig

/**
 * A chain of profile providers
 *
 * Within a profile file, a chain of providers is produced. Starting with a leaf provider,
 * subsequent providers use the credentials form previous providers to perform their task.
 *
 * A chain is a direct representation of the profile file, it can contain [LeafProvider.NamedSource]
 * providers that don't actually have implementations.
 */
internal data class ProfileChain(
    /**
     * The credentials provider that starts the assume role chain.
     */
    val leaf: LeafProvider,

    /**
     * The list of roles to assume (in-order). The first role will be assumed with credentials from [leaf].
     * Every role after that should be assumed with the prior role credentials.
     */
    val roles: List<RoleArn>,
) {
    companion object {
        /**
         * Resolves profile chain with the following precedence:
         *
         * 1. Static credentials
         * 2. Assume role with source profile OR assume role with named provider (mutually exclusive)
         * 3. Web ID token file & role arn
         * 4. SSO session
         * 5. Legacy SSO
         * 6. Process
         */
        internal fun resolve(config: AwsSharedConfig): ProfileChain {
            val visited = mutableSetOf<String>()
            val chain = mutableListOf<RoleArn>()
            var sourceProfileName = config.activeProfile.name
            var leaf: LeafProvider?

            loop@while (true) {
                val profile = config.profiles.getOrThrow(sourceProfileName) {
                    if (visited.isEmpty()) {
                        "could not find source profile $sourceProfileName"
                    } else {
                        "could not find source profile $sourceProfileName referenced from ${visited.last()}"
                    }
                }

                if (!visited.add(sourceProfileName)) {
                    // we're in a loop, break out
                    throw ProviderConfigurationException("profile formed an infinite loop: ${visited.joinToString(separator = " -> ")} -> $sourceProfileName")
                }

                // static credentials have the highest precedence
                leaf = profile.staticCredsOrNull()
                if (leaf != null) break@loop

                // the existence of `role_arn` is the only signal that multiple profiles will be chained
                val roleArn = profile.roleArnOrNull()
                if (roleArn != null) {
                    chain.add(roleArn)
                } else {
                    // have to find a leaf provider or error
                    leaf = profile.leafProvider(config)
                    break@loop
                }

                // a profile is allowed to use itself as a source profile for static credentials
                when (val nextProfile = profile.chainProvider()) {
                    is NextProfile.SelfReference -> {
                        // self-referential profile, attempt to load as a leaf provider (credential source)
                        leaf = profile.leafProvider(config)
                        break@loop
                    }
                    is NextProfile.Named -> sourceProfileName = nextProfile.name
                }
            }

            chain.reverse()
            return ProfileChain(checkNotNull(leaf), chain)
        }
    }
}

private inline fun Map<String, AwsProfile>.getOrThrow(name: String, lazyMessage: () -> String): AwsProfile =
    get(name) ?: throw ProviderConfigurationException(lazyMessage())

/**
 * A profile that specifies a role to assume
 *
 * A RoleArn can only be created from either a profile with `source_profile` or
 * one with `credential_source`
 */
internal data class RoleArn(
    /**
     * ARN of role to assume
     */
    val roleArn: String,

    /**
     * The source used to create the [RoleArn]
     */
    val source: RoleArnSource? = null,

    /**
     * Session name to pass to the assume role provider
     */
    val sessionName: String? = null,

    /**
     * External ID to pass to the assume role provider
     */
    val externalId: String? = null,
)

/**
 * Represents the possible sources for creating a [RoleArn].
 */
internal enum class RoleArnSource {
    SOURCE_PROFILE,
    CREDENTIALS_SOURCE,
}

internal const val ROLE_ARN = "role_arn"
internal const val EXTERNAL_ID = "external_id"
internal const val ROLE_SESSION_NAME = "role_session_name"
internal const val CREDENTIAL_SOURCE = "credential_source"
internal const val SOURCE_PROFILE = "source_profile"

internal const val WEB_IDENTITY_TOKEN_FILE = "web_identity_token_file"
internal const val AWS_ACCESS_KEY_ID = "aws_access_key_id"
internal const val AWS_SECRET_ACCESS_KEY = "aws_secret_access_key"
internal const val AWS_SESSION_TOKEN = "aws_session_token"
internal const val AWS_ACCOUNT_ID = "aws_account_id"

internal const val SSO_START_URL = "sso_start_url"
internal const val SSO_REGION = "sso_region"
internal const val SSO_ACCOUNT_ID = "sso_account_id"
internal const val SSO_ROLE_NAME = "sso_role_name"
internal const val SSO_SESSION = "sso_session"

internal const val LOGIN_SESSION = "login_session"

internal const val CREDENTIAL_PROCESS = "credential_process"

private fun AwsProfile.roleArnOrNull(): RoleArn? {
    val validSource = contains(CREDENTIAL_SOURCE) || contains(SOURCE_PROFILE)

    // chained roles have higher precedence than web id token file
    // web identity tokens are leaf providers, not chained roles
    if (!validSource && contains(WEB_IDENTITY_TOKEN_FILE)) return null

    val roleArn = getOrNull(ROLE_ARN) ?: return null

    val roleArnSource = when {
        contains(SOURCE_PROFILE) && !contains(CREDENTIAL_SOURCE) -> RoleArnSource.SOURCE_PROFILE
        contains(CREDENTIAL_SOURCE) && !contains(SOURCE_PROFILE) -> RoleArnSource.CREDENTIALS_SOURCE
        else -> null
    }

    return RoleArn(
        roleArn,
        roleArnSource,
        sessionName = getOrNull(ROLE_SESSION_NAME),
        externalId = getOrNull(EXTERNAL_ID),
    )
}

private sealed class LeafProviderResult {
    /**
     * Success, provider found and configured
     */
    data class Ok(val provider: LeafProvider) : LeafProviderResult()

    /**
     * Provider was found but had missing or invalid configuration
     */
    data class Err(val errorMessage: String) : LeafProviderResult()
}

/**
 * Unwrap the result or throw an exception if the result is [Err]
 */
private fun LeafProviderResult.unwrap(): LeafProvider = when (this) {
    is LeafProviderResult.Ok -> provider
    is Err -> throw ProviderConfigurationException(errorMessage)
}

/**
 * Returns the current result if not null or computes it by invoking [fn]
 */
private inline fun LeafProviderResult?.unwrapOrElse(fn: () -> LeafProviderResult): LeafProviderResult = when (this) {
    null -> fn()
    else -> this
}

/**
 * Return current result if not null, otherwise use the result from calling [fn]
 */
private inline fun LeafProviderResult?.orElse(fn: () -> LeafProviderResult?): LeafProviderResult? = when (this) {
    null -> fn()
    else -> this
}

/**
 * Attempt to load [LeafProvider.WebIdentityTokenRole] from the current profile or `null` if the profile
 * does not contain a web identity token provider
 */
private fun AwsProfile.webIdentityTokenCreds(): LeafProviderResult? {
    val roleArn = getOrNull(ROLE_ARN)
    val tokenFile = getOrNull(WEB_IDENTITY_TOKEN_FILE)
    val sessionName = getOrNull(ROLE_SESSION_NAME)
    return when {
        tokenFile == null -> null
        roleArn == null -> LeafProviderResult.Err("profile ($name) missing `$ROLE_ARN`")
        else -> LeafProviderResult.Ok(LeafProvider.WebIdentityTokenRole(roleArn, tokenFile, sessionName))
    }
}

/**
 * Attempt to load [LeafProvider.LegacySso] from the current profile or `null` if the current profile does not contain
 * a legacy SSO provider
 */
private fun AwsProfile.legacySsoCreds(): LeafProviderResult? {
    if (!contains(SSO_ACCOUNT_ID) && !contains(SSO_ROLE_NAME)) return null

    // if one or more of the above configuration values is present the profile MUST be resolved by the SSO credential provider.
    val startUrl = getOrNull(SSO_START_URL) ?: return LeafProviderResult.Err("profile ($name) missing `$SSO_START_URL`")
    val ssoRegion = getOrNull(SSO_REGION) ?: return LeafProviderResult.Err("profile ($name) missing `$SSO_REGION`")
    val accountId = getOrNull(SSO_ACCOUNT_ID) ?: return LeafProviderResult.Err("profile ($name) missing `$SSO_ACCOUNT_ID`")
    val roleName = getOrNull(SSO_ROLE_NAME) ?: return LeafProviderResult.Err("profile ($name) missing `$SSO_ROLE_NAME`")

    return LeafProviderResult.Ok(LeafProvider.LegacySso(startUrl, ssoRegion, accountId, roleName))
}

private fun AwsProfile.ssoSessionCreds(config: AwsSharedConfig): LeafProviderResult? {
    val sessionName = getOrNull(SSO_SESSION) ?: return null
    val session = config.ssoSessions[sessionName] ?: return LeafProviderResult.Err("profile ($name) references non-existing sso_session = `$sessionName`")

    // if session is defined the profile MUST be resolved by the SSO credential provider
    val startUrl = session.getOrNull(SSO_START_URL) ?: return LeafProviderResult.Err("sso-session ($sessionName) missing `$SSO_START_URL`")
    val ssoRegion = session.getOrNull(SSO_REGION) ?: return LeafProviderResult.Err("sso-session ($sessionName) missing `$SSO_REGION`")
    val accountId = getOrNull(SSO_ACCOUNT_ID) ?: return LeafProviderResult.Err("profile ($name) missing `$SSO_ACCOUNT_ID`")
    val roleName = getOrNull(SSO_ROLE_NAME) ?: return LeafProviderResult.Err("profile ($name) missing `$SSO_ROLE_NAME`")

    val sessionSsoRegion = session.getOrNull(SSO_REGION)
    val profileSsoRegion = getOrNull(SSO_REGION)
    if (sessionSsoRegion != null && profileSsoRegion != null && sessionSsoRegion != profileSsoRegion) {
        return LeafProviderResult.Err("sso-session ($sessionName) $SSO_REGION = `$sessionSsoRegion` does not match profile ($name) $SSO_REGION = `$profileSsoRegion`")
    }

    val sessionStartUrl = session.getOrNull(SSO_START_URL)
    val profileStartUrl = getOrNull(SSO_START_URL)
    if (sessionStartUrl != null && profileStartUrl != null && sessionStartUrl != profileStartUrl) {
        return LeafProviderResult.Err("sso-session ($sessionName) $SSO_START_URL = `$sessionStartUrl` does not match profile ($name) $SSO_START_URL = `$profileStartUrl`")
    }

    return LeafProviderResult.Ok(LeafProvider.SsoSession(sessionName, startUrl, ssoRegion, accountId, roleName))
}

/**
 * Attempt to load [LeafProvider.LoginSession] from the current profile or `null` if the profile
 * does not contain a login session configuration.
 */
private fun AwsProfile.loginSessionCreds(): LeafProviderResult? {
    val sessionName = getOrNull(LOGIN_SESSION) ?: return null
    return LeafProviderResult.Ok(LeafProvider.LoginSession(sessionName))
}

/**
 * Attempt to load [LeafProvider.Process] from the current profile or exception if the current profile does not contain
 * a credentials process command to execute
 */
private fun AwsProfile.processCreds(): LeafProviderResult {
    // Process is last in precedence - credentials not found means no credentials in profile
    if (!contains(CREDENTIAL_PROCESS)) return LeafProviderResult.Err("profile ($name) did not contain credential information")

    val credentialProcess = getOrNull(CREDENTIAL_PROCESS) ?: return LeafProviderResult.Err("profile ($name) missing `$CREDENTIAL_PROCESS`")

    return LeafProviderResult.Ok(LeafProvider.Process(credentialProcess))
}

/**
 * Load [LeafProvider.AccessKey] from the current profile or throw an exception if the profile does not contain
 * credentials
 */
private fun AwsProfile.staticCreds(): LeafProviderResult {
    val accessKeyId = getOrNull(AWS_ACCESS_KEY_ID)
    val secretKey = getOrNull(AWS_SECRET_ACCESS_KEY)
    val accountId = getOrNull(AWS_ACCOUNT_ID)
    return when {
        accessKeyId == null && secretKey == null -> LeafProviderResult.Err("profile ($name) missing `aws_access_key_id` & `aws_secret_access_key`")
        accessKeyId == null -> LeafProviderResult.Err("profile ($name) missing `aws_access_key_id`")
        secretKey == null -> LeafProviderResult.Err("profile ($name) missing `aws_secret_access_key`")
        else -> {
            val sessionToken = getOrNull(AWS_SESSION_TOKEN)
            val provider = LeafProvider.AccessKey(credentials(accessKeyId, secretKey, sessionToken, accountId = accountId))
            LeafProviderResult.Ok(provider)
        }
    }
}

/**
 * Attempt to load [LeafProvider.AccessKey] from the current profile or `null` if the current profile does not contain
 * credentials
 */
private fun AwsProfile.staticCredsOrNull(): LeafProvider? = when (val result = staticCreds()) {
    is LeafProviderResult.Ok -> result.provider
    else -> null
}

private sealed class NextProfile {
    object SelfReference : NextProfile()
    data class Named(val name: String) : NextProfile()
}

/**
 * Get the next profile name in the chain or the current profile if it specifies an explicit credential source
 */
private fun AwsProfile.chainProvider(): NextProfile {
    val sourceProfile = getOrNull(SOURCE_PROFILE)
    val credSource = getOrNull(CREDENTIAL_SOURCE)

    return when {
        sourceProfile != null && credSource != null -> throw ProviderConfigurationException("profile ($name) contained both `source_profile` and `credential_source`. Only one or the other can be defined.")
        sourceProfile == null && credSource == null -> throw ProviderConfigurationException("profile ($name) must contain `source_profile` or `credential_source` but neither were defined")
        sourceProfile != null && credSource == null -> if (sourceProfile == name) {
            NextProfile.SelfReference
        } else {
            NextProfile.Named(sourceProfile)
        }
        // loop back into this profile and pick up the credential source
        else -> NextProfile.SelfReference
    }
}

/**
 * Get a terminal leaf provider for the current profile or throw an exception
 */
private fun AwsProfile.leafProvider(config: AwsSharedConfig): LeafProvider {
    // profile must define either `credential_source` or explicit access keys
    val credSource = getOrNull(CREDENTIAL_SOURCE)
    if (credSource != null) return LeafProvider.NamedSource(credSource)

    // we want to stop on errors in earlier providers to get the right exception message, thus we take the first
    // non-null LeafProviderResult we encounter
    return webIdentityTokenCreds()
        .orElse { ssoSessionCreds(config) }
        .orElse(::legacySsoCreds)
        .orElse { loginSessionCreds() }
        .unwrapOrElse(::processCreds)
        .unwrap()
}
