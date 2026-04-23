/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject

import aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.MutableTransferContext
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferInterceptor
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferPhase
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.executePhase
import aws.sdk.kotlin.hll.s3transfermanager.model.DownloadObjectRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.DownloadObjectResponse
import aws.sdk.kotlin.hll.s3transfermanager.model.MultipartDownloadType
import aws.sdk.kotlin.hll.s3transfermanager.model.PartContext
import aws.sdk.kotlin.hll.s3transfermanager.model.utils.toGetObjectRequest
import aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject.phases.TransferBytes
import aws.sdk.kotlin.hll.s3transfermanager.utils.S3TransferManagerException
import aws.sdk.kotlin.services.s3.S3Client
import kotlinx.coroutines.sync.Semaphore

internal suspend fun <T> downloadObjectImplementation(
    downloadObjectRequest: DownloadObjectRequest,
    objectHandler: (suspend (PartContext) -> T)?,
    s3Client: S3Client,
    multipartDownloadType: MultipartDownloadType,
    targetPartSizeBytes: Long,
    interceptors: List<TransferInterceptor>,
    downloadPath: String?,
    networkSemaphore: Semaphore,
    fileSystemSemaphore: Semaphore,
    maxInMemoryParts: Int,
    bufferSemaphore: Semaphore,
): DownloadObjectResponse {
    val transferContext = MutableTransferContext(
        tmRequest = downloadObjectRequest,
        transferredBytes = 0L,
        s3Request = when (multipartDownloadType) {
            MultipartDownloadType.Part -> downloadObjectRequest.toGetObjectRequest(partNumber = 1)
            MultipartDownloadType.Range -> downloadObjectRequest.toGetObjectRequest(range = "bytes=0-${targetPartSizeBytes - 1}")
        },
    )

    if (objectHandler == null && downloadPath == null) {
        throw S3TransferManagerException("Please specify what to do with the downloaded object by setting a download path or an object handler.")
    }

    // No-op for "initialization" phase
    executePhase(
        TransferPhase.TransferInitiated,
        transferContext,
        interceptors,
    ) {}

    TransferBytes(
        multipartDownloadType,
        transferContext,
        s3Client,
        downloadPath,
        interceptors,
        objectHandler,
        networkSemaphore,
        fileSystemSemaphore,
        maxInMemoryParts,
        targetPartSizeBytes,
        bufferSemaphore,
    ).transfer()

    // No-op for "complete transfer" phase
    executePhase(
        TransferPhase.TransferInitiated,
        transferContext,
        interceptors,
    ) {}

    return transferContext.tmResponse as DownloadObjectResponse
}
