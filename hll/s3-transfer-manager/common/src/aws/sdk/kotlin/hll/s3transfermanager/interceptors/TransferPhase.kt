/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.interceptors

/**
 * Describes a type of phase that is executed during an [aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager] transfer
 */
internal sealed interface TransferPhase {
    object TransferInitiated : TransferPhase
    object BytesTransferred : TransferPhase
    object ObjectTransferred : TransferPhase
    object TransferCompleted : TransferPhase
}

/**
 * Executes a sequence of hooks around a phase of an operation.
 *
 * The execution flow is as follows:
 * 1. Runs all hooks scheduled to execute **before** the phase.
 * 2. Executes the phase logic.
 * 3. Runs all hooks scheduled to execute **after** the phase.
 */
internal suspend fun executePhase(
    phase: TransferPhase,
    context: MutableTransferContext,
    interceptors: List<TransferInterceptor>,
    block: suspend () -> Unit,
) {
    when (phase) {
        is TransferPhase.TransferInitiated -> {
            var immutableContext = context.immutableCopy()
            interceptors.forEachCatching { readBeforeTransferInitiated(immutableContext) }
            interceptors.forEach { it.modifyBeforeTransferInitiated(context) }
            block.invoke()
            immutableContext = context.immutableCopy()
            interceptors.forEachCatching { readAfterTransferInitiated(immutableContext) }
            interceptors.forEach { it.modifyAfterTransferInitiated(context) }
        }
        is TransferPhase.BytesTransferred -> {
            var immutableContext = context.immutableCopy()
            interceptors.forEachCatching { readBeforeBytesTransferred(immutableContext) }
            interceptors.forEach { it.modifyBeforeBytesTransferred(context) }
            block.invoke()
            immutableContext = context.immutableCopy()
            interceptors.forEachCatching { readAfterBytesTransferred(immutableContext) }
            interceptors.forEach { it.modifyAfterBytesTransferred(context) }
        }
        is TransferPhase.ObjectTransferred -> {
            var immutableContext = context.immutableCopy()
            interceptors.forEachCatching { readBeforeObjectTransferred(immutableContext) }
            interceptors.forEach { it.modifyBeforeObjectTransferred(context) }
            block.invoke()
            immutableContext = context.immutableCopy()
            interceptors.forEachCatching { readAfterObjectTransferred(immutableContext) }
            interceptors.forEach { it.modifyAfterObjectTransferred(context) }
        }
        is TransferPhase.TransferCompleted -> {
            var immutableContext = context.immutableCopy()
            interceptors.forEachCatching { readBeforeTransferCompleted(immutableContext) }
            interceptors.forEach { it.modifyBeforeTransferCompleted(context) }
            block.invoke()
            immutableContext = context.immutableCopy()
            interceptors.forEachCatching { readAfterTransferCompleted(immutableContext) }
            interceptors.forEach { it.modifyAfterTransferCompleted(context) }
        }
    }
}

/**
 * Executes an action for each [TransferInterceptor].
 * Collects all exceptions, if any, and finally throws the first one with the others suppressed.
 */
private fun List<TransferInterceptor>.forEachCatching(
    action: TransferInterceptor.() -> Unit,
) {
    var exception: Exception? = null

    this.forEach {
        try {
            it.action()
        } catch (e: Exception) {
            if (exception == null) {
                exception = e
            } else {
                exception.addSuppressed(e)
            }
        }
    }

    exception?.let { throw it }
}
