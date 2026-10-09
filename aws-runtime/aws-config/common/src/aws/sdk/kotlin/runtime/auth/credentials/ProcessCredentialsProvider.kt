/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.*
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.serde.json.JsonDeserializer
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlin.coroutines.coroutineContext

internal expect suspend fun executeCommand(
    command: String,
    platformProvider: PlatformProvider,
    maxOutputLengthBytes: Long,
    timeoutMillis: Long,
    clock: Clock = Clock.System,
): Pair<Int, String>

private const val PROVIDER_NAME = "Process"

/**
 * [CredentialsProvider] that invokes a command to retrieve [Credentials] from stdout.
 * If the exit code is non-zero, log and throw an exception including the stderr from the command.
 *
 * WARNING: This [CredentialsProvider] sources credentials from an external process.
 * This can potentially be dangerous, so proceed with caution. Other credential providers should be preferred if possible.
 * If using this option, secure the config file and any process/script files using security best practices for your operating system.
 * Additionally, make sure to follow command-line conventions such as surrounding filenames / paths with quotes if they contain spaces.
 * See the public documentation for a full list of expected conventions.
 * @see <a href="https://docs.aws.amazon.com/sdkref/latest/guide/feature-process-credentials.html">Process Credentials</a>
 *
 * @param credentialProcess the command to invoke to retrieve credentials
 * @param platformProvider the platform provider
 * @param maxOutputLengthBytes the maximum output of the process in bytes
 * @param timeoutMillis the timeout of the process
 * @throws RuntimeException if the process returns larger than [maxOutputLengthBytes] or takes longer than [timeoutMillis] to return [Credentials]
 */
public class ProcessCredentialsProvider(
    private val credentialProcess: String,
    private val platformProvider: PlatformProvider = PlatformProvider.System,
    private val maxOutputLengthBytes: Long = 64 * 1024,
    private val timeoutMillis: Long = 60_000,
) : CredentialsProvider {
    override suspend fun resolve(attributes: Attributes): Credentials {
        val logger = coroutineContext.logger<ProcessCredentialsProvider>()

        val (exitCode, output) = try {
            executeCommand(credentialProcess, platformProvider, maxOutputLengthBytes, timeoutMillis)
        } catch (ex: Exception) {
            throw CredentialsProviderException("Failed to execute command", ex)
        }

        if (exitCode != 0) {
            logger.warn { "Command completed with nonzero exit code $exitCode: $output" }
            throw CredentialsProviderException("Command completed with nonzero exit code $exitCode: $output")
        }

        val payload = output.encodeToByteArray()
        val deserializer = JsonDeserializer(payload)

        return when (val resp = deserializeJsonProcessCredentials(deserializer)) {
            is JsonCredentialsResponse.SessionCredentials -> {
                credentials(
                    resp.accessKeyId,
                    resp.secretAccessKey,
                    resp.sessionToken,
                    // A credential process that states no expiration leaves this null rather than claiming a date in
                    // the year 275760. Pinning it to Instant.MAX_VALUE told every consumer the credentials never
                    // expire, which suppresses refresh; a null expiration is re-read on the caching provider's own
                    // cadence instead.
                    resp.expiration,
                    PROVIDER_NAME,
                    resp.accountId,
                ).withBusinessMetric(AwsBusinessMetric.Credentials.CREDENTIALS_PROCESS)
            }
            else -> throw CredentialsProviderException("Credentials response was not of expected format")
        }
    }

    override fun toString(): String = this.simpleClassName
}
