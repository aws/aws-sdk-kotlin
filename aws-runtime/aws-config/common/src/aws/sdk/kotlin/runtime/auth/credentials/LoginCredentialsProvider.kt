/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.signin.SigninClient
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.CloseableCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.RefreshAwareCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.SelfManagedRefresh
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.identity.Identity
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.coroutineContext

/**
 * [CredentialsProvider] that uses AWS Login to source credentials.
 *
 * The provider does not initiate or perform the AWS Login flow. It is expected that you have
 * already performed the login flow using the AWS CLI (`aws login`). The provider
 * expects a valid non-expired access token for the AWS Login session in `~/.aws/login/cache` or
 * the directory specified by the `AWS_LOGIN_CACHE_DIRECTORY` environment variable.
 * If a cached token is not found, is expired, or the file is malformed an exception will be thrown.
 *
 * **Instantiating AWS Login provider directly**
 *
 * You can programmatically construct the AWS Login provider in your application, and provide the necessary
 * information to load and retrieve temporary credentials using an access token from `~/.aws/login/cache` or
 * the directory specified by the `AWS_LOGIN_CACHE_DIRECTORY` environment variable.
 *
 * ```
 * val loginProvider = LoginCredentialsProvider(
 *      loginSession = "my-login-session"
 * )
 * ```
 * This provider caches and refreshes credentials internally, so it does not need to be wrapped in a caching provider.
 * Close it when you are done with it to release the cache and the underlying client.
 *
 * @param loginSession The Login Session from the profile
 * @param region The AWS region used to call the log in service.
 * @param httpClient The [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and lifetime
 * are NOT managed by the provider. Caller is responsible for closing.
 * @param platformProvider The platform provider
 * @param clock The source of time for the provider
 */
public class LoginCredentialsProvider public constructor(
    public val loginSession: String,
    public val region: String? = null,
    public val httpClient: HttpClientEngine? = null,
    public val platformProvider: PlatformProvider = PlatformProvider.System,
    private val clock: Clock = Clock.System,
) : CloseableCredentialsProvider,
    RefreshAwareCredentialsProvider {
    private val cacheDirectory = resolveCacheDir(platformProvider)
    private val client = runBlocking { signinClient(region, httpClient) }

    private val refresh = SelfManagedRefresh(::resolveUncached, clock = clock)

    override suspend fun resolve(attributes: Attributes): Credentials = refresh.resolve(attributes)

    override suspend fun invalidate(rejectedIdentity: Identity): Unit = refresh.invalidate(rejectedIdentity)

    /**
     * Resolves through the login token provider with no caching or pacing of its own. The caller's cache decides what
     * a failure or an already-past expiration means.
     */
    private suspend fun resolveUncached(attributes: Attributes): Credentials {
        val logger = coroutineContext.logger<LoginCredentialsProvider>()

        val loginTokenProvider =
            LoginTokenProvider(
                loginSession,
                region,
                httpClient = httpClient,
                platformProvider = platformProvider,
                clock = clock,
                cacheDirectory = cacheDirectory,
                client = client,
            )

        logger.trace { "Attempting to load token using token provider for login-session: `$loginSession`" }
        val creds = loginTokenProvider.resolve(attributes)

        return creds.withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_LOGIN)
    }

    override fun close() {
        refresh.close()
        client.close()
    }
}

internal fun resolveCacheDir(platformProvider: PlatformProvider) = platformProvider.getenv("AWS_LOGIN_CACHE_DIRECTORY") ?: platformProvider.getenv("AWS_LOGIN_IN_CACHE_DIRECTORY") ?: platformProvider.filepath("~", ".aws", "login", "cache")

internal suspend fun signinClient(providedRegion: String? = null, providedHttpClient: HttpClientEngine? = null) = SigninClient.fromEnvironment {
    region = providedRegion
    httpClient = providedHttpClient
}
