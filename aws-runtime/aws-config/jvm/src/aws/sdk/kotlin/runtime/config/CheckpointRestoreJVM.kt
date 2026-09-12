/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.smithy.kotlin.runtime.http.HttpCall
import aws.smithy.kotlin.runtime.http.config.HttpEngineConfig
import aws.smithy.kotlin.runtime.http.engine.CloseableHttpClientEngine
import aws.smithy.kotlin.runtime.http.engine.DefaultHttpEngine
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineConfig
import aws.smithy.kotlin.runtime.http.engine.okhttp.OkHttpEngine
import aws.smithy.kotlin.runtime.http.engine.okhttp.OkHttpEngineConfig
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.io.SdkManaged
import aws.smithy.kotlin.runtime.io.SdkManagedBase
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.crac.Context
import org.crac.Core
import org.crac.Resource
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.CoroutineContext

private const val LAMBDA_INITIALIZATION_TYPE = "AWS_LAMBDA_INITIALIZATION_TYPE"
private const val SNAP_START = "snap-start"
private const val OKHTTP_ENGINE_NAME = "http-client-engine-OkHttp-context"
private const val OKHTTP4_ENGINE_NAME = "http-client-engine-OkHttp4-context"
private const val OKHTTP4_ENGINE_CLASS = "aws.smithy.kotlin.runtime.http.engine.okhttp4.OkHttp4Engine"
private const val CHECKPOINT_QUIESCENCE_TIMEOUT_MILLIS = 10_000L

internal actual val checkpointRestorePlatform: CheckpointRestorePlatform = LambdaSnapStartCracPlatform()

internal actual fun createCheckpointRestoreLock(): CheckpointRestoreLock = JvmCheckpointRestoreLock()

private class JvmCheckpointRestoreLock : CheckpointRestoreLock {
    private val delegate = ReentrantLock()

    override fun lock(): Unit = delegate.lock()

    override fun unlock(): Unit = delegate.unlock()
}

private class LambdaSnapStartCracPlatform : CheckpointRestorePlatform() {
    private val enabled = System.getenv(LAMBDA_INITIALIZATION_TYPE) == SNAP_START
    private val registrationLock = createCheckpointRestoreLock()
    private var restored = false

    override val isActive: Boolean
        get() = registrationLock.withLock { enabled && !restored }

    override fun register(lifecycle: CheckpointRestoreLifecycle): Any? {
        val resource = registrationLock.withLock {
            if (!enabled || restored) {
                null
            } else {
                CracResource(WeakReference(lifecycle), ::markRestored)
            }
        } ?: return null
        Core.getGlobalContext().register(resource)
        return resource
    }

    override fun configureHttpEngine(builder: HttpEngineConfig.Builder) {
        builder.configureCheckpointRestore(isActive)
    }

    override fun createDefaultHttpEngine(): HttpClientEngine = createCheckpointRestoreAwareDefaultHttpEngine(isActive)

    private fun markRestored() {
        registrationLock.withLock {
            restored = true
        }
    }
}

private class CracResource(
    private val lifecycle: WeakReference<CheckpointRestoreLifecycle>,
    private val markRestored: () -> Unit,
) : Resource {
    override fun beforeCheckpoint(context: Context<out Resource>) {
        lifecycle.get()?.beforeCheckpoint()
    }

    override fun afterRestore(context: Context<out Resource>) {
        try {
            lifecycle.get()?.afterRestore()
        } finally {
            markRestored()
        }
    }
}

internal fun HttpEngineConfig.Builder.configureCheckpointRestore(enabled: Boolean) {
    if (!enabled) {
        return
    }

    val engine = buildHttpEngineConfig().httpClient
    if (engine is SdkManagedCheckpointRestoreHttpClientEngine) {
        return
    }

    val replacement = if (engine is SdkManaged && engine is CloseableHttpClientEngine) {
        engine.replacementFactory()?.let {
            SdkManagedCheckpointRestoreHttpClientEngine(CheckpointRestoreHttpClientEngine(engine, replacementFactory = it))
        }
    } else {
        null
    }

    httpClient = replacement ?: engine
}

