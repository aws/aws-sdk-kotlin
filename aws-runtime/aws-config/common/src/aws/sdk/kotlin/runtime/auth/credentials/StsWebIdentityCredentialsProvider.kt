/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.arns.Arn
import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.StsClient
import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.assumeRoleWithWebIdentity
import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.model.PolicyDescriptorType
import aws.sdk.kotlin.runtime.config.AwsSdkClientOption
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.*
import aws.smithy.kotlin.runtime.client.SdkClientOption
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.config.EnvironmentSetting
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.telemetry.telemetryProvider
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val PROVIDER_NAME = "WebIdentityToken"

/**
 * A [CredentialsProvider] that exchanges a Web Identity Token for credentials from the AWS Security Token Service (STS).
 *
 * @param webIdentityParameters The parameters to pass to the `AssumeRoleWithWebIdentity` call
 * @param region The AWS region to assume the role in
 * @param platformProvider The platform API provider
 * @param httpClient the [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and lifetime
 * are NOT managed by the provider. Caller is responsible for closing.
 */
public class StsWebIdentityCredentialsProvider(
    public val webIdentityParameters: AssumeRoleWithWebIdentityParameters,
    public val region: String?,
    public val platformProvider: PlatformProvider = PlatformProvider.System,
    public val httpClient: HttpClientEngine? = null,
) : CredentialsProvider {

    /**
     * A [CredentialsProvider] that exchanges a Web Identity Token for credentials from the AWS Security Token Service
     * (STS).
     *
     * @param roleArn The ARN of the target role to assume, e.g. `arn:aws:iam:123456789:role/example`
     * @param webIdentityTokenFilePath The path to the file containing a JWT token
     * @param region The AWS region to assume the role in
     * @param roleSessionName The name to associate with the session. Use the role session name to uniquely identify a
     * session when the same role is assumed by different principals or for different reasons. In cross-account
     * scenarios, the role session name is visible to, and can be logged by the account that owns the role. The role
     * session name is also in the ARN of the assumed role principal.
     * @param duration The expiry duration of the credentials. Defaults to 15 minutes if not set.
     * @param platformProvider The platform API provider
     * @param httpClient the [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and
     * lifetime are NOT managed by the provider. Caller is responsible for closing.
     */
    public constructor(
        roleArn: String,
        webIdentityTokenFilePath: String,
        region: String?,
        roleSessionName: String? = null,
        duration: Duration = DEFAULT_CREDENTIALS_REFRESH_SECONDS.seconds,
        platformProvider: PlatformProvider = PlatformProvider.System,
        httpClient: HttpClientEngine? = null,
    ) : this(
        AssumeRoleWithWebIdentityParameters(
            roleArn = roleArn,
            webIdentityTokenFilePath = webIdentityTokenFilePath,
            roleSessionName = roleSessionName,
            duration = duration,
        ),
        region,
        platformProvider,
        httpClient,
    )

    public companion object {
        /**
         * Create an [StsWebIdentityCredentialsProvider] from the current execution environment. This will attempt
         * to automatically resolve any setting not explicitly provided from the current set of environment variables
         * or system properties.
         */
        public fun fromEnvironment(
            roleArn: String? = null,
            webIdentityTokenFilePath: String? = null,
            region: String? = null,
            roleSessionName: String? = null,
            duration: Duration = DEFAULT_CREDENTIALS_REFRESH_SECONDS.seconds,
            platformProvider: PlatformProvider = PlatformProvider.System,
            httpClient: HttpClientEngine? = null,
        ): StsWebIdentityCredentialsProvider {
            val resolvedRoleArn = platformProvider.resolve(roleArn, AwsSdkSetting.AwsRoleArn, "roleArn")
            val resolvedTokenFilePath = platformProvider.resolve(webIdentityTokenFilePath, AwsSdkSetting.AwsWebIdentityTokenFile, "webIdentityTokenFilePath")
            val resolvedRegion = region ?: AwsSdkSetting.AwsRegion.resolve(platformProvider)
            return StsWebIdentityCredentialsProvider(resolvedRoleArn, resolvedTokenFilePath, resolvedRegion, roleSessionName, duration, platformProvider, httpClient)
        }
    }

    override suspend fun resolve(attributes: Attributes): Credentials {
        val logger = coroutineContext.logger<StsAssumeRoleCredentialsProvider>()
        logger.debug { "retrieving assumed credentials via web identity" }

        val provider = this
        val params = provider.webIdentityParameters

        val token = platformProvider
            .readFileOrNull(params.webIdentityTokenFilePath)
            ?.decodeToString() ?: throw CredentialsProviderException("failed to read webIdentityToken from ${params.webIdentityTokenFilePath}")

        val telemetry = coroutineContext.telemetryProvider

        val client = StsClient.fromEnvironment {
            region = provider.region
            httpClient = provider.httpClient
            // NOTE: credentials provider not needed for this operation
            telemetryProvider = telemetry
            logMode = attributes.getOrNull(SdkClientOption.LogMode)
            applicationId = attributes.getOrNull(AwsSdkClientOption.ApplicationId)
        }

        val resp = try {
            client.assumeRoleWithWebIdentity {
                roleArn = params.roleArn
                webIdentityToken = token
                durationSeconds = params.duration.inWholeSeconds.toInt()
                roleSessionName = params.roleSessionName ?: defaultSessionName(platformProvider)
                providerId = params.providerId
                policyArns = params.convertedPolicyArns
                policy = params.policy
            }
        } catch (ex: Exception) {
            logger.debug { "sts refused to grant assumed role credentials from web identity" }
            throw CredentialsProviderException("STS failed to assume role from web identity", ex)
        } finally {
            client.close()
        }

        val roleCredentials = resp.credentials ?: throw CredentialsProviderException("STS credentials must not be null")
        logger.debug { "obtained assumed credentials via web identity; expiration=${roleCredentials.expiration.format(TimestampFormat.ISO_8601)}" }
        val accountId = resp.assumedRoleUser?.arn?.let { Arn.parse(it) }?.accountId

        return credentials(
            accessKeyId = roleCredentials.accessKeyId,
            secretAccessKey = roleCredentials.secretAccessKey,
            sessionToken = roleCredentials.sessionToken,
            expiration = roleCredentials.expiration,
            providerName = PROVIDER_NAME,
            accountId = accountId,
        ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_STS_ASSUME_ROLE_WEB_ID)
    }

    override fun toString(): String = this.simpleClassName
}

