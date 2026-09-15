/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.smithy.kotlin.runtime.http.config.HttpEngineConfig
import aws.smithy.kotlin.runtime.http.engine.DefaultHttpEngine
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal actual val checkpointRestorePlatform: CheckpointRestorePlatform = NoOpCheckpointRestorePlatform()

internal actual fun createCheckpointRestoreLock(): CheckpointRestoreLock = NativeCheckpointRestoreLock()

@OptIn(ExperimentalAtomicApi::class)
private class NativeCheckpointRestoreLock : CheckpointRestoreLock {
    private val state = AtomicInt(0)

    override fun lock() {
        var acquired: Boolean
        do {
            acquired = state.compareAndSet(0, 1)
        } while (!acquired)
    }

    override fun unlock() {
        check(state.compareAndSet(1, 0)) { "Checkpoint/restore lock is not held" }
    }
}

private class NoOpCheckpointRestorePlatform : CheckpointRestorePlatform() {
    override val isActive: Boolean = false

    override fun register(lifecycle: CheckpointRestoreLifecycle): Any? = null

    override fun configureHttpEngine(builder: HttpEngineConfig.Builder) { }

    override fun createDefaultHttpEngine(): HttpClientEngine = DefaultHttpEngine()
}
