/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.ClientException

/**
 * The reasons a [CredentialDaemonCredentialsProvider] can fail.
 *
 * @param isRecoverable whether the failure is likely to go away on a later attempt without user action
 */
public enum class CredentialDaemonErrorCode(public val isRecoverable: Boolean) {
    /**
     * The credential daemon could not be reached, or did not answer in time.
     */
    NOT_RUNNING(true),

    /**
     * The credential daemon needs the user to sign in again.
     */
    REAUTH_REQUIRED(false),

    /**
     * The credential daemon has no credential with the requested name.
     */
    CREDENTIAL_NOT_FOUND(false),

    /**
     * The credential daemon rejected the request.
     */
    REQUEST_REJECTED(true),

    /**
     * The credential daemon returned a kind of credential material this SDK does not support.
     */
    UNSUPPORTED_MATERIAL(false),

    /**
     * The credential daemon reply did not follow the protocol.
     */
    PROTOCOL_ERROR(true),

    /**
     * The credential name is invalid, or the shared config profile that refers to the credential daemon is invalid.
     * A profile error is raised as a [ProviderConfigurationException] whose cause carries this code.
     */
    INVALID_CONFIG(false),

    /**
     * The credential daemon cannot be used on this platform or runtime.
     */
    UNSUPPORTED_PLATFORM(false),
}

/**
 * An error raised while getting credentials from the credential daemon.
 *
 * @param errorCode the kind of failure
 * @param message the error message
 * @param daemonErrorCode the error code the daemon returned in `error.data.code`, if any
 * @param cause the underlying cause, if any
 */
public class CredentialDaemonException(
    public val errorCode: CredentialDaemonErrorCode,
    message: String,
    public val daemonErrorCode: String? = null,
    cause: Throwable? = null,
) : ClientException(message, cause)

/**
 * Link to the credential daemon documentation, included in setup-related error messages.
 */
internal const val CREDENTIAL_DAEMON_DOC = "https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-credential-daemon.html"

/**
 * Remove control characters from text that came from outside the SDK.
 */
internal fun String.stripControlChars(): String = filterNot { it.isISOControl() }

internal fun credentialDaemonNotRunning(path: String, reason: String?, cause: Throwable? = null): CredentialDaemonException = CredentialDaemonException(
    CredentialDaemonErrorCode.NOT_RUNNING,
    "could not get credentials from the credential daemon at `${path.stripControlChars()}`: " +
        "${reason?.stripControlChars() ?: "<no OS error message>"}. " +
        "Make sure the credential daemon is installed and running, see $CREDENTIAL_DAEMON_DOC",
    cause = cause,
)

/**
 * No socket location applies, so nothing was dialed
 */
internal fun credentialDaemonNoSocket(): CredentialDaemonException = CredentialDaemonException(
    CredentialDaemonErrorCode.NOT_RUNNING,
    "could not find the credential daemon: `AWS_CREDENTIAL_DAEMON_SOCKET` is not set to an absolute path and " +
        "nothing is at `\$HOME/.aws/credential-daemon.sock`. Install the credential daemon or link its socket to " +
        "`\$HOME/.aws/credential-daemon.sock`, or set `AWS_CREDENTIAL_DAEMON_SOCKET`, see $CREDENTIAL_DAEMON_DOC",
)

internal fun credentialDaemonUnsupportedPlatform(reason: String, cause: Throwable? = null): CredentialDaemonException = CredentialDaemonException(
    CredentialDaemonErrorCode.UNSUPPORTED_PLATFORM,
    "the credential daemon is not supported here: $reason. See $CREDENTIAL_DAEMON_DOC",
    cause = cause,
)

internal fun credentialDaemonProtocolError(what: String): CredentialDaemonException = CredentialDaemonException(
    CredentialDaemonErrorCode.PROTOCOL_ERROR,
    "invalid reply from the credential daemon: $what",
)

/**
 * A [CredentialDaemonException] with [CredentialDaemonErrorCode.INVALID_CONFIG], raised as is. For a malformed
 * shared config profile use [credentialDaemonProfileProviderConfigurationException] instead.
 */
internal fun credentialDaemonInvalidConfigException(message: String): CredentialDaemonException = CredentialDaemonException(CredentialDaemonErrorCode.INVALID_CONFIG, message)

/**
 * A malformed shared config profile that refers to the credential daemon. It is a [ProviderConfigurationException],
 * like the other profile format errors, and its cause is a [CredentialDaemonException] with
 * [CredentialDaemonErrorCode.INVALID_CONFIG].
 */
internal fun credentialDaemonProfileProviderConfigurationException(message: String): ProviderConfigurationException = ProviderConfigurationException(message, credentialDaemonInvalidConfigException(message))
