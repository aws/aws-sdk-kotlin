/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager

import aws.smithy.kotlin.runtime.content.ByteStream

/**
 * A transfer interceptor allows peeking into the progress
 * and context of an [S3TransferManager] transfer at a certain point of time using hooks.
 * Also allows modifying a transfer in progress using [TransferInterceptorContext] parameters such as [TransferInterceptorContext.response].
 *
 * Terminology:
 *  Hook - A specific execution point of an S3 transfer manager transfer. Exposed via methods in the [TransferInterceptor].
 *  Transfer context - See: [TransferInterceptorContext]
 *  Transfer initiated - The point in time a transfer is initiated. For example, in multipart uploads this is when a [aws.sdk.kotlin.services.s3.model.CreateMultipartUploadRequest] is sent to S3.
 *  Bytes transferred - Any time bytes are transferred to S3 for either an upload or download
 *  File transferred - Any time files are transferred to S3 for either an upload or download
 *  Transfer completed - The point in time a transfer is completed. For example in multipart uploads this is when a [aws.sdk.kotlin.services.s3.model.CompleteMultipartUploadRequest] is sent to S3.
 */
public interface TransferInterceptor {
    // Transfer initialization hooks
    public fun readBeforeTransferInitiated(context: TransferInterceptorContext) {}
    public fun modifyBeforeTransferInitiated(context: TransferInterceptorContext) {}
    public fun readAfterTransferInitiated(context: TransferInterceptorContext) {}
    public fun modifyAfterTransferInitiated(context: TransferInterceptorContext) {}

    // Byte transferring hooks
    public fun readBeforeBytesTransferred(context: TransferInterceptorContext) {}
    public fun modifyBeforeBytesTransferred(context: TransferInterceptorContext) {}
    public fun readAfterBytesTransferred(context: TransferInterceptorContext) {}
    public fun modifyAfterBytesTransferred(context: TransferInterceptorContext) {}

    // File transfer hooks
    public fun readBeforeFileTransferred(context: TransferInterceptorContext) {}
    public fun modifyBeforeFileTransferred(context: TransferInterceptorContext) {}
    public fun readAfterFileTransferred(context: TransferInterceptorContext) {}
    public fun modifyAfterFileTransferred(context: TransferInterceptorContext) {}

    // Transfer completion hooks
    public fun readBeforeTransferCompleted(context: TransferInterceptorContext) {}
    public fun modifyBeforeTransferCompleted(context: TransferInterceptorContext) {}
    public fun readAfterTransferCompleted(context: TransferInterceptorContext) {}
    public fun modifyAfterTransferCompleted(context: TransferInterceptorContext) {}
}

/**
 * Executes a sequence of operations around a hook.
 *
 * The execution flow is as follows:
 * 1. Runs all interceptors scheduled to execute **before** the hook.
 * 2. Executes the main hook logic.
 * 3. Runs all interceptors scheduled to execute **after** the hook.
 */
internal suspend fun operationHook(
    hook: TransferHook,
    context: TransferContext,
    interceptors: List<TransferInterceptor>,
    block: suspend () -> Unit,
) {
    when (hook) {
        is TransferInitiated -> {
            interceptors.forEachCatching { readBeforeTransferInitiated(context) }
            interceptors.forEachCatching { modifyBeforeTransferInitiated(context) }
            block.invoke()
            interceptors.forEachCatching { readAfterTransferInitiated(context) }
            interceptors.forEachCatching { modifyAfterTransferInitiated(context) }
        }
        is BytesTransferred -> {
            interceptors.forEachCatching { readBeforeBytesTransferred(context) }
            interceptors.forEachCatching { modifyBeforeBytesTransferred(context) }
            block.invoke()
            interceptors.forEachCatching { readAfterBytesTransferred(context) }
            interceptors.forEachCatching { modifyAfterBytesTransferred(context) }
        }
        is FileTransferred -> {
            interceptors.forEachCatching { readBeforeFileTransferred(context) }
            interceptors.forEachCatching { modifyBeforeFileTransferred(context) }
            block.invoke()
            interceptors.forEachCatching { readAfterFileTransferred(context) }
            interceptors.forEachCatching { modifyAfterFileTransferred(context) }
        }
        is TransferCompleted -> {
            interceptors.forEachCatching { readBeforeTransferCompleted(context) }
            interceptors.forEachCatching { modifyBeforeTransferCompleted(context) }
            block.invoke()
            interceptors.forEachCatching { readAfterTransferCompleted(context) }
            interceptors.forEachCatching { modifyAfterTransferCompleted(context) }
        }
        else -> {
            error("TransferHook not implemented: ${hook::class.simpleName}")
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

/**
 * Describes a type of hook that is used during an [S3TransferManager] transfer
 */
internal interface TransferHook
internal object TransferInitiated : TransferHook
internal object BytesTransferred : TransferHook
internal object FileTransferred : TransferHook
internal object TransferCompleted : TransferHook

/**
 * The context around an [S3TransferManager] transfer.
 * Used to track transfer progress or to modify in progress transfers, such as low level requests/responses from S3.
 */
public interface TransferInterceptorContext {
    // Req/Resp
    public var request: Any?
    public var response: Any?

    // Byte transfers
    public var transferableBytes: Long?
    public var currentBytes: ByteStream?
    public var transferredBytes: Long?

    // File transfers
    public var transferableFiles: Long?
    public var currentFile: String?
    public var transferredFiles: Long?
}

/**
 * Concrete implementation of [TransferInterceptorContext].
 * Used internally by the [S3TransferManager].
 */
internal data class TransferContext(
    // Req/Resp
    override var request: Any? = null,
    override var response: Any? = null,

    // Byte transfers
    override var transferableBytes: Long? = null,
    override var currentBytes: ByteStream? = null,
    override var transferredBytes: Long? = null,

    // File transfers
    override var transferableFiles: Long? = null,
    override var currentFile: String? = null,
    override var transferredFiles: Long? = null,
) : TransferInterceptorContext
