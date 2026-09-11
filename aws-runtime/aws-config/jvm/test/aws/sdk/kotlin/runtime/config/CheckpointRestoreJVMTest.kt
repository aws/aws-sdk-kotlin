/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.smithy.kotlin.runtime.http.HttpCall
import aws.smithy.kotlin.runtime.http.config.EngineFactory
import aws.smithy.kotlin.runtime.http.engine.CloseableHttpClientEngine
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineBase
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineConfig
import aws.smithy.kotlin.runtime.http.engine.HttpEngineConfigImpl
import aws.smithy.kotlin.runtime.http.engine.okhttp.OkHttpEngine
import aws.smithy.kotlin.runtime.http.engine.okhttp4.OkHttp4Engine
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.io.SdkManaged
import aws.smithy.kotlin.runtime.io.SdkManagedCloseable
import aws.smithy.kotlin.runtime.io.SdkManagedGroup
import aws.smithy.kotlin.runtime.io.addIfManaged
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CheckpointRestoreJVMTest {
    @Test
    fun testEngineRestartsAfterRestore() {
        val initialEngine = TestEngine()
        val replacementEngine = TestEngine()
        var replacementCount = 0
        val engine = CheckpointRestoreHttpClientEngine(initialEngine) {
            replacementCount++
            replacementEngine
        }

        assertTrue(engine.coroutineContext[Job]!!.isActive)

        engine.beforeCheckpoint()

        assertFalse(initialEngine.coroutineContext[Job]!!.isActive)
        assertEquals(1, initialEngine.shutdownCount)

        engine.afterRestore()

        assertEquals(1, replacementCount)
        assertTrue(engine.coroutineContext[Job]!!.isActive)

        engine.close()
        runBlocking { replacementEngine.coroutineContext[Job]!!.join() }
        assertEquals(1, replacementEngine.shutdownCount)
    }

    @Test
    fun testClosedEngineDoesNotRestart() {
        val initialEngine = TestEngine()
        var replacementCount = 0
        val engine = CheckpointRestoreHttpClientEngine(initialEngine) {
            replacementCount++
            TestEngine()
        }

        engine.close()
        runBlocking { initialEngine.coroutineContext[Job]!!.join() }
        engine.beforeCheckpoint()
        engine.afterRestore()

        assertEquals(0, replacementCount)
        assertEquals(1, initialEngine.shutdownCount)
    }

    @Test
    fun testSdkManagedDefaultEngineIsWrapped() {
        val builder = HttpEngineConfigImpl.BuilderImpl()

        builder.configureCheckpointRestore(true)

        val engine = assertIs<SdkManagedCheckpointRestoreHttpClientEngine>(builder.buildHttpEngineConfig().httpClient)
        val initialJob = engine.coroutineContext[Job]!!

        engine.beforeCheckpoint()
        assertFalse(initialJob.isActive)
        engine.afterRestore()
        assertTrue(engine.coroutineContext[Job]!!.isActive)
        engine.close()
    }

    @Test
    fun testUnsupportedSdkManagedEngineRetainsManagedOwnership() {
        val factory = UnsupportedManagedEngineFactory()
        val builder = HttpEngineConfigImpl.BuilderImpl().apply {
            httpClient(factory) { }
        }

        builder.configureCheckpointRestore(true)

        val engine = builder.buildHttpEngineConfig().httpClient
        assertIs<SdkManaged>(engine)
        assertEquals(1, factory.engines.size)

        SdkManagedGroup().apply {
            addIfManaged(engine)
            unshareAll()
        }
        assertEquals(1, factory.engines.single().shutdownCount)
    }

    @Test
    fun testProviderOwnedDefaultEngineIsNotSdkManaged() {
        val engine = createCheckpointRestoreAwareDefaultHttpEngine(true)

        assertFalse(engine is SdkManaged)
        assertIs<CheckpointRestoreHttpClientEngine>(engine)
        engine.close()
    }

    @Test
    fun testCallerOwnedEngineIsNotWrapped() {
        val explicitEngine = OkHttpEngine()
        val builder = HttpEngineConfigImpl.BuilderImpl().apply {
            httpClient = explicitEngine
        }

        builder.configureCheckpointRestore(true)

        assertSame(explicitEngine, builder.buildHttpEngineConfig().httpClient)
        explicitEngine.close()
    }

    @Test
    fun testSdkManagedOkHttp4EngineRetainsImplementationAfterRestore() {
        val builder = HttpEngineConfigImpl.BuilderImpl().apply {
            httpClient(OkHttp4Engine) { }
        }

        builder.configureCheckpointRestore(true)

        val engine = assertIs<SdkManagedCheckpointRestoreHttpClientEngine>(builder.buildHttpEngineConfig().httpClient)
        assertEquals("http-client-engine-OkHttp4-context", engine.coroutineContext[CoroutineName]?.name)

        engine.beforeCheckpoint()
        engine.afterRestore()

        assertEquals("http-client-engine-OkHttp4-context", engine.coroutineContext[CoroutineName]?.name)
        engine.close()
    }

    @Test
    fun testCheckpointWaitsForActiveRequests() {
        val initialEngine = TestEngine()
        val child = CoroutineScope(initialEngine.coroutineContext).launch {
            delay(25)
        }
        val engine = CheckpointRestoreHttpClientEngine(initialEngine, replacementFactory = ::TestEngine)

        engine.beforeCheckpoint()

        assertTrue(child.isCompleted)
        assertEquals(1, initialEngine.shutdownCount)
        engine.afterRestore()
        engine.close()
    }

    @Test
    fun testQuiescenceFailureAbortsCheckpointAndRestoresUsableEngine() {
        val initialEngine = TestEngine()
        val replacementEngine = TestEngine()
        val child = CoroutineScope(initialEngine.coroutineContext).launch {
            delay(Long.MAX_VALUE)
        }
        val engine = CheckpointRestoreHttpClientEngine(
            initialEngine,
            replacementFactory = { replacementEngine },
            checkpointQuiescenceTimeoutMillis = 1,
        )

        assertFailsWith<IllegalStateException> { engine.beforeCheckpoint() }
        assertSame(replacementEngine.coroutineContext, engine.coroutineContext)
        assertTrue(engine.coroutineContext[Job]!!.isActive)

        child.cancel()
        engine.close()
        runBlocking {
            initialEngine.coroutineContext[Job]!!.join()
            replacementEngine.coroutineContext[Job]!!.join()
        }
        assertEquals(1, initialEngine.shutdownCount)
        assertEquals(1, replacementEngine.shutdownCount)
    }
}

private class UnsupportedManagedEngineFactory : EngineFactory<HttpClientEngineConfig.Builder, UnsupportedManagedEngine> {
    val engines = mutableListOf<TestEngine>()

    override val engineConstructor: (HttpClientEngineConfig.Builder.() -> Unit) -> UnsupportedManagedEngine = {
        val delegate = TestEngine()
        engines += delegate
        UnsupportedManagedEngine(delegate)
    }
}

private class UnsupportedManagedEngine(
    private val delegate: TestEngine,
) : SdkManagedCloseable(delegate),
    CloseableHttpClientEngine by delegate

private class TestEngine : HttpClientEngineBase("checkpoint-restore-test") {
    override val config: HttpClientEngineConfig = HttpClientEngineConfig.Default
    var shutdownCount: Int = 0

    override suspend fun roundTrip(
        context: ExecutionContext,
        request: HttpRequest,
    ): HttpCall = error("not used")

    override fun shutdown() {
        shutdownCount++
    }
}
