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
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.net.ProtocolFamily
import java.net.SocketAddress
import java.net.StandardProtocolFamily
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
 * Access to JDK 16+ Unix domain socket APIs through reflection, so this code still compiles for and runs on
 * older JDKs. Reflection handles are loaded once per instance; socket paths are never cached.
 *
 * @param forName the function used to load classes by name, replaceable in tests
 */
internal class JvmUnixSockets(private val forName: (String) -> Class<*> = { Class.forName(it) }) {
    private class Handles(val addressOf: Method, val family: ProtocolFamily, val openChannel: Method)

    private val handles: Result<Handles> by lazy {
        try {
            val addressOf = forName("java.net.UnixDomainSocketAddress").getMethod("of", String::class.java)
            val family = StandardProtocolFamily.valueOf("UNIX")
            val openChannel = SocketChannel::class.java.getMethod("open", ProtocolFamily::class.java)
            Result.success(Handles(addressOf, family, openChannel))
        } catch (ex: ReflectiveOperationException) {
            Result.failure(ex)
        } catch (ex: IllegalArgumentException) {
            Result.failure(ex)
        } catch (ex: UnsupportedOperationException) {
            Result.failure(ex)
        }
    }

    /**
     * Throw an unsupported-platform error if this JDK has no Unix domain socket APIs
     */
    fun checkSupported() {
        loadHandles()
    }

    private fun loadHandles(): Handles = handles.getOrElse { throw credentialDaemonUnsupportedPlatform(UNSUPPORTED_JDK_MESSAGE, it) }

    /**
     * Create a Unix domain socket address for [path]
     */
    fun address(path: String): SocketAddress {
        val h = loadHandles()
        return try {
            h.addressOf.invoke(null, path) as SocketAddress
        } catch (ex: InvocationTargetException) {
            when (val cause = ex.cause) {
                is InvalidPathException -> throw credentialDaemonNotRunning(path, "invalid socket path: ${cause.message}", cause)
                else -> throw credentialDaemonNotRunning(path, cause?.message, cause ?: ex)
            }
        }
    }

    /**
     * Open an unconnected Unix domain socket channel, used to connect to [path]
     */
    fun open(path: String): SocketChannel {
        val h = loadHandles()
        return try {
            h.openChannel.invoke(null, h.family) as SocketChannel
        } catch (ex: InvocationTargetException) {
            when (val cause = ex.cause) {
                is UnsupportedOperationException -> throw credentialDaemonUnsupportedPlatform(UNSUPPORTED_JDK_MESSAGE, cause)
                is IOException -> throw credentialDaemonNotRunning(path, cause.message, cause)
                else -> throw cause ?: ex
            }
        }
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
