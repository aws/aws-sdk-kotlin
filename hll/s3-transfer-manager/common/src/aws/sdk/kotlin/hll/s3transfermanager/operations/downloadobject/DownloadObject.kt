/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject

import aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.MutableTransferContext
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferInterceptor
import aws.sdk.kotlin.hll.s3transfermanager.model.DownloadObjectRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.DownloadObjectResponse
import aws.sdk.kotlin.hll.s3transfermanager.model.MultipartDownloadType
import aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject.phases.completeTransfer
import aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject.phases.initiateTransfer
import aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject.phases.transferBytes
import aws.sdk.kotlin.hll.s3transfermanager.utils.S3TransferManagerException
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectResponse
import aws.smithy.kotlin.runtime.telemetry.TelemetryProviderContext
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext

// TODO: Must be cancellable (add tests). Why isn't there a cancel fun for uploads?
// TODO: Covert each operation to a class/interface?
internal suspend fun <T> downloadObjectImplementation(
    downloadObjectRequest: DownloadObjectRequest,
    objectHandler: (suspend (GetObjectResponse) -> T)?,
    s3Client: S3Client,
    multipartDownloadType: MultipartDownloadType,
    targetPartSizeBytes: Long,
    interceptors: List<TransferInterceptor>,
    downloadPath: String?,
    networkSemaphore: Semaphore,
    diskSemaphore: Semaphore,
    maxInMemoryParts: Int,
    bufferSemaphore: Semaphore,
): DownloadObjectResponse = withContext(currentCoroutineContext() + TelemetryProviderContext(s3Client.config.telemetryProvider)) {
    if (objectHandler == null && downloadPath == null) {
        throw S3TransferManagerException("Please specify what to do with the downloaded object by setting a download path or an object handler.")
    }

    val logger = coroutineContext.logger<S3TransferManager>()
    val transferContext = MutableTransferContext(
        tmRequest = downloadObjectRequest,
    )

    initiateTransfer(
        multipartDownloadType,
        transferContext,
        downloadObjectRequest,
        targetPartSizeBytes,
        interceptors,
    )

    transferBytes(
        multipartDownloadType,
        transferContext,
        s3Client,
        downloadPath,
        interceptors,
        objectHandler,
        logger,
        networkSemaphore,
        diskSemaphore,
        maxInMemoryParts,
        targetPartSizeBytes,
        bufferSemaphore,
    )

    completeTransfer()

    return@withContext transferContext.tmResponse as DownloadObjectResponse
}
