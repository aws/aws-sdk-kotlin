/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.imds

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.loadAwsSharedConfig
import aws.smithy.kotlin.runtime.client.endpoints.Endpoint
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.http.operation.EndpointResolver
import aws.smithy.kotlin.runtime.http.operation.ResolveEndpointRequest
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy

internal const val EC2_METADATA_SERVICE_ENDPOINT_PROFILE_KEY = "ec2_metadata_service_endpoint"
internal const val EC2_METADATA_SERVICE_ENDPOINT_MODE_PROFILE_KEY = "ec2_metadata_service_endpoint_mode"

internal class ImdsEndpointProvider(
    private val platformProvider: PlatformProvider,
    private val endpointConfiguration: EndpointConfiguration,
) : EndpointResolver {
    // cached endpoint and profile
    private val resolvedEndpoint = asyncLazy(::doResolveEndpoint)
    private val activeProfile = asyncLazy { loadAwsSharedConfig(platformProvider).activeProfile }

    override suspend fun resolve(request: ResolveEndpointRequest): Endpoint = resolveImdsEndpoint()
    internal suspend fun resolveImdsEndpoint(): Endpoint = resolvedEndpoint.get()

    private suspend fun doResolveEndpoint(): Endpoint = when (endpointConfiguration) {
        is EndpointConfiguration.Custom -> endpointConfiguration.endpoint
        else -> resolveEndpointFromConfig()
    }

    private suspend fun resolveEndpointFromConfig(): Endpoint {
        // explicit endpoint configured
        val endpoint = loadEndpointFromEnv() ?: loadEndpointFromProfile()
        if (endpoint != null) return endpoint

        // endpoint default from mode
        val mode = when (endpointConfiguration) {
            is EndpointConfiguration.ModeOverride -> endpointConfiguration.mode
            else -> loadEndpointModeFromEnv() ?: loadEndpointModeFromProfile() ?: EndpointMode.IPv4
        }
        return mode.defaultEndpoint
    }

    private fun loadEndpointFromEnv(): Endpoint? = AwsSdkSetting.AwsEc2MetadataServiceEndpoint.resolve(platformProvider)?.toEndpoint()

    private suspend fun loadEndpointFromProfile(): Endpoint? {
        val profile = activeProfile.get()
        return profile.getOrNull(EC2_METADATA_SERVICE_ENDPOINT_PROFILE_KEY)?.toEndpoint()
    }

    private fun loadEndpointModeFromEnv(): EndpointMode? = AwsSdkSetting.AwsEc2MetadataServiceEndpointMode.resolve(platformProvider)?.let { EndpointMode.fromValue(it) }

    private suspend fun loadEndpointModeFromProfile(): EndpointMode? {
        val profile = activeProfile.get()
        return profile.getOrNull(EC2_METADATA_SERVICE_ENDPOINT_MODE_PROFILE_KEY)?.let { EndpointMode.fromValue(it) }
    }
}

// Parse a string as a URL and convert to an endpoint
internal fun String.toEndpoint(): Endpoint = Endpoint(this)
