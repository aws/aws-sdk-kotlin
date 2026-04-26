/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.auth.credentials.CachedAuthFilePermissions
import aws.sdk.kotlin.runtime.config.endpoints.AccountIdEndpointMode
import aws.smithy.kotlin.runtime.client.config.RequestHttpChecksumConfig
import aws.smithy.kotlin.runtime.client.config.ResponseHttpChecksumConfig
import aws.smithy.kotlin.runtime.client.config.RetryMode
import aws.smithy.kotlin.runtime.http.auth.AuthScheme
import aws.smithy.kotlin.runtime.net.url.Url

/**
 * Represents an AWS config profile.
 */
@InternalSdkApi
public typealias AwsProfile = ConfigSection

// Standard cross-SDK properties

/**
 * The AWS signing and endpoint region to use for a profile
 */
@InternalSdkApi
public val AwsProfile.region: String?
    get() = getOrNull("region")

/**
 * The identifier that specifies the entity making the request for a profile
 */
@InternalSdkApi
public val AwsProfile.awsAccessKeyId: String?
    get() = getOrNull("aws_access_key_id")

/**
 * The credentials that authenticate the entity specified by the access key
 */
@InternalSdkApi
public val AwsProfile.awsSecretAccessKey: String?
    get() = getOrNull("aws_secret_access_key")

/**
 * A semi-temporary session token that authenticates the entity is allowed to access a specific set of resources
 */
@InternalSdkApi
public val AwsProfile.awsSessionToken: String?
    get() = getOrNull("aws_session_token")

/**
 * A role that the user must automatically assume, giving it semi-temporary access to a specific set of resources
 */
@InternalSdkApi
public val AwsProfile.roleArn: String?
    get() = getOrNull("role_arn")

/**
 * Specifies which profile must be used to automatically assume the role specified by role_arn
 */
@InternalSdkApi
public val AwsProfile.sourceProfile: String?
    get() = getOrNull("source_profile")

/**
 * The maximum number of request attempts to perform. This is one more than the number of retries, so
 * aws.maxAttempts = 1 will have 0 retries.
 */
@InternalSdkApi
public val AwsProfile.maxAttempts: Int?
    get() = getIntOrNull("max_attempts")

/**
 * The command which the SDK will invoke to retrieve credentials
 */
@InternalSdkApi
public val AwsProfile.credentialProcess: String?
    get() = getOrNull("credential_process")

/**
 * Which [RetryMode] to use for the default RetryPolicy, when one is not specified at the client level.
 */
@InternalSdkApi
public val AwsProfile.retryMode: RetryMode?
    get() = getEnumOrNull<RetryMode>("retry_mode")

/**
 * Whether service clients should make requests to the FIPS endpoint variant.
 */
@InternalSdkApi
public val AwsProfile.useFips: Boolean?
    get() = getBooleanOrNull("use_fips_endpoint")

/**
 * Whether service clients should make requests to the dual-stack endpoint variant.
 */
@InternalSdkApi
public val AwsProfile.useDualStack: Boolean?
    get() = getBooleanOrNull("use_dualstack_endpoint")

/**
 * The default endpoint URL that applies to all services.
 */
@InternalSdkApi
public val AwsProfile.endpointUrl: Url?
    get() = getUrlOrNull("endpoint_url")

/**
 * Whether to ignore configured endpoint URLs.
 */
@InternalSdkApi
public val AwsProfile.ignoreEndpointUrls: Boolean?
    get() = getBooleanOrNull("ignore_configured_endpoint_urls")

/**
 * The name of the services config section used by this profile.
 */
@InternalSdkApi
public val AwsProfile.servicesSection: String?
    get() = getOrNull("services")

/**
 * The SDK user agent app ID used to identify applications.
 */
@InternalSdkApi
public val AwsProfile.sdkUserAgentAppId: String?
    get() = getOrNull("sdk_ua_app_id")

/**
 * Whether service clients should make requests to the dual-stack endpoint variant.
 */
@InternalSdkApi
public val AwsProfile.accountIdEndpointMode: AccountIdEndpointMode?
    get() = getEnumOrNull<AccountIdEndpointMode>("account_id_endpoint_mode")

/**
 * Determines when a request should be compressed or not
 */
