/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials.internal

import aws.sdk.kotlin.runtime.auth.credentials.CredentialDaemonErrorCode
import aws.sdk.kotlin.runtime.auth.credentials.CredentialDaemonException
import aws.sdk.kotlin.runtime.auth.credentials.credentialDaemonNoSocket
import aws.sdk.kotlin.runtime.auth.credentials.credentialDaemonNotRunning
import aws.sdk.kotlin.runtime.auth.credentials.credentialDaemonProtocolError
import aws.sdk.kotlin.runtime.auth.credentials.stripControlChars
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.content.Document
import aws.smithy.kotlin.runtime.serde.json.JsonDeserializer
import aws.smithy.kotlin.runtime.serde.json.jsonStreamWriter
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * The environment variable that names the credential daemon socket
 */
internal const val AWS_CREDENTIAL_DAEMON_SOCKET = "AWS_CREDENTIAL_DAEMON_SOCKET"

/**
 * The JVM system property that names the credential daemon socket, checked before [AWS_CREDENTIAL_DAEMON_SOCKET]
 */
internal const val AWS_CREDENTIAL_DAEMON_SOCKET_PROPERTY = "aws.credentialDaemonSocket"

/**
 * The most bytes a credential daemon reply line may hold, not counting the terminating `\n`
 */
internal const val CREDENTIAL_DAEMON_MAX_REPLY_BYTES = 64_000

internal const val CREDENTIAL_DAEMON_PROVIDER_NAME = "CredentialDaemon"

/**
 * The most characters of a daemon-supplied value that an error message repeats
 */
internal const val CREDENTIAL_DAEMON_MAX_ECHO_CHARS = 64

private val CREDENTIAL_NAME_REGEX = Regex("^[A-Za-z0-9_-][A-Za-z0-9._-]{0,63}$")
private val EXPIRES_AT_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?Z$")

/**
 * Cut daemon-supplied text to [CREDENTIAL_DAEMON_MAX_ECHO_CHARS] characters, marking a cut with `...`.
 * A cut never splits a surrogate pair.
 */
internal fun String.truncateForMessage(): String {
    if (length <= CREDENTIAL_DAEMON_MAX_ECHO_CHARS) return this
    var end = CREDENTIAL_DAEMON_MAX_ECHO_CHARS
    if (this[end - 1].isHighSurrogate()) end--
    return substring(0, end) + "..."
}

/**
 * Whether [name] is a valid credential daemon credential name
 */
internal fun isValidCredentialDaemonName(name: String): Boolean = CREDENTIAL_NAME_REGEX.matches(name)

/**
 * The per-user socket location, relative to `$HOME`
 */
internal const val CREDENTIAL_DAEMON_HOME_SOCKET = ".aws/credential-daemon.sock"

/**
 * What is at a socket location, checked without following a symbolic link at the location itself
 */
internal sealed interface SocketPathEntry {
    /**
     * Nothing is there
     */
    data object Missing : SocketPathEntry

    /**
     * Something is there: a socket, another file, or a symbolic link whose target exists
     */
    data object Present : SocketPathEntry

    /**
     * A symbolic link whose target is missing
     *
     * @param target the link's target, as read from the link
     */
    data class DanglingLink(val target: String) : SocketPathEntry
}

private fun String?.absoluteOrNull(): String? = this?.takeIf { it.startsWith('/') }

/**
 * Resolve the credential daemon socket path in the order that [aws.sdk.kotlin.runtime.auth.credentials.CredentialDaemonCredentialsProvider]
 * documents. The result is never cached, callers resolve it for every fetch.
 *
 * Throws a [CredentialDaemonException] with [CredentialDaemonErrorCode.NOT_RUNNING] when no location applies, or
 * when `$HOME/.aws/credential-daemon.sock` is a link whose target is missing. Nothing is dialed in either case.
 */
internal fun resolveCredentialDaemonSocketPath(
    platform: PlatformProvider,
    socketPath: String? = null,
    entryAt: (String) -> SocketPathEntry = ::socketPathEntry,
): String {
    // The provider setting, the system property and the environment variable apply only if set to an absolute
    // path. An empty or relative value counts as unset. System properties are only set on the JVM.
    socketPath.absoluteOrNull()?.let { return it }
    platform.getProperty(AWS_CREDENTIAL_DAEMON_SOCKET_PROPERTY).absoluteOrNull()?.let { return it }
    platform.getenv(AWS_CREDENTIAL_DAEMON_SOCKET).absoluteOrNull()?.let { return it }

    // HOME comes from the environment. An unset, empty or relative HOME skips the location.
    val home = platform.getenv("HOME")?.takeIf { it.startsWith('/') }
    if (home != null) {
        val link = "${home.trimEnd('/')}/$CREDENTIAL_DAEMON_HOME_SOCKET"
        when (val entry = entryAt(link)) {
            SocketPathEntry.Present -> return link
            is SocketPathEntry.DanglingLink -> throw credentialDaemonNotRunning(
                link,
                "it is a symbolic link to `${entry.target.stripControlChars()}`, which does not exist",
            )
            SocketPathEntry.Missing -> {}
        }
    }

    throw credentialDaemonNoSocket()
}

/**
 * Build the `credential/get` request line, including the terminating `\n`
 */
