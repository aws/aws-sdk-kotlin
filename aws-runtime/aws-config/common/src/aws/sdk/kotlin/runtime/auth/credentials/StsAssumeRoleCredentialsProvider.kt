/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.arns.Arn
import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.StsClient
import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.assumeRole
import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.model.PolicyDescriptorType
import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.model.RegionDisabledException
import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.model.Tag
import aws.sdk.kotlin.runtime.config.AwsSdkClientOption
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.*
import aws.smithy.kotlin.runtime.client.SdkClientOption
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.telemetry.telemetryProvider
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import aws.smithy.kotlin.runtime.util.PlatformEnvironProvider
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val GLOBAL_STS_PARTITION_ENDPOINT = "aws-global"
private const val PROVIDER_NAME = "AssumeRoleProvider"

/**
 * A [CredentialsProvider] that uses another provider to assume a role from the AWS Security Token Service (STS).
 *
 * When asked to provide credentials, this provider will first invoke the inner credentials provider
 * to get AWS credentials for STS. Then, it will call STS to get assumed credentials for the desired role.
 *
 * @param bootstrapCredentialsProvider The underlying provider to use for source credentials
 * @param assumeRoleParameters The parameters to pass to the `AssumeRole` call
 * @param region The AWS region to assume the role in. If not set then the global STS endpoint will be used.
 * @param httpClient the [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and lifetime
 * are NOT managed by the provider. Caller is responsible for closing.
 */
public class StsAssumeRoleCredentialsProvider(
    public val bootstrapCredentialsProvider: CredentialsProvider,
    public val assumeRoleParameters: AssumeRoleParameters,
    public val region: String? = null,
    public val httpClient: HttpClientEngine? = null,
) : CredentialsProvider {

    /**
     * A [CredentialsProvider] that uses another provider to assume a role from the AWS Security Token Service (STS).
     *
     * When asked to provide credentials, this provider will first invoke the inner credentials provider
     * to get AWS credentials for STS. Then, it will call STS to get assumed credentials for the desired role.
     *
     * @param bootstrapCredentialsProvider The underlying provider to use for source credentials
     * @param roleArn The ARN of the target role to assume, e.g. `arn:aws:iam:123456789:role/example`
     * @param region The AWS region to assume the role in. If not set then the global STS endpoint will be used.
     * @param roleSessionName The name to associate with the session. Use the role session name to uniquely identify a
     * session when the same role is assumed by different principals or for different reasons. In cross-account
     * scenarios, the role session name is visible to, and can be logged by the account that owns the role. The role
     * session name is also in the ARN of the assumed role principal.
     * @param externalId A unique identifier that might be required when you assume a role in another account. If the
     * administrator of the account to which the role belongs provided you with an external ID, then provide that value
     * in this parameter.
     * @param duration The expiry duration of the STS credentials. Defaults to 15 minutes if not set.
     * @param httpClient the [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and
     * lifetime are NOT managed by the provider. Caller is responsible for closing.
     */
    public constructor(
        bootstrapCredentialsProvider: CredentialsProvider,
        roleArn: String,
        region: String? = null,
        roleSessionName: String? = null,
        externalId: String? = null,
        duration: Duration = DEFAULT_CREDENTIALS_REFRESH_SECONDS.seconds,
        httpClient: HttpClientEngine? = null,
    ) : this(
        bootstrapCredentialsProvider,
        AssumeRoleParameters(
            roleArn = roleArn,
            roleSessionName = roleSessionName,
            externalId = externalId,
            duration = duration,
        ),
        region,
        httpClient,
    )

    override suspend fun resolve(attributes: Attributes): Credentials {
        val logger = coroutineContext.logger<StsAssumeRoleCredentialsProvider>()
        logger.debug { "retrieving assumed credentials" }

        // NOTE: multi region access points require regional STS endpoints
        val provider = this
        val telemetry = coroutineContext.telemetryProvider
        val client = StsClient.fromEnvironment {
            region = provider.region ?: GLOBAL_STS_PARTITION_ENDPOINT
            credentialsProvider = provider.bootstrapCredentialsProvider
            httpClient = provider.httpClient
            telemetryProvider = telemetry
            logMode = attributes.getOrNull(SdkClientOption.LogMode)
            applicationId = attributes.getOrNull(AwsSdkClientOption.ApplicationId)
        }

        val resp = try {
            client.assumeRole {
                val params = provider.assumeRoleParameters

                roleArn = params.roleArn
                externalId = params.externalId
                roleSessionName = params.roleSessionName ?: defaultSessionName()
                durationSeconds = params.duration.inWholeSeconds.toInt()
                policyArns = params.convertedPolicyArns
                policy = params.policy
                tags = params.convertedTags
                transitiveTagKeys = params.transitiveTagKeys
                serialNumber = params.serialNumber
                tokenCode = params.tokenCode
                sourceIdentity = params.sourceIdentity
            }
        } catch (ex: Exception) {
            logger.debug { "sts refused to grant assumed role credentials" }
            when (ex) {
                is RegionDisabledException -> throw ProviderConfigurationException(
                    "STS is not activated in the requested region (${client.config.region}). Please check your configuration and activate STS in the target region if necessary",
                    ex,
                )
                else -> throw CredentialsProviderException("failed to assume role from STS", ex)
            }
        } finally {
            client.close()
        }

        val roleCredentials = resp.credentials ?: throw CredentialsProviderException("STS credentials must not be null")
        val accountId = resp.assumedRoleUser?.arn?.let { Arn.parse(it).accountId }
        logger.debug { "obtained assumed credentials; expiration=${roleCredentials.expiration.format(TimestampFormat.ISO_8601)}" }

        return credentials(
            accessKeyId = roleCredentials.accessKeyId,
            secretAccessKey = roleCredentials.secretAccessKey,
            sessionToken = roleCredentials.sessionToken,
            expiration = roleCredentials.expiration,
            providerName = PROVIDER_NAME,
            accountId = accountId,
        ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_STS_ASSUME_ROLE)
    }

    override fun toString(): String = this.simpleClassName
}

