/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlin.coroutines.coroutineContext

/**
 * [CredentialsProvider] that uses AWS Login to source credentials.
 *
 * The provider does not initiate or perform the AWS Login flow. It is expected that you have
 * already performed the login flow using (e.g. using the AWS CLI `aws login`). The provider
 * expects a valid non-expired access token for the AWS Login session in `~/.aws/login/cache` or
 * the directory specified by the `AWS_LOGIN_CACHE_DIRECTORY` environment variable.
 * If a cached token is not found, it is expired, or the file is malformed an exception will be thrown.
 *
 *
 * **Instantiating AWS Login provider directly**
 *
 * You can programmatically construct the AWS Login provider in your application, and provide the necessary
 * information to load and retrieve temporary credentials using an access token from `~/.aws/login/cache` or
 * the directory specified by the `AWS_LOGIN_CACHE_DIRECTORY` environment variable.
 *
 * ```
 * val source = LoginCredentialsProvider(
 *     loginSession = "my-login-session"
 * )
 *
 * // Wrap the provider with a caching provider to cache the credentials until their expiration time
 * val loginProvider = CachedCredentialsProvider(source)
 * ```
 * It is important that you wrap the provider with [CachedCredentialsProvider] if you are programmatically constructing
 * the provider directly. This prevents your application from accessing the cached access token and requesting new
 * credentials each time the provider is used to source credentials.
 *
 * @param loginSession The Login Session from the profile
 * @param httpClient The [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and lifetime
 * are NOT managed by the provider. Caller is responsible for closing.
 * @param platformProvider The platform provider
 * @param clock The source of time for the provider
 */
public class LoginCredentialsProvider public constructor(
    public val loginSession: String,
    public val httpClient: HttpClientEngine? = null,
    public val platformProvider: PlatformProvider = PlatformProvider.System,
    private val clock: Clock = Clock.System,
) : CredentialsProvider {
    override suspend fun resolve(attributes: Attributes): Credentials {
        val logger = coroutineContext.logger<LoginCredentialsProvider>()

        val loginTokenProvider =
            LoginTokenProvider(loginSession, httpClient = httpClient, platformProvider = platformProvider, clock = clock)

        logger.trace { "Attempting to load token using token provider for login-session: `$loginSession`" }
        val creds = loginTokenProvider.resolve(attributes)

        return creds.withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_LOGIN)
    }
}
