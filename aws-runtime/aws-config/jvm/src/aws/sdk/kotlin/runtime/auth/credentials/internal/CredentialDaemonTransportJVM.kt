/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials.internal

import aws.sdk.kotlin.runtime.auth.credentials.credentialDaemonNotRunning
import aws.sdk.kotlin.runtime.auth.credentials.credentialDaemonProtocolError
import aws.sdk.kotlin.runtime.auth.credentials.credentialDaemonUnsupportedPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.LinkOption
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.time.Duration.Companion.milliseconds

private const val UNSUPPORTED_JDK_MESSAGE = "Unix domain sockets require Java 16 or newer"

/**
 * The Unix domain socket operations the transport needs, replaceable in tests
 */
internal interface UnixSocketApi {
    /**
     * Create a Unix domain socket address for [path]
     */
    fun address(path: String): SocketAddress

    /**
     * Open an unconnected Unix domain socket channel
     */
    fun openChannel(): SocketChannel
}

/**
 * [UnixSocketApi] backed by the Java 16 API. This adapter references [Jdk16UnixDomainSockets] only inside its method
 * bodies, so it loads on any JVM; its methods must only be called once [Jdk16UnixDomainSockets.ensureLinked] succeeded.
 */
private object Jdk16UnixSocketApi : UnixSocketApi {
    override fun address(path: String): SocketAddress = Jdk16UnixDomainSockets.address(path)
    override fun openChannel(): SocketChannel = Jdk16UnixDomainSockets.openChannel()
}

/**
 * Unix domain sockets for the credential daemon transport.
 *
 * The Java 16 API is checked once per instance by calling [link]. A [LinkageError] from it means the running JVM is
 * older than 16, and every call then raises an unsupported-platform error. Socket paths are never cached.
 *
 * @param link resolves the Java 16 API, replaceable in tests
 */
internal class JvmUnixSockets(
    private val link: () -> UnixSocketApi = {
        Jdk16UnixDomainSockets.ensureLinked()
        Jdk16UnixSocketApi
    },
) {
    private val api: Result<UnixSocketApi> by lazy {
        try {
            Result.success(link())
        } catch (ex: LinkageError) {
            Result.failure(ex)
        }
    }

    /**
     * Throw an unsupported-platform error if this JVM has no Unix domain socket API
     */
    fun checkSupported() {
        api()
    }

    private fun api(): UnixSocketApi = api.getOrElse { throw credentialDaemonUnsupportedPlatform(UNSUPPORTED_JDK_MESSAGE, it) }

    /**
     * Create a Unix domain socket address for [path]
     */
    fun address(path: String): SocketAddress = try {
        api().address(path)
    } catch (ex: InvalidPathException) {
        throw credentialDaemonNotRunning(path, "invalid socket path: ${ex.message}", ex)
    }

    /**
     * Open an unconnected Unix domain socket channel, used to connect to [path]
     */
    fun open(path: String): SocketChannel = try {
        api().openChannel()
    } catch (ex: UnsupportedOperationException) {
        throw credentialDaemonUnsupportedPlatform(UNSUPPORTED_JDK_MESSAGE, ex)
    } catch (ex: IOException) {
        throw credentialDaemonNotRunning(path, ex.message, ex)
    }

    companion object {
        val Default: JvmUnixSockets = JvmUnixSockets()
    }
}

internal actual suspend fun credentialDaemonExchange(
    socketPath: String,
    request: ByteArray,
    connectTimeoutMillis: Long,
    callTimeoutMillis: Long,
): ByteArray = exchange(JvmUnixSockets.Default, socketPath, request, connectTimeoutMillis, callTimeoutMillis)

internal suspend fun exchange(
    sockets: JvmUnixSockets,
    path: String,
    request: ByteArray,
    connectTimeoutMillis: Long,
    callTimeoutMillis: Long,
): ByteArray {
    val address = sockets.address(path)
    val ch = sockets.open(path)
    try {
        // real-time dispatcher so that virtual time in tests can not fire the timeouts early
        return withContext(Dispatchers.IO) {
            val line = withTimeoutOrNull(callTimeoutMillis.milliseconds) {
                val connected = withTimeoutOrNull(connectTimeoutMillis.milliseconds) {
                    try {
                        runInterruptible { ch.connect(address) }
                    } catch (ex: IOException) {
                        // a timeout interrupts the blocked call, let the timeout win over the I/O error
                        currentCoroutineContext().ensureActive()
                        throw credentialDaemonNotRunning(path, ex.message, ex)
                    }
                    true
                }
                if (connected == null) throw credentialDaemonNotRunning(path, "connect timeout of $connectTimeoutMillis ms elapsed")

                try {
                    runInterruptible {
                        writeAll(ch, request)
                        readLine(ch)
                    }
                } catch (ex: IOException) {
                    currentCoroutineContext().ensureActive()
                    throw credentialDaemonProtocolError("connection closed before a complete reply")
                }
            }
            line ?: throw credentialDaemonNotRunning(path, "call timeout of $callTimeoutMillis ms elapsed")
        }
    } finally {
        ch.close()
    }
}

private fun writeAll(ch: SocketChannel, bytes: ByteArray) {
    val buf = ByteBuffer.wrap(bytes)
    while (buf.hasRemaining()) {
        ch.write(buf)
    }
}

/**
 * Read bytes until the first `\n` and return those before it. Stop reading as soon as more than
 * [CREDENTIAL_DAEMON_MAX_REPLY_BYTES] bytes arrive without a `\n`.
 */
private fun readLine(ch: SocketChannel): ByteArray {
    val out = ByteArrayOutputStream()
    val buf = ByteBuffer.allocate(4096)
    while (true) {
        buf.clear()
        val n = ch.read(buf)
        if (n < 0) throw credentialDaemonProtocolError("connection closed before a complete reply")
        val arr = buf.array()
        for (i in 0 until n) {
            val b = arr[i]
            if (b == '\n'.code.toByte()) return out.toByteArray()
            out.write(b.toInt())
            if (out.size() > CREDENTIAL_DAEMON_MAX_REPLY_BYTES) {
                throw credentialDaemonProtocolError("reply exceeds $CREDENTIAL_DAEMON_MAX_REPLY_BYTES bytes")
            }
        }
    }
}

internal actual fun checkCredentialDaemonTransportSupported() {
    JvmUnixSockets.Default.checkSupported()
}

internal actual fun socketPathEntry(path: String): SocketPathEntry {
    val p = try {
        Paths.get(path)
    } catch (_: InvalidPathException) {
        return SocketPathEntry.Missing
    }
    val attrs = try {
        Files.readAttributes(p, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
    } catch (_: IOException) {
        return SocketPathEntry.Missing
    }
    if (!attrs.isSymbolicLink || Files.exists(p)) return SocketPathEntry.Present
    val target = try {
        Files.readSymbolicLink(p).toString()
    } catch (_: IOException) {
        "<unreadable link target>"
    }
    return SocketPathEntry.DanglingLink(target)
}
