/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager

// TODO: Add PUBLIC documentation to each thing in the context
/**
 * TODO
 */
public data class TransferContext(
    // Req/Resp
    var request: Any? = null,
    var response: Any? = null,

    // Byte transfers
    var transferableBytes: Long? = null,
    var currentBytes: ByteArray? = null,
    var transferredBytes: Long? = null,

    // File transfers
    var transferableFiles: Long? = null,
    var currentFile: String? = null,
    var transferredFiles: Long? = null,
)

// TODO: Add PUBLIC documentation to each hook
/**
 * TODO
 */
public interface TransferInterceptor {
    // Transfer initialization hooks
    public fun readBeforeTransferInitiated(context: TransferContext) {}
    public fun modifyBeforeTransferInitiated(context: TransferContext): TransferContext = context
    public fun readAfterTransferInitiated(context: TransferContext) {}
    public fun modifyAfterTransferInitiated(context: TransferContext): TransferContext = context

    // Byte transferring hooks
    public fun readBeforeBytesTransferred(context: TransferContext) {}
    public fun modifyBeforeBytesTransferred(context: TransferContext): TransferContext = context
    public fun readAfterBytesTransferred(context: TransferContext) {}
    public fun modifyAfterBytesTransferred(context: TransferContext): TransferContext = context

    // File transfer hooks
    public fun readBeforeFileTransferred(context: TransferContext) {}
    public fun modifyBeforeFileTransferred(context: TransferContext): TransferContext = context
    public fun readAfterFileTransferred(context: TransferContext) {}
    public fun modifyAfterFileTransferred(context: TransferContext): TransferContext = context

    // Transfer completion hooks
    public fun readBeforeTransferCompleted(context: TransferContext) {}
    public fun modifyBeforeTransferCompleted(context: TransferContext): TransferContext = context
    public fun readAfterTransferCompleted(context: TransferContext) {}
    public fun modifyAfterTransferCompleted(context: TransferContext): TransferContext = context
}

/**
 * TODO
 */
internal interface TransferHook
internal object TransferInitiated : TransferHook
internal object BytesTransferred : TransferHook
internal object FileTransferred : TransferHook
internal object TransferCompleted : TransferHook
