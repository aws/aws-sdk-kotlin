/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials.internal

import java.net.SocketAddress
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.SocketChannel

/**
 * Direct calls to the Java 16 Unix domain socket API.
 *
 * This file is compiled against the JDK 17 class library but emits Java 8 bytecode. On a JVM older than 16 the
 * classes it references don't exist, so loading this object fails with a [LinkageError]. Only touch it after
 * [ensureLinked] has succeeded once.
 *
 * Keep every reference to a Java 9+ API in this compilation, and keep it free of SDK dependencies.
 */
internal object Jdk16UnixDomainSockets {
    /**
     * Resolve every Java 16 symbol this object uses. Throws a [LinkageError] if the running JVM lacks them.
     */
    fun ensureLinked() {
        // both are needed: the compiler drops a bare class literal, and on Java 8 to 15 the first missing symbol
        // throws. `of` only builds an address; it does no I/O.
        StandardProtocolFamily.UNIX
        UnixDomainSocketAddress.of("/")
    }

    /**
     * Create a Unix domain socket address for [path]. Throws [java.nio.file.InvalidPathException] if the JDK rejects
     * the path.
     */
    fun address(path: String): SocketAddress = UnixDomainSocketAddress.of(path)

    /**
     * Open an unconnected Unix domain socket channel
     */
    fun openChannel(): SocketChannel = SocketChannel.open(StandardProtocolFamily.UNIX)
}