@InternalSdkApi
public val AwsProfile.disableRequestCompression: Boolean?
    get() = getBooleanOrNull("disable_request_compression")

/**
 * The threshold used to determine when a request should be compressed
 */
@InternalSdkApi
public val AwsProfile.requestMinCompressionSizeBytes: Long?
    get() = getLongOrNull("request_min_compression_size_bytes")

/**
 * The set of regions to use when signing a request with sigV4a.
 */
@InternalSdkApi
public val AwsProfile.sigV4aSigningRegionSet: String?
    get() = getOrNull("sigv4a_signing_region_set")

/**
 * A flag indicating whether endpoint discovery should be enabled for a service that supports it. This setting has no
 * effect for services which _do not_ support endpoint discovery.
 */
@InternalSdkApi
public val AwsProfile.endpointDiscoveryEnabled: Boolean?
    get() = getBooleanOrNull("endpoint_discovery_enabled")

/**
 * Configures request checksum calculation
 */
@InternalSdkApi
public val AwsProfile.requestChecksumCalculation: RequestHttpChecksumConfig?
    get() = getEnumOrNull<RequestHttpChecksumConfig>("request_checksum_calculation")

/**
 * Configures response checksum validation
 */
@InternalSdkApi
public val AwsProfile.responseChecksumValidation: ResponseHttpChecksumConfig?
    get() = getEnumOrNull<ResponseHttpChecksumConfig>("response_checksum_validation")

/**
 * The ordered preference of [AuthScheme] that this client will use
 */
@InternalSdkApi
public val AwsProfile.authSchemePreference: String?
    get() = getOrNull("auth_scheme_preference")

/**
 * The set of permissions to apply when writing cached auth files (e.g., during cache token refresh for SSO or AWS Login
 * credentials). This property has no effect on Windows.
 */
@InternalSdkApi
public val AwsProfile.cachedAuthFilePermissions: CachedAuthFilePermissions?
    get() = getEnumOrNull<CachedAuthFilePermissions>("cached_auth_file_permissions")

/**
 * Parse a config value as a boolean, ignoring case.
 */
@InternalSdkApi
public fun AwsProfile.getBooleanOrNull(key: String, subKey: String? = null): Boolean? = getOrNull(key, subKey)?.let {
    it.lowercase().toBooleanStrictOrNull() ?: throw ConfigurationException(
        "Failed to parse config property ${buildKeyString(key, subKey)} as a boolean",
    )
}

/**
 * Parse a config value as an int.
 */
@InternalSdkApi
public fun AwsProfile.getIntOrNull(key: String, subKey: String? = null): Int? = getOrNull(key, subKey)?.let {
    it.toIntOrNull() ?: throw ConfigurationException(
        "Failed to parse config property ${buildKeyString(key, subKey)} as an integer",
    )
}

/**
 * Parse a config value as a long.
 */
@InternalSdkApi
public fun AwsProfile.getLongOrNull(key: String, subKey: String? = null): Long? = getOrNull(key, subKey)?.let {
    it.toLongOrNull() ?: throw ConfigurationException(
        "Failed to parse config property ${buildKeyString(key, subKey)} as a long",
    )
}

/**
 * Parse a config value as an enum.
 */
@InternalSdkApi
public inline fun <reified T : Enum<T>> AwsProfile.getEnumOrNull(key: String, subKey: String? = null): T? = getOrNull(key, subKey)?.let { value ->
    enumValues<T>().firstOrNull {
        it.name.equals(value, ignoreCase = true)
    } ?: throw ConfigurationException(
        buildString {
            append(key)
            append(" '")
            append(value)
            append("' is not supported, should be one of: ")
            enumValues<T>().joinTo(this) { it.name.lowercase() }
        },
    )
}

internal fun AwsProfile.getUrlOrNull(key: String, subKey: String? = null): Url? = getOrNull(key, subKey)?.let {
    try {
        Url.parse(it)
    } catch (e: Exception) {
        throw ConfigurationException("Failed to parse config property ${buildKeyString(key, subKey)} as a URL", e)
    }
}

private fun buildKeyString(key: String, subKey: String? = null): String = listOfNotNull(key, subKey).joinToString(".")
