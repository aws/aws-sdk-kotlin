/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.runtime.config.imds.*
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.*
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.io.IOException
import aws.smithy.kotlin.runtime.serde.json.JsonDeserializer
import aws.smithy.kotlin.runtime.telemetry.logging.info
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.PlatformEnvironProvider
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.SingleFlightGroup
import aws.smithy.kotlin.runtime.util.asyncLazy
import kotlin.coroutines.coroutineContext

private const val CODE_ASSUME_ROLE_UNAUTHORIZED_ACCESS: String = "AssumeRoleUnauthorizedAccess"
private const val PROVIDER_NAME = "IMDSv2"

/**
 * [CredentialsProvider] that uses EC2 instance metadata service (IMDS) to provide credentials information.
 * This provider requires that the EC2 instance has an
 * [instance profile](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/iam-roles-for-amazon-ec2.html#ec2-instance-profile)
 * configured.
 * @param instanceProfileName overrides the instance profile name. When set, this provider skips querying IMDS for the
 * name of the active profile.
 * @param client a preconfigured IMDS client with which to retrieve instance metadata. If an instance is passed, the
 * caller is responsible for closing it. If no instance is passed, a default instance is created and will be closed when
 * this credentials provider is closed.
 * @param platformProvider a platform provider used for env vars and system properties
 */
