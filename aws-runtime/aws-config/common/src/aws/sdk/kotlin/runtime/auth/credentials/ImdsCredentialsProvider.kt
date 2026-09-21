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
import aws.smithy.kotlin.runtime.collections.attributesOf
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.identity.Identity
import aws.smithy.kotlin.runtime.serde.json.JsonDeserializer
import aws.smithy.kotlin.runtime.telemetry.logging.info
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.PlatformEnvironProvider
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlin.coroutines.coroutineContext

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
 * This provider caches and refreshes credentials internally, so it does not need to be wrapped in a caching provider.
 * If IMDS is reachable but hands back credentials that have already expired, they keep being used and the refresh is
 * retried on a backoff. Close it when you are done with it to release the cache and the underlying client.
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
) : CloseableCredentialsProvider,
    RefreshAwareCredentialsProvider {

    private val refresh = SelfManagedRefresh(::resolveUncached, clock = clock)

    override suspend fun resolve(attributes: Attributes): Credentials = refresh.resolve(attributes)

    override suspend fun invalidate(rejectedIdentity: Identity): Unit = refresh.invalidate(rejectedIdentity)

    /**
     * Issues the IMDS requests with no caching or pacing of its own.
     *
     * A failure propagates instead of falling back to a retained value, and an already-past expiration is returned
     * as-is. Both are the caller's cache to interpret: it retains the previous credentials, decides whether they may
     * still be used, and paces the retry.
     */
    private suspend fun resolveUncached(attributes: Attributes): Credentials {
        if (AwsSdkSetting.AwsEc2MetadataDisabled.resolve(platformProvider) == true) {
            throw CredentialsNotLoadedException("AWS EC2 metadata is explicitly disabled; credentials not loaded")
        }

        val profileName = try {
            profileOverride ?: loadProfile()
        } catch (ex: Exception) {
            throw CredentialsProviderException("failed to load instance profile", ex)
        }

        val payload = try {
            client.value.get("$CREDENTIALS_BASE_PATH$profileName")
        } catch (ex: Exception) {
            throw CredentialsProviderException("failed to load credentials", ex)
        }

        return when (val resp = deserializeJsonCredentials(JsonDeserializer(payload.encodeToByteArray()))) {
            is JsonCredentialsResponse.SessionCredentials -> Credentials(
                resp.accessKeyId,
                resp.secretAccessKey,
                resp.sessionToken,
                resp.expiration,
                PROVIDER_NAME,
                attributesOf { CredentialsRefreshBehaviorKey to CredentialsRefreshBehavior.RefreshableWithStaticStability },
            ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_IMDS)

            is JsonCredentialsResponse.Error -> when (resp.code) {
                CODE_ASSUME_ROLE_UNAUTHORIZED_ACCESS -> throw ProviderConfigurationException(
                    "Incorrect IMDS/IAM configuration: [${resp.code}] ${resp.message}. " +
                        "Hint: Does this role have a trust relationship with EC2?",
                )
                else -> throw CredentialsProviderException(
                    "Error retrieving credentials from IMDS: code=${resp.code}; ${resp.message}",
                )
            }
        }
    }

    override fun close() {
        refresh.close()
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

    override fun toString(): String = this.simpleClassName
}
