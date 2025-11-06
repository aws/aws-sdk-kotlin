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

private const val PROVIDER_NAME = "LOGIN"

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
