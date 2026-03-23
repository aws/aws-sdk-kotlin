/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject.phases

import aws.sdk.kotlin.hll.s3transfermanager.interceptors.MutableTransferContext
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferInterceptor
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferPhase
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.executePhase
import aws.sdk.kotlin.hll.s3transfermanager.model.DownloadObjectRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.MultipartDownloadType
import aws.sdk.kotlin.hll.s3transfermanager.model.utils.toGetObjectRequest

internal suspend fun initiateTransfer(
    multipartDownloadType: MultipartDownloadType,
    context: MutableTransferContext,
    downloadObjectRequest: DownloadObjectRequest,
    targetPartSizeBytes: Long,
    interceptors: List<TransferInterceptor>,
) {
    context.transferredBytes = 0L
    context.s3Request = when (multipartDownloadType) {
        MultipartDownloadType.Part -> downloadObjectRequest.toGetObjectRequest(partNumber = 1)
        MultipartDownloadType.Range -> downloadObjectRequest.toGetObjectRequest(range = "bytes=0-${targetPartSizeBytes - 1}")
    }

    // No-op for initialization phase
    executePhase(
        TransferPhase.TransferInitiated,
        context,
        interceptors,
    ) {}
}
