/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.FakeCredentialDaemon.Action
import aws.sdk.kotlin.runtime.auth.credentials.internal.JvmUnixSockets
import aws.sdk.kotlin.runtime.auth.credentials.internal.SocketPathEntry
import aws.sdk.kotlin.runtime.auth.credentials.internal.credentialDaemonRequest
import aws.sdk.kotlin.runtime.auth.credentials.internal.exchange
import aws.sdk.kotlin.runtime.auth.credentials.internal.socketPathEntry
import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.CachedCredentialsProvider
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetrics
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import aws.smithy.kotlin.runtime.util.OperatingSystem
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assumptions
import java.io.File
import java.net.SocketAddress
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal fun fakePlatform(
    env: Map<String, String>,
    os: OsFamily = OsFamily.Linux,
    android: Boolean = false,
): PlatformProvider = object : PlatformProvider by PlatformProvider.System {
    override fun getenv(key: String): String? = env[key]
    override fun getAllEnvVars(): Map<String, String> = env
    override fun getProperty(key: String): String? = null
    override fun getAllProperties(): Map<String, String> = emptyMap()
    override fun osInfo(): OperatingSystem = OperatingSystem(os, "test")
    override val isAndroid: Boolean = android
}

class CredentialDaemonCredentialsProviderTest {
    private fun envFor(daemon: FakeCredentialDaemon) = mapOf("AWS_CREDENTIAL_DAEMON_SOCKET" to daemon.path)

    private suspend fun assertDaemonError(
        code: CredentialDaemonErrorCode,
        provider: CredentialDaemonCredentialsProvider,
    ): CredentialDaemonException {
        val ex = assertFailsWith<CredentialDaemonException> { provider.resolve() }
        assertEquals(code, ex.errorCode, ex.message)
        return ex
    }

    @Test
    fun testSuccess() = runTest {
        FakeCredentialDaemon(handler = { Action.Reply(FakeCredentialDaemon.sessionReply(extraMaterial = ""","account_id":"123456789012"""")) }).use { daemon ->
            val creds = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(envFor(daemon))).resolve()
            assertEquals("ASIADAEMON", creds.accessKeyId)
            assertEquals("secret", creds.secretAccessKey)
            assertEquals("token", creds.sessionToken)
            assertEquals("123456789012", creds.attributes.getOrNull(AwsClientOption.AccountId))
            val metrics = creds.attributes.getOrNull(BusinessMetrics)
            assertTrue(metrics != null && AwsBusinessMetric.Credentials.CREDENTIAL_DAEMON in metrics, "metrics: $metrics")
            assertEquals("Aq", AwsBusinessMetric.Credentials.CREDENTIAL_DAEMON.identifier)

            assertEquals(1, daemon.requests.size)
            val request = Json.parseToJsonElement(daemon.requests.single()).jsonObject
            assertEquals("credential/get", request["method"]!!.jsonPrimitive.content)
            assertEquals("dev", request["params"]!!.jsonObject["credential"]!!.jsonPrimitive.content)
        }
    }

    @Test
    fun testSsoParams() = runTest {
        FakeCredentialDaemon().use { daemon ->
            CredentialDaemonCredentialsProvider("dev", "123456789012", "Admin", fakePlatform(envFor(daemon))).resolve()
            val params = Json.parseToJsonElement(daemon.requests.single()).jsonObject["params"]!!.jsonObject
            assertEquals("123456789012", params["account_id"]!!.jsonPrimitive.content)
            assertEquals("Admin", params["role_name"]!!.jsonPrimitive.content)
        }
    }

    /**
     * A temporary `HOME` with an empty `.aws` folder, and the location 2 path under it
     */
    private fun tempHome(): Pair<File, File> {
        val home = FakeCredentialDaemon.tempDir()
        val aws = File(home, ".aws").apply { mkdirs() }
        return home to File(aws, "credential-daemon.sock")
    }

    @Test
    fun testLinkToSocket() = runTest {
        val (home, link) = tempHome()
        FakeCredentialDaemon().use { daemon ->
            Files.createSymbolicLink(link.toPath(), Paths.get(daemon.path))
            val env = mapOf("HOME" to home.absolutePath)
            assertEquals("ASIADAEMON", CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(env)).resolve().accessKeyId)
            assertEquals(1, daemon.requests.size)
        }
    }

    @Test
    fun testLinkIsASocket() = runTest {
        val (home, link) = tempHome()
        FakeCredentialDaemon(socketPath = link.absolutePath).use { daemon ->
            val env = mapOf("HOME" to home.absolutePath)
            assertEquals("ASIADAEMON", CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(env)).resolve().accessKeyId)
            assertEquals(1, daemon.requests.size)
        }
    }

