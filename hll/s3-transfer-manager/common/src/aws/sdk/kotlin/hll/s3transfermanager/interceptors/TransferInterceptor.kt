/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.interceptors

/**
 * A transfer interceptor allows peeking into the progress
 * and context of an [aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager] transfer at a certain phase using hooks.
 * Also allows modifying a transfer in progress using [TransferInterceptorContext] parameters such as [TransferInterceptorContext.s3Response].
 *
 * Terminology:
 *  Phase - A specific execution point for a transfer.
 *  Hook - Methods that allows interceptors to read/modify a transfer before and after a phase.
 *  Transfer context - See: [TransferInterceptorContext]
 *  Transfer initiated - The point in time a transfer is initiated. For example, in multipart uploads this is when a [aws.sdk.kotlin.services.s3.model.CreateMultipartUploadRequest] is sent to S3.
 *  Bytes transferred - Any time bytes are transferred to S3 for either an upload or download
 *  Object transferred - Any time objects are transferred to S3 for either an upload or download
 *  Transfer completed - The point in time a transfer is completed. For example in multipart uploads this is when a [aws.sdk.kotlin.services.s3.model.CompleteMultipartUploadRequest] is sent to S3.
 */
public interface TransferInterceptor {
    public fun readBeforeTransferInitiated(context: TransferContext) {}
    public fun modifyBeforeTransferInitiated(context: MutableTransferContext) {}
    public fun readAfterTransferInitiated(context: TransferContext) {}
    public fun modifyAfterTransferInitiated(context: MutableTransferContext) {}

    public fun readBeforeBytesTransferred(context: TransferContext) {}
    public fun modifyBeforeBytesTransferred(context: MutableTransferContext) {}
    public fun readAfterBytesTransferred(context: TransferContext) {}
    public fun modifyAfterBytesTransferred(context: MutableTransferContext) {}

    public fun readBeforeObjectTransferred(context: TransferContext) {}
    public fun modifyBeforeObjectTransferred(context: MutableTransferContext) {}
    public fun readAfterObjectTransferred(context: TransferContext) {}
    public fun modifyAfterObjectTransferred(context: MutableTransferContext) {}

    public fun readBeforeTransferCompleted(context: TransferContext) {}
    public fun modifyBeforeTransferCompleted(context: MutableTransferContext) {}
    public fun readAfterTransferCompleted(context: TransferContext) {}
    public fun modifyAfterTransferCompleted(context: MutableTransferContext) {}
}
