/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.crt.http.HttpClientConnectionManager
import aws.sdk.kotlin.crt.http.HttpClientConnectionManagerOptionsBuilder
import aws.sdk.kotlin.crt.io.ClientBootstrap
import aws.sdk.kotlin.crt.io.SocketOptions
import aws.sdk.kotlin.crt.io.TlsContext
import aws.sdk.kotlin.crt.io.Uri
import aws.sdk.kotlin.runtime.config.imds.EndpointConfiguration
import aws.sdk.kotlin.runtime.config.imds.ImdsClient
import aws.smithy.kotlin.runtime.client.endpoints.Endpoint
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.SdkHttpClient
import aws.smithy.kotlin.runtime.http.complete
import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.url
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import libcrt.AWS_OP_SUCCESS
import libcrt.aws_thread_join_all_managed
import libcrt.aws_thread_set_managed_join_timeout_ns
import platform.posix.getenv
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.seconds

/**
 * Repro for the Kotlin/Native exit hang: after a CRT HTTP connection attempt fails and everything is closed, CRT's
 * managed thread count must drain, or `aws_thread_join_all_managed` (run by the CRT exit handler) waits forever.
 *
 * Each test runs one failed request at one layer, closes everything, then joins all managed threads with a timeout,
 * so a stuck count fails the test instead of hanging the process. Run one test per process, since a leak in one test
 * makes every later join fail too:
 *
 *     CRT_TEARDOWN_REPRO=1 test.kexe --ktest_filter='*CrtTeardownReproTest.crtConnectionManagerUnreachable'
 *
 * Without `CRT_TEARDOWN_REPRO` set, every test returns at once, so the normal suite makes no network calls.
 * `REPRO_HOST` overrides the unreachable host. The default, `192.0.2.1` (TEST-NET-1), makes connects time out.
 */
@OptIn(ExperimentalForeignApi::class)
class CrtTeardownReproTest {
    init {
        initCrtLoggingFromEnv()
    }

    private val enabled = getenv("CRT_TEARDOWN_REPRO") != null

    private val unreachableHost = getenv("REPRO_HOST")?.toKString() ?: "192.0.2.1"

    @Test
    fun crtConnectionManagerUnreachable() = repro { crtConnectionManager("http://$unreachableHost") }

    @Test
    fun crtConnectionManagerRefused() = repro { crtConnectionManager("http://127.0.0.1:1") }

    @Test
    fun crtHttpEngineUnreachable() = repro {
        CrtHttpEngine { connectTimeout = 1.seconds }.use { engine ->
            val request = HttpRequest {
                method = HttpMethod.GET
                url(Url.parse("http://$unreachableHost/"))
            }
            assertFails { SdkHttpClient(engine).call(request).complete() }
        }
        assertManagedThreadsJoin()
    }

    @Test
    fun imdsClientUnreachable() = repro {
        ImdsClient {
            endpointConfiguration = EndpointConfiguration.Custom(Endpoint("http://$unreachableHost"))
        }.use { client ->
            assertFails { client.get("/latest/meta-data/placement/region") }
        }
        assertManagedThreadsJoin()
    }

    /**
     * Control: an engine that is never closed must fail the join, or the other tests prove nothing. The join timeout
     * is left set so the CRT exit handler gives up instead of hanging.
     */
    @Test
    fun controlLeakedEngineFailsJoin() = repro {
        val engine = CrtHttpEngine { connectTimeout = 1.seconds }
        val request = HttpRequest {
            method = HttpMethod.GET
            url(Url.parse("http://$unreachableHost/"))
        }
        assertFails { SdkHttpClient(engine).call(request).complete() }
        aws_thread_set_managed_join_timeout_ns(5.seconds.inWholeNanoseconds.toULong())
        assertNotEquals(AWS_OP_SUCCESS, aws_thread_join_all_managed(), "join drained even though the engine leaked")
    }

    private suspend fun crtConnectionManager(uri: String) {
        val bootstrap = ClientBootstrap()
        val tlsContext = TlsContext()
        val manager = HttpClientConnectionManager(
            HttpClientConnectionManagerOptionsBuilder().apply {
                this.uri = Uri.parse(uri)
                clientBootstrap = bootstrap
                this.tlsContext = tlsContext
                socketOptions = SocketOptions(connectTimeoutMs = 1_000)
            }.build(),
        )
        assertFails { manager.acquireConnection() }

        manager.close()
        manager.waitForShutdown()
        tlsContext.close()
        bootstrap.close()
        bootstrap.waitForShutdown()
        assertManagedThreadsJoin()
    }

    private fun repro(block: suspend () -> Unit) {
        if (enabled) runBlocking { block() }
    }

    private fun assertManagedThreadsJoin() {
        aws_thread_set_managed_join_timeout_ns(10.seconds.inWholeNanoseconds.toULong())
        try {
            assertEquals(AWS_OP_SUCCESS, aws_thread_join_all_managed(), "CRT managed threads did not drain within 10s")
        } finally {
            aws_thread_set_managed_join_timeout_ns(0u)
        }
    }
}
