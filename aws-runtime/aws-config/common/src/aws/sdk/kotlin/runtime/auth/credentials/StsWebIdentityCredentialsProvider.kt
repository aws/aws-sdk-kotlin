/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.StsClient
import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.assumeRoleWithWebIdentity
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.smithy.kotlin.runtime.auth.awscredentials.CloseableCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.auth.awscredentials.DEFAULT_CREDENTIALS_REFRESH_SECONDS
import aws.smithy.kotlin.runtime.config.EnvironmentSetting
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.tracing.*
import aws.smithy.kotlin.runtime.util.Attributes
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val PROVIDER_NAME = "WebIdentityToken"

/**
 * A [CredentialsProvider] that exchanges a Web Identity Token for credentials from the AWS Security Token Service (STS).
 *
 * @param roleArn The ARN of the target role to assume, e.g. `arn:aws:iam:123456789:role/example`
 * @param webIdentityTokenFilePath The path to the file containing a JWT token
 * @param region The AWS region to assume the role in
 * @param roleSessionName The name to associate with the session. Use the role session name to uniquely identify a session
 * when the same role is assumed by different principals or for different reasons. In cross-account scenarios, the
 * role session name is visible to, and can be logged by the account that owns the role. The role session name is also
 * in the ARN of the assumed role principal.
 * @param duration The expiry duration of the credentials. Defaults to 15 minutes if not set.
 * @param platformProvider The platform API provider
 * @param httpClientEngine The [HttpClientEngine] to use when making requests to the STS service
 */
public class StsWebIdentityCredentialsProvider(
    private val roleArn: String,
    private val webIdentityTokenFilePath: String,
    private val region: String,
    private val roleSessionName: String? = null,
    private val duration: Duration = DEFAULT_CREDENTIALS_REFRESH_SECONDS.seconds,
    private val platformProvider: PlatformProvider = PlatformProvider.System,
    private val httpClientEngine: HttpClientEngine? = null,
) : CloseableCredentialsProvider {

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
            httpClientEngine: HttpClientEngine? = null,
        ): StsWebIdentityCredentialsProvider {
            val resolvedRoleArn = platformProvider.resolve(roleArn, AwsSdkSetting.AwsRoleArn, "roleArn")
            val resolvedTokenFilePath = platformProvider.resolve(webIdentityTokenFilePath, AwsSdkSetting.AwsWebIdentityTokenFile, "webIdentityTokenFilePath")
            val resolvedRegion = platformProvider.resolve(region, AwsSdkSetting.AwsRegion, "region")
            return StsWebIdentityCredentialsProvider(resolvedRoleArn, resolvedTokenFilePath, resolvedRegion, roleSessionName, duration, platformProvider, httpClientEngine)
        }
    }

    override suspend fun resolve(attributes: Attributes): Credentials {
        val traceSpan = coroutineContext.traceSpan
        val logger = traceSpan.logger<StsAssumeRoleCredentialsProvider>()
        logger.debug { "retrieving assumed credentials via web identity" }
        val provider = this

        val token = platformProvider
            .readFileOrNull(webIdentityTokenFilePath)
            ?.decodeToString() ?: throw CredentialsProviderException("failed to read webIdentityToken from $webIdentityTokenFilePath")

        val client = StsClient {
            region = provider.region
            httpClientEngine = provider.httpClientEngine
            // NOTE: credentials provider not needed for this operation
            tracer = traceSpan.asNestedTracer("STS-")
        }

        val resp = try {
            client.assumeRoleWithWebIdentity {
                roleArn = provider.roleArn
                webIdentityToken = token
                durationSeconds = provider.duration.inWholeSeconds.toInt()
                roleSessionName = provider.roleSessionName ?: defaultSessionName(platformProvider)
            }
        } catch (ex: Exception) {
            logger.debug { "sts refused to grant assumed role credentials from web identity" }
            throw CredentialsProviderException("STS failed to assume role from web identity", ex)
        } finally {
            client.close()
        }

        val roleCredentials = resp.credentials ?: throw CredentialsProviderException("STS credentials must not be null")
        logger.debug { "obtained assumed credentials via web identity; expiration=${roleCredentials.expiration?.format(TimestampFormat.ISO_8601)}" }

        return Credentials(
            accessKeyId = checkNotNull(roleCredentials.accessKeyId) { "Expected accessKeyId in STS assumeRoleWithWebIdentity response" },
            secretAccessKey = checkNotNull(roleCredentials.secretAccessKey) { "Expected secretAccessKey in STS assumeRoleWithWebIdentity response" },
            sessionToken = roleCredentials.sessionToken,
            expiration = roleCredentials.expiration,
            providerName = PROVIDER_NAME,
        )
    }

    override fun close() { }
}

// convenience function to resolve parameters for fromEnvironment()
private inline fun <reified T> PlatformProvider.resolve(explicit: T?, setting: EnvironmentSetting<T>, name: String): T =
    explicit
        ?: setting.resolve(this)
        ?: throw ProviderConfigurationException(
            "Required field `$name` could not be automatically inferred for StsWebIdentityCredentialsProvider. Either explicitly pass a value, set the environment variable `${setting.envVar}`, or set the JVM system property `${setting.sysProp}`",
        )