/**
 * Parameters passed to an `AssumeRoleWithWebIdentity` call
 * @param roleArn The ARN of the target role to assume, e.g. `arn:aws:iam:123456789:role/example`
 * @param webIdentityTokenFilePath The path to the file containing a JWT token
 * @param roleSessionName The name to associate with the session. Use the role session name to uniquely identify a
 * session when the same role is assumed by different principals or for different reasons. In cross-account scenarios,
 * the role session name is visible to, and can be logged by the account that owns the role. The role session name is
 * also in the ARN of the assumed role principal.
 * @param duration The expiry duration of the credentials. Defaults to 15 minutes if not set.
 * @param providerId The fully qualified host component of the domain name of the OAuth 2.0 identity provider
 * @param policyArns The Amazon Resource Names (ARNs) of the IAM managed policies that you want to use as managed
 * session policies
 * @param policy An IAM policy in JSON format that you want to use as an inline session policy
 */
public class AssumeRoleWithWebIdentityParameters(
    public val roleArn: String,
    public val webIdentityTokenFilePath: String,
    public val roleSessionName: String? = null,
    public val duration: Duration = DEFAULT_CREDENTIALS_REFRESH_SECONDS.seconds,
    public val providerId: String? = null,
    public val policyArns: List<String>? = null,
    public val policy: String? = null,
) {
    internal val convertedPolicyArns = policyArns?.map { PolicyDescriptorType { arn = it } }
}

// convenience function to resolve parameters for fromEnvironment()
private inline fun <reified T> PlatformProvider.resolve(explicit: T?, setting: EnvironmentSetting<T>, name: String): T =
    explicit
        ?: setting.resolve(this)
        ?: throw ProviderConfigurationException(
            "Required field `$name` could not be automatically inferred for StsWebIdentityCredentialsProvider. Either explicitly pass a value, set the environment variable `${setting.envVar}`, or set the JVM system property `${setting.sysProp}`",
        )
