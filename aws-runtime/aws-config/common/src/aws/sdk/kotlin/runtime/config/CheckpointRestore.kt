/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.smithy.kotlin.runtime.http.config.HttpEngineConfig
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine

internal abstract class CheckpointRestorePlatform {
    internal abstract val isActive: Boolean

    internal abstract fun register(lifecycle: CheckpointRestoreLifecycle): Any?

    internal abstract fun configureHttpEngine(builder: HttpEngineConfig.Builder)

    internal abstract fun createDefaultHttpEngine(): HttpClientEngine
}

internal expect val checkpointRestorePlatform: CheckpointRestorePlatform

internal interface CheckpointRestoreLock {
    fun lock()

    fun unlock()
}

internal expect fun createCheckpointRestoreLock(): CheckpointRestoreLock

internal inline fun <T> CheckpointRestoreLock.withLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}

internal class CheckpointRestoreLifecycle(
    private val beforeCheckpointHook: () -> Unit,
    private val afterRestoreHook: () -> Unit,
) {
    @Suppress("unused")
    private var registration: Any? = null
    private var registered = false

    internal fun register(): CheckpointRestoreLifecycle = apply {
        check(!registered) { "Checkpoint/restore lifecycle is already registered" }
        registered = true
        registration = checkpointRestorePlatform.register(this)
    }

    internal fun beforeCheckpoint(): Unit = beforeCheckpointHook()

    internal fun afterRestore(): Unit = afterRestoreHook()
}

internal fun isCheckpointRestoreEnvironment(): Boolean = checkpointRestorePlatform.isActive

internal fun HttpEngineConfig.Builder.configureCheckpointRestore() {
    checkpointRestorePlatform.configureHttpEngine(this)
}

internal fun createCheckpointRestoreAwareDefaultHttpEngine(): HttpClientEngine = checkpointRestorePlatform.createDefaultHttpEngine()
