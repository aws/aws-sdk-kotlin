/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.net.url.Url

/**
 * Represents shared configuration (profiles, SSO sessions, credentials, etc)
 */
@InternalSdkApi
public class AwsSharedConfig internal constructor(
    private val sections: TypedSectionMap,
    private val source: AwsConfigurationSource,
) {

    /**
     * Map of profile names to profile.
     */
    public val profiles: Map<String, AwsProfile>
        get() = sections[ConfigSectionType.PROFILE] ?: emptyMap()

    /**
     * Map of SSO session name to SsoSessionConfig
     */
    public val ssoSessions: Map<String, SsoSessionConfig>
        get() = sections[ConfigSectionType.SSO_SESSION] ?: emptyMap()

    /**
     * Map of section name to ServiceConfig
     */
    public val services: Map<String, ServicesConfig>
        get() = sections[ConfigSectionType.SERVICES] ?: emptyMap()

    /**
     * Resolve the active profile or the default profile if none is defined
     */
    public val activeProfile: AwsProfile
        get() = profiles[source.profile] ?: AwsProfile(source.profile, emptyMap())
}

/**
 * Get the configured endpoint URL for a specific service, falling back to the global default.
 * @param serviceKey The config key for the service, generally this is sdkId in snake_case form.
 */
public fun AwsSharedConfig.resolveEndpointUrl(serviceKey: String): Url? = resolveServiceEndpointUrl(serviceKey) ?: activeProfile.endpointUrl

private fun AwsSharedConfig.resolveServiceEndpointUrl(serviceKey: String): Url? = activeProfile.servicesSection?.let { sectionName ->
    val section = services[sectionName]
        ?: throw ConfigurationException("shared config points to nonexistent services section '$sectionName'")

    section.getUrlOrNull(serviceKey, "endpoint_url")
}
