/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.Jdk16UnixDomainSockets
import org.junit.jupiter.api.Assumptions
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A fake credential daemon on a temporary Unix domain socket. Test code is compiled against the JDK 17 API, and the
 * Java 16 socket calls live in [Jdk16UnixDomainSockets] and [Jdk16TestSockets] so that this class still loads on
 * older JVMs. Tests that create one are skipped on JVMs without Unix domain socket support.
 *
 * @param socketPath where to bind, a fresh temporary path by default
 * @param accept whether to accept connections at all; `false` leaves them in the backlog
 * @param backlog the listen backlog
 * @param handler what to do with each connection
 */
class FakeCredentialDaemon(
    socketPath: String? = null,
    accept: Boolean = true,
    backlog: Int = 50,
    private val handler: (String) -> Action = { Action.Reply(sessionReply()) },
) : Closeable {
    sealed class Action {
        /** Write these bytes and close */
        data class Reply(val bytes: ByteArray) : Action() {
            constructor(text: String) : this(text.toByteArray())
        }

        /** Never reply, wait until the client closes the connection */
        object Hang : Action()

        /** Close the connection without replying */
        object Close : Action()
    }

    companion object {
        val unixSocketsSupported: Boolean by lazy {
            try {
                Jdk16UnixDomainSockets.ensureLinked()
                true
            } catch (_: LinkageError) {
                false
            }
        }

        fun assumeSupported() {
            Assumptions.assumeTrue(unixSocketsSupported, "the JDK does not support Unix domain sockets")
        }

        fun tempDir(): File = Files.createTempDirectory(Paths.get("/tmp"), "cd").toFile().apply { deleteOnExit() }

        fun address(path: String): SocketAddress = Jdk16UnixDomainSockets.address(path)

        fun openServerChannel(): ServerSocketChannel = Jdk16TestSockets.openServerChannel()

        fun openClientChannel(): SocketChannel = Jdk16UnixDomainSockets.openChannel()

        fun sessionReply(
            accessKeyId: String = "ASIADAEMON",
            expiresAt: String = "2030-01-01T00:00:00Z",
            extraMaterial: String = "",
        ): String = """{"jsonrpc":"2.0","id":1,"result":{"credential":"dev","material":{"type":"session_credentials",""" +
            """"access_key_id":"$accessKeyId","secret_access_key":"secret","session_token":"token"$extraMaterial},""" +
            """"expires_at":"$expiresAt"}}""" + "\n"
    }

    val path: String
    private val server: ServerSocketChannel
    private val thread: Thread?
    private val fillers = mutableListOf<SocketChannel>()

    /** The request lines received, without `\n` */
    val requests: MutableList<String> = CopyOnWriteArrayList()

    /** Counts down each time a hanging connection sees EOF from the client */
    val clientClosed = CountDownLatch(1)

    init {
        assumeSupported()
        path = socketPath ?: File(tempDir(), "d.sock").absolutePath
        server = openServerChannel()
        server.bind(address(path), backlog)
        thread = if (accept) {
            Thread({ acceptLoop() }, "fake-credential-daemon").apply {
                isDaemon = true
                start()
            }
        } else {
            null
        }
    }

    private fun acceptLoop() {
        while (true) {
            val ch = try {
                server.accept()
            } catch (_: IOException) {
                return
            }
            Thread {
                ch.use { serve(it) }
            }.apply {
                isDaemon = true
                start()
            }
        }
    }

    private fun serve(ch: SocketChannel) {
        val request = try {
            readLine(ch)
        } catch (_: IOException) {
            return
        } ?: return
        requests.add(request)
        when (val action = handler(request)) {
            is Action.Reply -> {
                val buf = ByteBuffer.wrap(action.bytes)
                try {
                    while (buf.hasRemaining()) ch.write(buf)
                } catch (_: IOException) {
                }
            }
            Action.Hang -> {
                val buf = ByteBuffer.allocate(16)
                try {
                    while (ch.read(buf.apply { clear() }) >= 0) { }
                } catch (_: IOException) {
                }
                clientClosed.countDown()
            }
            Action.Close -> {}
        }
    }

    private fun readLine(ch: SocketChannel): String? {
        val out = ByteArrayOutputStream()
        val buf = ByteBuffer.allocate(1)
        while (true) {
            buf.clear()
            if (ch.read(buf) < 0) return null
            val b = buf.get(0)
            if (b == '\n'.code.toByte()) return out.toString("UTF-8")
            out.write(b.toInt())
        }
    }

    /**
     * Fill the listen backlog with pending connections, so a further blocking connect stalls. Linux only.
     */
    fun fillBacklog() {
        repeat(1000) {
            val ch = openClientChannel()
            ch.configureBlocking(false)
            try {
                ch.connect(address(path))
                fillers.add(ch)
            } catch (_: IOException) {
                ch.close()
                return
            }
        }
        error("backlog never filled")
    }

    /**
     * Stop listening and leave the socket file behind, like a daemon that died
     */
    fun stopListening() {
        server.close()
    }

    override fun close() {
        fillers.forEach { it.close() }
        server.close()
        File(path).delete()
    }
}

/**
 * Java 16 socket calls only the tests need. Kept out of [FakeCredentialDaemon] so that class loads on older JVMs.
 */
private object Jdk16TestSockets {
    fun openServerChannel(): ServerSocketChannel = ServerSocketChannel.open(java.net.StandardProtocolFamily.UNIX)
}