/**
 * Parameters passed to an `AssumeRole` call
 * @param roleArn The ARN of the target role to assume, e.g. `arn:aws:iam:123456789:role/example`
 * @param roleSessionName The name to associate with the session. Use the role session name to uniquely identify a
 * session when the same role is assumed by different principals or for different reasons. In cross-account scenarios,
 * the role session name is visible to, and can be logged by the account that owns the role. The role session name is
 * also in the ARN of the assumed role principal.
 * @param externalId A unique identifier that might be required when you assume a role in another account. If the
 * administrator of the account to which the role belongs provided you with an external ID, then provide that value in
 * this parameter.
 * @param duration The expiry duration of the STS credentials. Defaults to 15 minutes if not set.
 * @param policyArns The Amazon Resource Names (ARNs) of the IAM managed policies that you want to use as managed
 * session policies
 * @param policy An IAM policy in JSON format that you want to use as an inline session policy
 * @param tags A list of session tags that you want to pass
 * @param transitiveTagKeys A list of keys for session tags that you want to set as transitive
 * @param serialNumber The identification number of the MFA device that is associated with the user who is making the
 * `AssumeRole` call
 * @param tokenCode The value provided by the MFA device, if the trust policy of the role being assumed requires MFA
 * @param sourceIdentity The source identity specified by the principal that is calling the `AssumeRole` operation
 */
public class AssumeRoleParameters(
    public val roleArn: String,
    public val roleSessionName: String? = null,
    public val externalId: String? = null,
    public val duration: Duration = DEFAULT_CREDENTIALS_REFRESH_SECONDS.seconds,
    public val policyArns: List<String>? = null,
    public val policy: String? = null,
    public val tags: Map<String, String>? = null,
    public val transitiveTagKeys: List<String>? = null,
    public val serialNumber: String? = null,
    public val tokenCode: String? = null,
    public val sourceIdentity: String? = null,
) {
    internal val convertedPolicyArns = policyArns?.map { PolicyDescriptorType { arn = it } }
    internal val convertedTags = tags?.map {
        Tag {
            key = it.key
            value = it.value
        }
    }
}

// role session name must be provided to assume a role, when the user doesn't provide one we choose a name for them
internal fun defaultSessionName(platformEnvironProvider: PlatformEnvironProvider = PlatformProvider.System): String =
    AwsSdkSetting.AwsRoleSessionName.resolve(platformEnvironProvider) ?: "aws-sdk-kotlin-${Instant.now().epochMilliseconds}"
