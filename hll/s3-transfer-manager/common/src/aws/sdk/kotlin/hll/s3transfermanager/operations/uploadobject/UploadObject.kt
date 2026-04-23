/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.uploadobject

import aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.MutableTransferContext
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferInterceptor
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadObjectRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadObjectResponse
import aws.sdk.kotlin.hll.s3transfermanager.operations.uploadobject.phases.completeTransfer
import aws.sdk.kotlin.hll.s3transfermanager.operations.uploadobject.phases.initiateTransfer
import aws.sdk.kotlin.hll.s3transfermanager.operations.uploadobject.phases.transferBytes
import aws.sdk.kotlin.hll.s3transfermanager.utils.S3TransferManagerException
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.telemetry.TelemetryProviderContext
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext

// TODO: Create abstraction class to use on each S3 TM operation. It should cut down on parameters between functions.
internal suspend fun uploadObjectImplementation(
    uploadObjectRequest: UploadObjectRequest,
    client: S3Client,
    multipartUploadThresholdBytes: Long,
    partSizeBytes: Long,
    interceptors: List<TransferInterceptor>,
    maxInMemoryParts: Int,
    maxConcurrentPartUploads: Int,
    bufferSemaphore: Semaphore,
): UploadObjectResponse = withContext(currentCoroutineContext() + TelemetryProviderContext(client.config.telemetryProvider)) {
    val contentLength = uploadObjectRequest.contentLength ?: uploadObjectRequest.body?.contentLength ?: throw S3TransferManagerException("Content length must be known. Please set it in the request parameters.")
    val multipartUpload = contentLength >= multipartUploadThresholdBytes
    val logger = coroutineContext.logger<S3TransferManager>()
    val transferContext = MutableTransferContext(
        tmRequest = uploadObjectRequest,
    )

    val mpuUploadId = initiateTransfer(
        multipartUpload,
        transferContext,
        contentLength,
        uploadObjectRequest,
        interceptors,
        client,
    )

    val uploadedParts = transferBytes(
        multipartUpload,
        contentLength,
        partSizeBytes,
        logger,
        uploadObjectRequest,
        transferContext,
        mpuUploadId,
        interceptors,
        client,
        maxInMemoryParts,
        maxConcurrentPartUploads,
        bufferSemaphore,
    )

    completeTransfer(
        multipartUpload,
        transferContext,
        uploadObjectRequest,
        mpuUploadId,
        uploadedParts,
        interceptors,
        client,
    )

    return@withContext transferContext.tmResponse as UploadObjectResponse
}
