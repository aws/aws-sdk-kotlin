/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.checkCredentialDaemonTransportSupported
import aws.sdk.kotlin.runtime.auth.credentials.internal.credentialDaemonExchange
import aws.sdk.kotlin.runtime.auth.credentials.internal.credentialDaemonRequest
import aws.sdk.kotlin.runtime.auth.credentials.internal.isValidCredentialDaemonName
import aws.sdk.kotlin.runtime.auth.credentials.internal.parseCredentialDaemonReply
import aws.sdk.kotlin.runtime.auth.credentials.internal.resolveCredentialDaemonSocketPath
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.simpleClassName
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlin.coroutines.coroutineContext

/**
 * A [CredentialsProvider] that gets temporary credentials from a local credential daemon over a Unix domain socket.
 *
 * Each call to [resolve] opens one connection, sends one JSON-RPC 2.0 `credential/get` request and reads one reply.
 * The socket path is looked up on every call, in this order:
 * 1. The `AWS_CREDENTIAL_DAEMON_SOCKET` environment variable, if it is an absolute path
 * 2. `$HOME/.aws/credential-daemon.sock`, if anything is there: a socket, a symbolic link, or a link whose target
 * is missing. `HOME` is read from the environment, and this location is skipped when `HOME` is unset, empty or
 * relative.
 *
 * The provider dials the first location that applies and tries no other location if that dial fails. When no
 * location applies, or location 2 is a link whose target is missing, it raises
 * [CredentialDaemonErrorCode.NOT_RUNNING] without dialing.
 *
 * This provider is used by [ProfileCredentialsProvider] for profiles that set `daemon_credential`.
 *
 * NOTE: This provider does not implement any caching. Use [CachedCredentialsProvider] to decorate it.
 *
 * On the JVM this provider requires Java 16 or newer. Failures raise [CredentialDaemonException].
 *
 * @param credentialName the name of the credential to request from the daemon
 * @param accountId the SSO account ID to request, if any
 * @param roleName the SSO role name to request, if any
 * @param platformProvider the platform provider
 * @param connectTimeoutMillis how long to wait for the connection to the daemon
 * @param callTimeoutMillis how long the whole exchange with the daemon may take
 */
public class CredentialDaemonCredentialsProvider(
    public val credentialName: String,
    public val accountId: String? = null,
    public val roleName: String? = null,
    private val platformProvider: PlatformProvider = PlatformProvider.System,
    private val connectTimeoutMillis: Long = 1_000,
    private val callTimeoutMillis: Long = 5_000,
) : CredentialsProvider {
    init {
        require(connectTimeoutMillis > 0) { "connectTimeoutMillis must be greater than 0" }
        require(callTimeoutMillis > 0) { "callTimeoutMillis must be greater than 0" }
    }

    override suspend fun resolve(attributes: Attributes): Credentials {
        val logger = coroutineContext.logger<CredentialDaemonCredentialsProvider>()

        if (!isValidCredentialDaemonName(credentialName)) {
            throw credentialDaemonInvalidConfigException(
                "credential daemon credential name `${credentialName.stripControlChars()}` must be 1-64 characters [A-Za-z0-9._-] and not start with '.'",
            )
        }

        if (platformProvider.isAndroid) throw credentialDaemonUnsupportedPlatform("Android is not supported")
        val family = platformProvider.osInfo().family
        if (family != OsFamily.Linux && family != OsFamily.MacOs) {
            throw credentialDaemonUnsupportedPlatform("operating system $family is not supported")
        }

        checkCredentialDaemonTransportSupported()
        val path = resolveCredentialDaemonSocketPath(platformProvider)
        logger.debug { "Requesting credential `$credentialName` from the credential daemon at `$path`" }

        val request = credentialDaemonRequest(credentialName, accountId, roleName)
        val line = credentialDaemonExchange(path, request, connectTimeoutMillis, callTimeoutMillis)
        return parseCredentialDaemonReply(line, credentialName)
            .withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIAL_DAEMON)
    }

    override fun toString(): String = this.simpleClassName
}