internal fun credentialDaemonRequest(name: String, accountId: String?, roleName: String?): ByteArray {
    val writer = jsonStreamWriter().apply {
        beginObject()
        writeName("jsonrpc")
        writeValue("2.0")
        writeName("id")
        writeValue(1)
        writeName("method")
        writeValue("credential/get")
        writeName("params")
        beginObject()
        writeName("credential")
        writeValue(name)
        if (accountId != null) {
            writeName("account_id")
            writeValue(accountId)
        }
        if (roleName != null) {
            writeName("role_name")
            writeValue(roleName)
        }
        endObject()
        endObject()
    }
    return writer.bytes!! + '\n'.code.toByte()
}

/**
 * Parse a credential daemon reply line (without its `\n`) into [Credentials].
 *
 * Messages of [CredentialDaemonErrorCode.PROTOCOL_ERROR] never include reply content.
 */
internal fun parseCredentialDaemonReply(line: ByteArray, name: String): Credentials {
    try {
        line.decodeToString(throwOnInvalidSequence = true)
    } catch (_: CharacterCodingException) {
        throw credentialDaemonProtocolError("reply is not valid UTF-8")
    }

    val root: Document? = try {
        JsonDeserializer(line).deserializeDocument()
    } catch (_: Exception) {
        throw credentialDaemonProtocolError("reply is not valid JSON")
    }

    val top = root as? Document.Map ?: throw credentialDaemonProtocolError("reply is not a JSON object")
    if ((top["jsonrpc"] as? Document.String)?.value != "2.0") throw credentialDaemonProtocolError("reply `jsonrpc` is not \"2.0\"")

    // a JSON `null` is kept as a `null` value, so a `null` `result` or `error` counts as absent
    val hasResult = top["result"] != null
    val hasError = top["error"] != null
    if (hasResult == hasError) throw credentialDaemonProtocolError("reply must contain exactly one of `result` and `error`")

    if (hasError) throw daemonError(top["error"], name)

    val result = top["result"] as? Document.Map ?: throw credentialDaemonProtocolError("reply `result` is not an object")
    val material = result["material"] as? Document.Map ?: throw credentialDaemonProtocolError("reply `result.material` is not an object")
    val type = (material["type"] as? Document.String)?.value
    if (type.isNullOrEmpty()) throw credentialDaemonProtocolError("missing required field type")

    if (type != "session_credentials") {
        throw CredentialDaemonException(
            CredentialDaemonErrorCode.UNSUPPORTED_MATERIAL,
            "credential daemon returned unsupported material type `${type.stripControlChars().truncateForMessage()}`; update the AWS SDK for Kotlin",
        )
    }

    val accessKeyId = material.requiredString("access_key_id")
    val secretAccessKey = material.requiredString("secret_access_key")
    val expiresAt = result.requiredString("expires_at")
    val sessionToken = material.optionalString("session_token")
    val accountId = material.optionalString("account_id")

    if (!EXPIRES_AT_REGEX.matches(expiresAt)) throw credentialDaemonProtocolError("unparseable expires_at")
    val expiration = try {
        Instant.fromIso8601(expiresAt)
    } catch (_: Exception) {
        throw credentialDaemonProtocolError("unparseable expires_at")
    }

    return credentials(accessKeyId, secretAccessKey, sessionToken, expiration, CREDENTIAL_DAEMON_PROVIDER_NAME, accountId)
}

private fun Document.Map.requiredString(field: String): String {
    val value = (get(field) as? Document.String)?.value
    if (value.isNullOrEmpty()) throw credentialDaemonProtocolError("missing required field $field")
    return value
}

private fun Document.Map.optionalString(field: String): String? = when (val value = get(field)) {
    null -> null
    is Document.String -> value.value.ifEmpty { null }
    else -> throw credentialDaemonProtocolError("field $field is not a string")
}

private fun daemonError(error: Document?, name: String): CredentialDaemonException {
    val obj = error as? Document.Map ?: return credentialDaemonProtocolError("reply `error` is not an object")
    val msg = (obj["message"] as? Document.String)?.value?.stripControlChars()?.ifEmpty { null }
    val dataCode = (((obj["data"] as? Document.Map)?.get("code")) as? Document.String)?.value?.stripControlChars()

    // the daemon's own text goes in a trailing parenthetical, after the SDK's instruction
    fun details(vararg parts: String?): String = parts.filterNotNull().let { if (it.isEmpty()) "" else it.joinToString(", ", " (", ")") }
    val daemonText = msg?.let { "daemon: $it" }

    return when (dataCode) {
        "REAUTH_REQUIRED" -> CredentialDaemonException(
            CredentialDaemonErrorCode.REAUTH_REQUIRED,
            "the credential daemon requires you to sign in again" + details(daemonText),
            dataCode,
        )
        "CREDENTIAL_NOT_FOUND" -> CredentialDaemonException(
            CredentialDaemonErrorCode.CREDENTIAL_NOT_FOUND,
            "this user has no credential named `$name`; sign in again" + details(daemonText),
            dataCode,
        )
        else -> CredentialDaemonException(
            CredentialDaemonErrorCode.REQUEST_REJECTED,
            "the credential daemon rejected the request" + details(dataCode?.let { "code: $it" }, daemonText),
            dataCode,
        )
    }
}