internal fun createCheckpointRestoreAwareDefaultHttpEngine(enabled: Boolean): HttpClientEngine {
    val engine = DefaultHttpEngine()
    if (!enabled) {
        return engine
    }

    val replacementFactory = engine.replacementFactory() ?: return engine
    return CheckpointRestoreHttpClientEngine(engine, replacementFactory = replacementFactory)
}

private fun HttpClientEngine.replacementFactory(): (() -> CloseableHttpClientEngine)? {
    val contextClassLoader = Thread.currentThread().contextClassLoader
    return when (val engineConfig = config) {
        is OkHttpEngineConfig -> when (coroutineContext[CoroutineName]?.name) {
            OKHTTP_ENGINE_NAME -> ({ OkHttpEngine(engineConfig) })
            OKHTTP4_ENGINE_NAME -> ({ createOkHttp4Engine(engineConfig, contextClassLoader) })
            else -> null
        }
        else -> null
    }
}

private fun createOkHttp4Engine(
    config: OkHttpEngineConfig,
    classLoader: ClassLoader?,
): CloseableHttpClientEngine {
    val effectiveClassLoader = classLoader ?: CheckpointRestoreHttpClientEngine::class.java.classLoader
    val engineClass = Class.forName(OKHTTP4_ENGINE_CLASS, true, effectiveClassLoader)
    val constructor = engineClass.getConstructor(OkHttpEngineConfig::class.java)
    return constructor.newInstance(config) as CloseableHttpClientEngine
}

internal class CheckpointRestoreHttpClientEngine(
    initialEngine: CloseableHttpClientEngine,
    private val checkpointQuiescenceTimeoutMillis: Long = CHECKPOINT_QUIESCENCE_TIMEOUT_MILLIS,
    private val replacementFactory: () -> CloseableHttpClientEngine,
) : CloseableHttpClientEngine {
    private val lock = createCheckpointRestoreLock()
    private var closed = false
    private var checkpointed = false
    private var currentEngine = initialEngine

    override val config: HttpClientEngineConfig = initialEngine.config

    @Suppress("unused")
    private val checkpointRestoreLifecycle = if (isCheckpointRestoreEnvironment()) {
        CheckpointRestoreLifecycle(::beforeCheckpoint, ::afterRestore).register()
    } else {
        null
    }

    override val coroutineContext: CoroutineContext
        get() = lock.withLock { currentEngine.coroutineContext }

    override suspend fun roundTrip(context: ExecutionContext, request: HttpRequest): HttpCall {
        val engine = lock.withLock {
            check(!checkpointed) { "HTTP engine is unavailable during a checkpoint" }
            currentEngine
        }
        return engine.roundTrip(context, request)
    }

    override fun close() {
        val engine = lock.withLock {
            if (closed) {
                return
            }
            closed = true
            currentEngine
        }
        engine.close()
    }

    internal fun beforeCheckpoint() {
        val engine = lock.withLock {
            if (closed || checkpointed) {
                return
            }
            checkpointed = true
            currentEngine
        }

        try {
            if (engine is SdkManaged) {
                engine.unshare()
            } else {
                engine.close()
            }
            engine.coroutineContext[Job]?.let { job ->
                runBlocking {
                    withTimeout(checkpointQuiescenceTimeoutMillis) {
                        job.join()
                    }
                }
            }
        } catch (ex: Exception) {
            recoverFromFailedCheckpoint()
            throw IllegalStateException("HTTP engine did not become idle before the checkpoint", ex)
        }
    }

    internal fun afterRestore() {
        lock.withLock {
            if (closed || !checkpointed) {
                return
            }
            currentEngine = replacementFactory()
            checkpointed = false
        }
    }

    private fun recoverFromFailedCheckpoint() {
        lock.withLock {
            if (closed || !checkpointed) {
                return
            }
            currentEngine = replacementFactory()
            checkpointed = false
        }
    }
}

internal class SdkManagedCheckpointRestoreHttpClientEngine(
    private val delegate: CheckpointRestoreHttpClientEngine,
) : SdkManagedBase(),
    CloseableHttpClientEngine by delegate {
    override fun unshare(): Boolean {
        val shouldClose = super.unshare()
        if (shouldClose) {
            close()
        }
        return shouldClose
    }

    internal fun beforeCheckpoint(): Unit = delegate.beforeCheckpoint()

    internal fun afterRestore(): Unit = delegate.afterRestore()
}
