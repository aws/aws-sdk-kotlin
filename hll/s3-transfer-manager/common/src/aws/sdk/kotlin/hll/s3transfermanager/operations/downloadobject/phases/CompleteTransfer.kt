/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject.phases

import aws.sdk.kotlin.hll.s3transfermanager.interceptors.MutableTransferContext
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferInterceptor
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferPhase
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.executePhase
import aws.sdk.kotlin.hll.s3transfermanager.model.utils.toDownloadObjectResponse
import aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject.DownloadBytesResult
import aws.sdk.kotlin.services.s3.model.GetObjectResponse

internal suspend fun completeTransfer(
    context: MutableTransferContext,
    interceptors: List<TransferInterceptor>,
) {
    // No-op for "complete transfer" phase
    executePhase(
        TransferPhase.TransferInitiated,
        context,
        interceptors,
    ) {}
}