    @Test
    fun testStaleLinkNamesTheLinkAndDoesNotFallBack() = runTest {
        val (home, link) = tempHome()
        FakeCredentialDaemon().use { daemon ->
            Files.createSymbolicLink(link.toPath(), Paths.get(daemon.path))
            daemon.stopListening()
            assertTrue(File(daemon.path).exists(), "socket file should be left behind")
            val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(mapOf("HOME" to home.absolutePath)))
            val ex = assertDaemonError(CredentialDaemonErrorCode.NOT_RUNNING, provider)
            assertTrue(ex.message!!.contains(link.absolutePath), ex.message)
            assertFalse(ex.message!!.contains(daemon.path), ex.message)
        }
    }

    @Test
    fun testDanglingLink() = runTest {
        FakeCredentialDaemon.assumeSupported()
        val (home, link) = tempHome()
        val target = File(FakeCredentialDaemon.tempDir(), "missing-daemon.sock").absolutePath
        Files.createSymbolicLink(link.toPath(), Paths.get(target))
        val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(mapOf("HOME" to home.absolutePath)))
        val ex = assertDaemonError(CredentialDaemonErrorCode.NOT_RUNNING, provider)
        assertTrue(ex.message!!.contains(link.absolutePath), ex.message)
        assertTrue(ex.message!!.contains(target), ex.message)
        assertTrue(ex.message!!.contains("does not exist"), ex.message)
    }

    @Test
    fun testRegularFileAtLinkPathIsDialed() = runTest {
        FakeCredentialDaemon.assumeSupported()
        val (home, link) = tempHome()
        link.writeText("not a socket")
        val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(mapOf("HOME" to home.absolutePath)))
        val ex = assertDaemonError(CredentialDaemonErrorCode.NOT_RUNNING, provider)
        assertTrue(ex.message!!.contains(link.absolutePath), ex.message)
        assertFalse(ex.message!!.contains("could not find the credential daemon"), ex.message)
    }

    @Test
    fun testEnvFileIsDialed() = runTest {
        FakeCredentialDaemon.assumeSupported()
        val file = File(FakeCredentialDaemon.tempDir(), "env.sock").apply { writeText("not a socket") }
        val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(mapOf("AWS_CREDENTIAL_DAEMON_SOCKET" to file.absolutePath)))
        val ex = assertDaemonError(CredentialDaemonErrorCode.NOT_RUNNING, provider)
        assertTrue(ex.message!!.contains(file.absolutePath), ex.message)
    }

    @Test
    fun testNoLink() = runTest {
        FakeCredentialDaemon.assumeSupported()
        val (home, _) = tempHome()
        val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(mapOf("HOME" to home.absolutePath)))
        val ex = assertDaemonError(CredentialDaemonErrorCode.NOT_RUNNING, provider)
        assertTrue(ex.message!!.contains("\$HOME/.aws/credential-daemon.sock"), ex.message)
        assertTrue(ex.message!!.contains("AWS_CREDENTIAL_DAEMON_SOCKET"), ex.message)
        assertTrue(ex.message!!.contains(CREDENTIAL_DAEMON_DOC), ex.message)

        // no HOME, and a relative HOME
        assertDaemonError(CredentialDaemonErrorCode.NOT_RUNNING, CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(emptyMap())))
        assertDaemonError(
            CredentialDaemonErrorCode.NOT_RUNNING,
            CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(mapOf("HOME" to "relative/home"))),
        )
    }

    /**
     * Runs only on a JDK without Unix domain sockets (older than 16), where the real transport must report
     * `UNSUPPORTED_PLATFORM` before it looks for a socket.
     */
    @Test
    fun testOldJdkIsUnsupportedPlatform() = runTest {
        Assumptions.assumeFalse(FakeCredentialDaemon.unixSocketsSupported, "the JDK supports Unix domain sockets")
        val ex = assertDaemonError(
            CredentialDaemonErrorCode.UNSUPPORTED_PLATFORM,
            CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(emptyMap())),
        )
        assertTrue(ex.cause is ClassNotFoundException, "cause: ${ex.cause}")
    }

    @Test
    fun testSocketPathEntry() {
        val (_, link) = tempHome()
        assertEquals(SocketPathEntry.Missing, socketPathEntry(link.absolutePath))
        link.writeText("x")
        assertEquals(SocketPathEntry.Present, socketPathEntry(link.absolutePath))
        link.delete()
        val target = File(FakeCredentialDaemon.tempDir(), "missing-daemon.sock")
        Files.createSymbolicLink(link.toPath(), target.toPath())
        assertEquals(SocketPathEntry.DanglingLink(target.absolutePath), socketPathEntry(link.absolutePath))
        target.writeText("now it exists")
        assertEquals(SocketPathEntry.Present, socketPathEntry(link.absolutePath))
        assertEquals(SocketPathEntry.Missing, socketPathEntry("/tmp/a\u0000b"))
    }

    @Test
    fun testEnvWinsOverLink() = runTest {
        val (home, link) = tempHome()
        FakeCredentialDaemon(socketPath = link.absolutePath).use { atLink ->
            FakeCredentialDaemon(handler = { Action.Reply(FakeCredentialDaemon.sessionReply("ASIAENV")) }).use { fromEnv ->
                val env = mapOf("HOME" to home.absolutePath, "AWS_CREDENTIAL_DAEMON_SOCKET" to fromEnv.path)
                assertEquals("ASIAENV", CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(env)).resolve().accessKeyId)
                assertEquals(0, atLink.requests.size)
            }
        }
    }

    @Test
    fun testPathResolvedOnEveryCall() = runTest {
        FakeCredentialDaemon(handler = { Action.Reply(FakeCredentialDaemon.sessionReply("ASIAONE")) }).use { one ->
            FakeCredentialDaemon(handler = { Action.Reply(FakeCredentialDaemon.sessionReply("ASIATWO")) }).use { two ->
                val env = mutableMapOf("AWS_CREDENTIAL_DAEMON_SOCKET" to one.path)
                val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(env))
                assertEquals("ASIAONE", provider.resolve().accessKeyId)
                env["AWS_CREDENTIAL_DAEMON_SOCKET"] = two.path
                assertEquals("ASIATWO", provider.resolve().accessKeyId)
            }
        }
    }

    @Test
    fun testMissingSocket() = runTest {
        FakeCredentialDaemon.assumeSupported()
        val path = File(FakeCredentialDaemon.tempDir(), "missing.sock").absolutePath
        val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(mapOf("AWS_CREDENTIAL_DAEMON_SOCKET" to path)))
        val ex = assertDaemonError(CredentialDaemonErrorCode.NOT_RUNNING, provider)
        assertTrue(ex.message!!.contains(path), ex.message)
        assertTrue(ex.message!!.contains(CREDENTIAL_DAEMON_DOC), ex.message)
        assertTrue(ex.message!!.contains("No such file or directory"), ex.message)
    }

    @Test
    fun testConnectTimeout() = runTest {
        Assumptions.assumeTrue(System.getProperty("os.name").startsWith("Linux"), "needs Linux backlog behaviour")
        FakeCredentialDaemon(accept = false, backlog = 1).use { daemon ->
            daemon.fillBacklog()
            val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(envFor(daemon)), connectTimeoutMillis = 200, callTimeoutMillis = 2_000)
            val ex = assertDaemonError(CredentialDaemonErrorCode.NOT_RUNNING, provider)
            assertTrue(ex.message!!.contains("connect timeout of 200 ms"), ex.message)
        }
    }

    @Test
    fun testCallTimeoutDuringConnect() = runTest {
        Assumptions.assumeTrue(System.getProperty("os.name").startsWith("Linux"), "needs Linux backlog behaviour")
        FakeCredentialDaemon(accept = false, backlog = 1).use { daemon ->
            daemon.fillBacklog()
            val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(envFor(daemon)), connectTimeoutMillis = 2_000, callTimeoutMillis = 200)
            val ex = assertDaemonError(CredentialDaemonErrorCode.NOT_RUNNING, provider)
            assertTrue(ex.message!!.contains("call timeout of 200 ms"), ex.message)
        }
    }

    @Test
    fun testCallTimeoutDuringRead() = runTest {
        FakeCredentialDaemon(handler = { Action.Hang }).use { daemon ->
            val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(envFor(daemon)), callTimeoutMillis = 300)
            val ex = assertDaemonError(CredentialDaemonErrorCode.NOT_RUNNING, provider)
            assertTrue(ex.message!!.contains("call timeout of 300 ms"), ex.message)
            assertTrue(daemon.clientClosed.await(5, TimeUnit.SECONDS), "the daemon should see the connection closed")
        }
    }

    private fun paddedReply(length: Int): String {
        val base = FakeCredentialDaemon.sessionReply(extraMaterial = ""","pad":""""").length - 1
        val reply = FakeCredentialDaemon.sessionReply(extraMaterial = ""","pad":"${"x".repeat(length - base)}"""")
        assertEquals(length + 1, reply.length)
        return reply
    }

    @Test
    fun testReplyAtLimit() = runTest {
        FakeCredentialDaemon(handler = { Action.Reply(paddedReply(64_000)) }).use { daemon ->
            val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(envFor(daemon)))
            assertEquals("ASIADAEMON", provider.resolve().accessKeyId)
        }
    }

    @Test
    fun testReplyOverLimit() = runTest {
        FakeCredentialDaemon(handler = { Action.Reply(paddedReply(64_001)) }).use { daemon ->
            val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(envFor(daemon)))
            val ex = assertDaemonError(CredentialDaemonErrorCode.PROTOCOL_ERROR, provider)
            assertTrue(ex.message!!.contains("exceeds 64000 bytes"), ex.message)
        }
    }

    @Test
    fun testEofBeforeNewline() = runTest {
        FakeCredentialDaemon(handler = { Action.Reply(FakeCredentialDaemon.sessionReply().trimEnd('\n')) }).use { daemon ->
            val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(envFor(daemon)))
            val ex = assertDaemonError(CredentialDaemonErrorCode.PROTOCOL_ERROR, provider)
            assertTrue(ex.message!!.contains("connection closed before a complete reply"), ex.message)
        }
        FakeCredentialDaemon(handler = { Action.Close }).use { daemon ->
            val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(envFor(daemon)))
            assertDaemonError(CredentialDaemonErrorCode.PROTOCOL_ERROR, provider)
        }
    }

    @Test
    fun testDaemonErrorCodes() = runTest {
        val cases = mapOf(
            "REAUTH_REQUIRED" to CredentialDaemonErrorCode.REAUTH_REQUIRED,
            "CREDENTIAL_NOT_FOUND" to CredentialDaemonErrorCode.CREDENTIAL_NOT_FOUND,
            "SOMETHING_ELSE" to CredentialDaemonErrorCode.REQUEST_REJECTED,
        )
        cases.forEach { (daemonCode, expected) ->
            val reply = """{"jsonrpc":"2.0","id":1,"error":{"code":-32000,"message":"m","data":{"code":"$daemonCode"}}}""" + "\n"
            FakeCredentialDaemon(handler = { Action.Reply(reply) }).use { daemon ->
                val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(envFor(daemon)))
                assertEquals(daemonCode, assertDaemonError(expected, provider).daemonErrorCode)
            }
        }

        val unsupported = """{"jsonrpc":"2.0","id":1,"result":{"material":{"type":"x509"}}}""" + "\n"
        FakeCredentialDaemon(handler = { Action.Reply(unsupported) }).use { daemon ->
            val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(envFor(daemon)))
            assertDaemonError(CredentialDaemonErrorCode.UNSUPPORTED_MATERIAL, provider)
        }
    }

    @Test
    fun testInvalidName() = runTest {
        val provider = CredentialDaemonCredentialsProvider(".bad", platformProvider = fakePlatform(mapOf("AWS_CREDENTIAL_DAEMON_SOCKET" to "/nonexistent")))
        assertDaemonError(CredentialDaemonErrorCode.INVALID_CONFIG, provider)
    }

    @Test
    fun testUnsupportedPlatforms() = runTest {
        val env = mapOf("AWS_CREDENTIAL_DAEMON_SOCKET" to "/nonexistent")
        val windows = assertDaemonError(
            CredentialDaemonErrorCode.UNSUPPORTED_PLATFORM,
            CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(env, os = OsFamily.Windows)),
        )
        assertTrue(windows.message!!.contains("windows", ignoreCase = true), windows.message)
        assertTrue(windows.message!!.contains(CREDENTIAL_DAEMON_DOC), windows.message)

        assertDaemonError(
            CredentialDaemonErrorCode.UNSUPPORTED_PLATFORM,
            CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(env, android = true)),
        )

        // JDK < 16: the Unix domain socket classes do not exist
        val oldJdk = JvmUnixSockets(forName = { throw ClassNotFoundException(it) })
        val ex = assertFailsWith<CredentialDaemonException> {
            exchange(oldJdk, "/nonexistent", credentialDaemonRequest("dev", null, null), 1_000, 5_000)
        }
        assertEquals(CredentialDaemonErrorCode.UNSUPPORTED_PLATFORM, ex.errorCode)
        assertTrue(ex.message!!.contains("Java 16"), ex.message)
        assertTrue(ex.message!!.contains(CREDENTIAL_DAEMON_DOC), ex.message)
    }

    @Test
    fun testInvalidPath() = runTest {
        FakeCredentialDaemon.assumeSupported()
        // `UnixDomainSocketAddress.of` stand-in that rejects every path; InvalidPathException is an
        // IllegalArgumentException, which must not be mistaken for a missing JDK API
        val sockets = JvmUnixSockets(forName = { if (it == "java.net.UnixDomainSocketAddress") RejectingAddress::class.java else Class.forName(it) })
        val injected = assertFailsWith<CredentialDaemonException> {
            exchange(sockets, "/bad", credentialDaemonRequest("dev", null, null), 1_000, 5_000)
        }
        assertTrue(injected.message!!.contains("invalid socket path"), injected.message)
        assertEquals(CredentialDaemonErrorCode.NOT_RUNNING, injected.errorCode)

        // a real path the JDK rejects
        val ex = assertFailsWith<CredentialDaemonException> {
            exchange(JvmUnixSockets(), "/tmp/a\u0000b", credentialDaemonRequest("dev", null, null), 1_000, 5_000)
        }
        assertEquals(CredentialDaemonErrorCode.NOT_RUNNING, ex.errorCode)
        assertTrue(ex.message!!.none { it.isISOControl() }, ex.message)
    }

    /**
     * A daemon that sends [first] to the first request and [next] to every later one, with a cache in front of
     * the provider on a [ManualClock]. The first reply expires an hour after the clock's start.
     */
    private fun scriptedCache(next: String, block: suspend (CachedCredentialsProvider, FakeCredentialDaemon) -> Unit) = runTest {
        val start = Instant.fromIso8601("2030-01-01T00:00:00Z")
        val first = FakeCredentialDaemon.sessionReply("ASIAREJECTED", expiresAt = "2030-01-01T01:00:00Z")
        var calls = 0
        FakeCredentialDaemon(handler = { _ -> Action.Reply(if (calls++ == 0) first else next) }).use { daemon ->
            val provider = CredentialDaemonCredentialsProvider("dev", platformProvider = fakePlatform(envFor(daemon)))
            val cached = CachedCredentialsProvider(provider, clock = ManualClock(start))
            assertEquals("ASIAREJECTED", cached.resolve().accessKeyId)
            // the SDK has no Invalidate(), so the case's invalidate step is skipped
            block(cached, daemon)
        }
    }

    @Test
    fun testInvalidatedRefreshes_expectedFailure_untilCredentialRefresh() = scriptedCache(
        FakeCredentialDaemon.sessionReply("ASIADAEMON", expiresAt = "2030-01-01T01:00:00Z"),
    ) { cached, daemon ->
        // needs the Credential Refresh SEP; when it is released in the Kotlin SDK, drop `assertFails` and the suffix.
        assertFails {
            assertEquals("ASIADAEMON", cached.resolve().accessKeyId)
            assertEquals(2, daemon.requests.size)
        }
    }

    @Test
    fun testInvalidatedSameKey_expectedFailure_untilCredentialRefresh() = scriptedCache(
        """{"jsonrpc":"2.0","id":1,"result":{"credential":"dev","material":{"type":"session_credentials",""" +
            """"access_key_id":"ASIAREJECTED","secret_access_key":"s3cr3t","session_token":"token"},""" +
            """"expires_at":"2030-01-01T01:00:00Z"}}""" + "\n",
    ) { cached, _ ->
        // needs the Credential Refresh SEP; when it is released in the Kotlin SDK, drop `assertFails` and the suffix.
        assertFails {
            val ex = assertFailsWith<Throwable> { cached.resolve() }
            assertNull((ex as? CredentialDaemonException)?.errorCode, ex.message)
            assertFalse(ex.message.orEmpty().contains("s3cr3t"), ex.message)
        }
    }

    @Test
    fun testTimeoutsMustBePositive() {
        assertFailsWith<IllegalArgumentException> { CredentialDaemonCredentialsProvider("dev", connectTimeoutMillis = 0) }
        assertFailsWith<IllegalArgumentException> { CredentialDaemonCredentialsProvider("dev", callTimeoutMillis = -1) }
    }
}

class RejectingAddress {
    companion object {
        @JvmStatic
        fun of(path: String): SocketAddress = throw InvalidPathException(path, "rejected for test")
    }
}
