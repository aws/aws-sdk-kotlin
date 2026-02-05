/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.imds.EC2MetadataError
import aws.sdk.kotlin.runtime.config.imds.ImdsClient
import aws.sdk.kotlin.runtime.config.imds.InstanceMetadataProvider
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.*
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.io.IOException
import aws.smithy.kotlin.runtime.serde.json.JsonDeserializer
import aws.smithy.kotlin.runtime.telemetry.logging.info
import aws.smithy.kotlin.runtime.telemetry.logging.warn
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.PlatformEnvironProvider
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

private const val CREDENTIALS_BASE_PATH: String = "/latest/meta-data/iam/security-credentials/"
private const val CODE_ASSUME_ROLE_UNAUTHORIZED_ACCESS: String = "AssumeRoleUnauthorizedAccess"
private const val PROVIDER_NAME = "IMDSv2"

/**
 * [CredentialsProvider] that uses EC2 instance metadata service (IMDS) to provide credentials information.
 * This provider requires that the EC2 instance has an [instance profile](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/iam-roles-for-amazon-ec2.html#ec2-instance-profile)
 * configured.
 *
 * See [EC2 IAM Roles](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/iam-roles-for-amazon-ec2.html) for more
 * information.
 *
 * @param profileOverride override the instance profile name. When retrieving credentials, a call must first be made to
 * `<IMDS_BASE_URL>/latest/meta-data/iam/security-credentials/`. This returns the instance profile used. If
 * [profileOverride] is set, the initial call to retrieve the profile is skipped and the provided value is used instead.
 * @param client the IMDS client to use to resolve credentials information with. This provider takes ownership over
 * the lifetime of the given [ImdsClient] and will close it when the provider is closed.
 * @param platformProvider the [PlatformEnvironProvider] instance
 */
public class ImdsCredentialsProvider(
    public val profileOverride: String? = null,
    public val client: Lazy<InstanceMetadataProvider> = lazy { ImdsClient() },
    public val platformProvider: PlatformEnvironProvider = PlatformProvider.System,
    private val clock: Clock = Clock.System,
) : CloseableCredentialsProvider {
    private var previousCredentials: Credentials? = null

    // the time to refresh the Credentials. If set, it will take precedence over the Credentials' expiration time
    private var nextRefresh: Instant? = null

    // protects previousCredentials and nextRefresh
    private val mu = Mutex()

    override suspend fun resolve(attributes: Attributes): Credentials {
        if (AwsSdkSetting.AwsEc2MetadataDisabled.resolve(platformProvider) == true) {
            throw CredentialsNotLoadedException("AWS EC2 metadata is explicitly disabled; credentials not loaded")
        }

        // if we have previously served IMDS credentials and it's not time for a refresh, just return the previous credentials
        mu.withLock {
            previousCredentials?.run {
                nextRefresh?.takeIf { clock.now() < it }?.run {
                    return previousCredentials!!
                }
            }
        }

        val profileName = try {
            profileOverride ?: loadProfile()
        } catch (ex: Exception) {
            return useCachedCredentials(ex) ?: throw CredentialsProviderException("failed to load instance profile", ex)
        }

        val payload = try {
            client.value.get("$CREDENTIALS_BASE_PATH$profileName")
        } catch (ex: Exception) {
            return useCachedCredentials(ex) ?: throw CredentialsProviderException("failed to load credentials", ex)
        }

        val deserializer = JsonDeserializer(payload.encodeToByteArray())

        return when (val resp = deserializeJsonCredentials(deserializer)) {
            is JsonCredentialsResponse.SessionCredentials -> {
                nextRefresh = if (resp.expiration != null && resp.expiration < clock.now()) {
                    coroutineContext.warn<ImdsCredentialsProvider> {
                        "Attempting credential expiration extension due to a credential service availability issue. " +
                            "A refresh of these credentials will be attempted again in " +
                            "${ DEFAULT_CREDENTIALS_REFRESH_SECONDS / 60 } minutes."
                    }
                    clock.now() + DEFAULT_CREDENTIALS_REFRESH_SECONDS.seconds
                } else {
                    null
                }

                val creds = Credentials(
                    resp.accessKeyId,
                    resp.secretAccessKey,
                    resp.sessionToken,
                    resp.expiration,
                    PROVIDER_NAME,
                ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_IMDS)

                creds.also {
                    mu.withLock { previousCredentials = it }
                }
            }
            is JsonCredentialsResponse.Error -> {
                when (resp.code) {
                    CODE_ASSUME_ROLE_UNAUTHORIZED_ACCESS -> throw ProviderConfigurationException("Incorrect IMDS/IAM configuration: [${resp.code}] ${resp.message}. Hint: Does this role have a trust relationship with EC2?")
                    else -> throw CredentialsProviderException("Error retrieving credentials from IMDS: code=${resp.code}; ${resp.message}")
                }
            }
        }
    }

    override fun close() {
        if (client.isInitialized()) {
            client.value.close()
        }
    }

    private suspend fun loadProfile() = try {
        client.value.get(CREDENTIALS_BASE_PATH)
    } catch (ex: EC2MetadataError) {
        if (ex.status == HttpStatusCode.NotFound) {
            coroutineContext.info<ImdsCredentialsProvider> {
                "Received 404 from IMDS when loading profile information. Hint: This instance may not have an " +
                    "IAM role associated."
            }
        }
        throw ex
    }

    private suspend fun useCachedCredentials(ex: Exception): Credentials? = when {
        ex is IOException || ex is EC2MetadataError && ex.status == HttpStatusCode.InternalServerError -> {
            mu.withLock {
                previousCredentials?.apply { nextRefresh = clock.now() + DEFAULT_CREDENTIALS_REFRESH_SECONDS.seconds }
            }
        }
        else -> null
    }

    override fun toString(): String = this.simpleClassName
}