public class ImdsCredentialsProvider(
    instanceProfileName: String? = null,
    client: InstanceMetadataProvider? = null,
    platformProvider: PlatformProvider = PlatformProvider.System,
) : CloseableCredentialsProvider {

    @Deprecated("This constructor supports parameters which are no longer used in the implementation. It will be removed in version 1.5.")
    public constructor(
        profileOverride: String? = null,
        client: Lazy<InstanceMetadataProvider> = lazy { ImdsClient() },
        platformProvider: PlatformEnvironProvider = PlatformProvider.System,
        @Suppress("UNUSED_PARAMETER") clock: Clock = Clock.System,
    ) : this(
        profileOverride,
        client.value,
        platformProvider = platformProvider as? PlatformProvider ?: PlatformProvider.System,
    )

    private val actualPlatformProvider = platformProvider

    @Deprecated("This property is retained for backwards compatibility but no longer needs to be public and will be removed in version 1.5.")
    public val platformProvider: PlatformEnvironProvider = actualPlatformProvider

    private val manageClient: Boolean = client == null

    private val actualClient = client ?: ImdsClient {
        this.platformProvider = actualPlatformProvider
    }

    @Deprecated("This property is retained for backwards compatibility but no longer needs to be public and will be removed in version 1.5.")
    public val client: Lazy<InstanceMetadataProvider>
        get() = lazyOf(actualClient)

    private val instanceProfileName = asyncLazy {
        instanceProfileName ?: resolveEc2InstanceProfileName(platformProvider)
    }

    @Deprecated("This property is retained for backwards compatibility but no longer needs to be public and will be removed in version 1.5.")
    public val profileOverride: String? = instanceProfileName

    private val providerDisabled = asyncLazy {
        resolveDisableEc2Metadata(platformProvider) ?: false
    }

    /**
     * Tracks the known-good version of IMDS APIs available in the local environment. This starts as `null` and will be
     * updated after the first successful API call.
     */
    private var apiVersion: ApiVersion? = null

    private val urlBase: String
        get() = (apiVersion ?: ApiVersion.EXTENDED).urlBase

    private var previousCredentials: Credentials? = null

    /**
     * Tracks the instance profile name resolved from IMDS. This starts as `null` and will be updated after a
     * successful API call. Note that if [instanceProfileName] is set, profile name resolution  will be skipped.
     */
    private var resolvedProfileName: String? = null

    /**
     * A deduplicator for resolving credentials and tracking mutable state about IMDS
     */
    private val sfg = SingleFlightGroup<Credentials>()

    override suspend fun resolve(attributes: Attributes): Credentials = sfg.singleFlight(::resolveSingleFlight)

    private suspend fun resolveSingleFlight(): Credentials {
        if (providerDisabled.get()) {
            throw CredentialsNotLoadedException("AWS EC2 metadata is explicitly disabled; credentials not loaded")
        }

        val profileName = instanceProfileName.get() ?: resolvedProfileName ?: try {
            actualClient.get(urlBase).also {
                if (apiVersion == null) {
                    // Tried EXTENDED and it worked; remember that for the future
                    apiVersion = ApiVersion.EXTENDED
                }
            }
        } catch (ex: EC2MetadataError) {
            when {
                apiVersion == null && ex.status == HttpStatusCode.NotFound -> {
                    // Tried EXTENDED and that didn't work; fallback to LEGACY
                    apiVersion = ApiVersion.LEGACY
                    return resolveSingleFlight()
                }

                ex.status == HttpStatusCode.NotFound -> {
                    coroutineContext.info<ImdsCredentialsProvider> {
                        "Received 404 when loading profile name. This instance may not have an associated profile."
                    }
                    throw ex
                }

                else -> return usePreviousCredentials()
                    ?: throw ImdsProfileException(ex).wrapAsCredentialsProviderException()
            }
        } catch (ex: IOException) {
            return usePreviousCredentials() ?: throw ImdsProfileException(ex).wrapAsCredentialsProviderException()
        } catch (ex: Exception) {
            throw ImdsProfileException(ex).wrapAsCredentialsProviderException()
        }

        val credsPayload = try {
            actualClient.get("$urlBase$profileName")
        } catch (ex: EC2MetadataError) {
            when {
                apiVersion == null && ex.status == HttpStatusCode.NotFound -> {
                    // Tried EXTENDED and that didn't work; fallback to LEGACY
                    apiVersion = ApiVersion.LEGACY
                    return resolveSingleFlight()
                }

                instanceProfileName.get() == null && ex.status == HttpStatusCode.NotFound -> {
                    // A previously-resolved profile is now invalid; forget the resolved name and re-resolve
                    resolvedProfileName = null
                    return resolveSingleFlight()
                }

                else -> return usePreviousCredentials()
                    ?: throw ImdsCredentialsException(profileName, ex).wrapAsCredentialsProviderException()
            }
        } catch (ex: IOException) {
            return usePreviousCredentials()
                ?: throw ImdsCredentialsException(profileName, ex).wrapAsCredentialsProviderException()
        } catch (ex: Exception) {
            throw ImdsCredentialsException(profileName, ex).wrapAsCredentialsProviderException()
        }

        if (instanceProfileName.get() == null) {
            // No profile name was provided at construction time; cache the resolved name
            resolvedProfileName = profileName
        }

        val deserializer = JsonDeserializer(credsPayload.encodeToByteArray())

        return when (val resp = deserializeJsonCredentials(deserializer)) {
            is JsonCredentialsResponse.SessionCredentials -> {
                val creds = credentials(
                    resp.accessKeyId,
                    resp.secretAccessKey,
                    resp.sessionToken,
                    resp.expiration,
                    PROVIDER_NAME,
                    resp.accountId,
                ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_IMDS)

                creds.also { previousCredentials = it }
            }
            is JsonCredentialsResponse.Error -> when (resp.code) {
                CODE_ASSUME_ROLE_UNAUTHORIZED_ACCESS -> throw ProviderConfigurationException("Incorrect IMDS/IAM configuration: [${resp.code}] ${resp.message}. Hint: Does this role have a trust relationship with EC2?")
                else -> throw CredentialsProviderException("Error retrieving credentials from IMDS: code=${resp.code}; ${resp.message}")
            }
        }
    }

    override fun close() {
        if (manageClient) {
            actualClient.close()
        }
    }

    private suspend fun usePreviousCredentials(): Credentials? = previousCredentials?.apply {
        coroutineContext.info<ImdsCredentialsProvider> {
            "Attempting to reuse previously-fetched credentials (expiration = $expiration)"
        }
    }

    override fun toString(): String = this.simpleClassName

    /**
     * Identifies different versions of IMDS APIs for fetching credentials
     */
    private enum class ApiVersion(val urlBase: String) {
        /**
         * The original, now-deprecated API
         */
        LEGACY("/latest/meta-data/iam/security-credentials/"),

        /**
         * The new API which provides `AccountId` and potentially other fields in the future
         */
        EXTENDED("/latest/meta-data/iam/security-credentials-extended/"),
    }
}

internal class ImdsCredentialsException(
    profileName: String,
    cause: Throwable,
) : RuntimeException("Failed to load credentials for EC2 instance profile \"$profileName\"", cause)

internal class ImdsProfileException(cause: Throwable) : RuntimeException("Failed to load instance profile name", cause)

private fun Throwable.wrapAsCredentialsProviderException() = CredentialsProviderException(message.orEmpty(), this)
