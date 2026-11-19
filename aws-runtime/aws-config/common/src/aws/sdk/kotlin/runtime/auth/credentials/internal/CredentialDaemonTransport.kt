/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials.internal

/**
 * Open one connection to the credential daemon at [socketPath], send [request] and read one reply line.
 *
 * @param socketPath the Unix domain socket path
 * @param request the request line, including its terminating `\n`
 * @param connectTimeoutMillis how long to wait for the connection to be established
 * @param callTimeoutMillis how long the whole exchange may take
 * @return the reply line without its terminating `\n`
 */
internal expect suspend fun credentialDaemonExchange(
    socketPath: String,
    request: ByteArray,
    connectTimeoutMillis: Long,
    callTimeoutMillis: Long,
): ByteArray

/**
 * Throw an unsupported-platform [aws.sdk.kotlin.runtime.auth.credentials.CredentialDaemonException] if this
 * platform or runtime can't open a Unix domain socket
 */
internal expect fun checkCredentialDaemonTransportSupported()

/**
 * What is at [path], checked with `lstat`: the link at [path] itself is not followed, but a link's target is
 * checked for existence
 */
internal expect fun socketPathEntry(path: String): SocketPathEntry
