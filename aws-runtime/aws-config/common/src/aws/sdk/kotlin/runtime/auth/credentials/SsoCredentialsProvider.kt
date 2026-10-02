/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.runtime.auth.credentials.internal.nonRecoverable
import aws.sdk.kotlin.runtime.auth.credentials.internal.sso.SsoClient
import aws.sdk.kotlin.runtime.auth.credentials.internal.sso.getRoleCredentials
import aws.sdk.kotlin.runtime.auth.credentials.internal.sso.model.UnauthorizedException
import aws.sdk.kotlin.runtime.config.AwsSdkClientOption
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.*
import aws.smithy.kotlin.runtime.client.SdkClientOption
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.identity.Identity
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.telemetry.telemetryProvider
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.fromEpochMilliseconds
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlin.coroutines.coroutineContext

private const val PROVIDER_NAME = "SSO"

/**
 * [CredentialsProvider] that uses AWS Single Sign-On (AWS SSO) to source credentials. The
 * provider is expected to be configured for the AWS Region where the AWS SSO user portal is hosted.
 *
 * The provider does not initiate or perform the AWS SSO login flow. It is expected that you have
 * already performed the SSO login flow using (e.g. using the AWS CLI `aws sso login`). The provider
 * expects a valid non-expired access token for the AWS SSO user portal URL in `~/.aws/sso/cache`.
 * If a cached token is not found, it is expired, or the file is malformed an exception will be thrown.
 *
 *
 * **Instantiating AWS SSO provider directly**
 *
 * You can programmatically construct the AWS SSO provider in your application, and provide the necessary
 * information to load and retrieve temporary credentials using an access token from `~/.aws/sso/cache`.
 *
 * ```
 * val ssoProvider = SsoCredentialsProvider(
 *     accountId = "123456789",
 *     roleName = "SsoReadOnlyRole",
 *     startUrl = "https://my-sso-portal.awsapps.com/start",
 *     ssoRegion = "us-east-2"
 * )
 * ```
 * This provider caches and refreshes credentials internally, so it does not need to be wrapped in a caching provider.
 * Close it when you are done with it to release the cache.
 *
 *
 * **Additional Resources**
 * * [Configuring the AWS CLI to use AWS Single Sign-On](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-sso.html)
 * * [AWS Single Sign-On User Guide](https://docs.aws.amazon.com/singlesignon/latest/userguide/what-is.html)
 *
 * @param accountId The AWS account ID that temporary AWS credentials will be resolved for
 * @param roleName The IAM role in the AWS account that temporary AWS credentials will be resolved for
 * @param startUrl The start URL (also known as the "User Portal URL") provided by the SSO service
 * @param ssoRegion The AWS region where the SSO directory for the given [startUrl] is hosted.
 * @param ssoSessionName The SSO Session name from the profile. If a session name is given an [SsoTokenProvider]
 * will be used to fetch tokens.
 * @param httpClient The [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and lifetime
 * are NOT managed by the provider. Caller is responsible for closing.
 * @param platformProvider The platform provider
 * @param clock The source of time for the provider
 */
public class SsoCredentialsProvider public constructor(
    public val accountId: String,
    public val roleName: String,
    public val startUrl: String,
    public val ssoRegion: String,
    public val ssoSessionName: String? = null,
    public val httpClient: HttpClientEngine? = null,
    public val platformProvider: PlatformProvider = PlatformProvider.System,
    private val clock: Clock = Clock.System,
) : CloseableCredentialsProvider,
    RefreshAwareCredentialsProvider {

    private val ssoTokenProvider = ssoSessionName?.let { sessName ->
        SsoTokenProvider(sessName, startUrl, ssoRegion, httpClient = httpClient, platformProvider = platformProvider, clock = clock)
    }

    private val refresh = SelfManagedRefresh(::resolveUncached, clock = clock)

    override suspend fun resolve(attributes: Attributes): Credentials = refresh.resolve(attributes)

    override suspend fun invalidate(rejectedIdentity: Identity): Unit = refresh.invalidate(rejectedIdentity)

    /**
     * Calls `GetRoleCredentials` with no caching or pacing of its own. The caller's cache decides what a failure or an
     * already-past expiration means.
     */
    private suspend fun resolveUncached(attributes: Attributes): Credentials {
        val logger = coroutineContext.logger<SsoCredentialsProvider>()

        val token = if (ssoTokenProvider != null) {
            logger.trace { "Attempting to load token using token provider for sso-session: `$ssoSessionName`" }
            ssoTokenProvider.resolve(attributes)
        } else {
            logger.trace { "Attempting to load token from file using legacy format" }
            legacyLoadTokenFile()
        }

        val telemetry = coroutineContext.telemetryProvider
        val client = SsoClient.fromEnvironment {
            region = ssoRegion
            httpClient = this@SsoCredentialsProvider.httpClient
            telemetryProvider = telemetry
            logMode = attributes.getOrNull(SdkClientOption.LogMode)
            applicationId = attributes.getOrNull(AwsSdkClientOption.ApplicationId)
            // FIXME - create an anonymous credential provider to explicitly avoid default chain creation (technically the transform should remove need for sigv4 cred provider since it's all anon auth)
        }

        val resp = try {
            client.getRoleCredentials {
                accountId = this@SsoCredentialsProvider.accountId
                roleName = this@SsoCredentialsProvider.roleName
                accessToken = token.token
            }
        } catch (ex: Exception) {
            // GetRoleCredentials models exactly one error that a retry cannot fix, so no error-code list is needed.
            // The other three - InvalidRequestException, ResourceNotFoundException and TooManyRequestsException -
            // stay recoverable: the last is a throttle, and the first two can follow a transient misconfiguration.
            val wrapped = CredentialsNotLoadedException("GetRoleCredentials operation failed", ex)
            throw if (ex is UnauthorizedException) wrapped.nonRecoverable() else wrapped
        } finally {
            client.close()
        }

        val roleCredentials = resp.roleCredentials ?: throw CredentialsProviderException("Expected SSO roleCredentials to not be null")

        val creds = credentials(
            accessKeyId = checkNotNull(roleCredentials.accessKeyId) { "Expected accessKeyId in SSO roleCredentials response" },
            secretAccessKey = checkNotNull(roleCredentials.secretAccessKey) { "Expected secretAccessKey in SSO roleCredentials response" },
            sessionToken = roleCredentials.sessionToken,
            expiration = Instant.fromEpochMilliseconds(roleCredentials.expiration),
            PROVIDER_NAME,
            accountId = accountId,
            refreshBehavior = CredentialsRefreshBehavior.RefreshableWithStaticStability,
        )

        return if (ssoTokenProvider != null) {
            creds.withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_SSO)
        } else {
            creds.withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_SSO_LEGACY)
        }
    }

    // non sso-session legacy token flow
    private suspend fun legacyLoadTokenFile(): SsoToken {
        val token = readTokenFromCache(startUrl, platformProvider)
        val now = clock.now()
        if (now > token.expiration) throw ProviderConfigurationException("The SSO session has expired. To refresh this SSO session run `aws sso login` with the corresponding profile.")

        return token
    }

    /**
     * Releases the refresh lifecycle. The `SsoClient` each resolution builds is closed by that resolution, and the
     * token provider holds nothing closeable, so there is nothing else to release here.
     */
    override fun close() {
        refresh.close()
    }

    override fun toString(): String = this.simpleClassName
}
