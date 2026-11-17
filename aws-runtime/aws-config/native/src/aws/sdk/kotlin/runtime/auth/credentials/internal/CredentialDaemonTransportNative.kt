/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials.internal

import aws.sdk.kotlin.runtime.auth.credentials.credentialDaemonUnsupportedPlatform

internal actual suspend fun credentialDaemonExchange(
    socketPath: String,
    request: ByteArray,
    connectTimeoutMillis: Long,
    callTimeoutMillis: Long,
): ByteArray = throw credentialDaemonUnsupportedPlatform("Kotlin/Native is not supported")

internal actual fun checkCredentialDaemonTransportSupported(): Unit = throw credentialDaemonUnsupportedPlatform("Kotlin/Native is not supported")

internal actual fun socketPathEntry(path: String): SocketPathEntry = SocketPathEntry.Missing
