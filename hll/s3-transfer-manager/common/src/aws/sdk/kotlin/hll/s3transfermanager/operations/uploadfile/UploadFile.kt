/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.uploadfile

import aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager
import aws.sdk.kotlin.hll.s3transfermanager.TransferContext
import aws.sdk.kotlin.hll.s3transfermanager.TransferInterceptor
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileResponse
import aws.sdk.kotlin.hll.s3transfermanager.model.utils.toUploadFileResponse
import aws.sdk.kotlin.hll.s3transfermanager.operations.uploadfile.hooks.completeTransfer
import aws.sdk.kotlin.hll.s3transfermanager.operations.uploadfile.hooks.initiateTransfer
import aws.sdk.kotlin.hll.s3transfermanager.operations.uploadfile.hooks.transferBytes
import aws.sdk.kotlin.hll.s3transfermanager.utils.S3TransferManagerException
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CompleteMultipartUploadResponse
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import aws.smithy.kotlin.runtime.telemetry.TelemetryProviderContext
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext

internal suspend fun uploadFileImplementation(
    uploadFileRequest: UploadFileRequest,
    client: S3Client,
    multipartUploadThresholdBytes: Long,
    partSizeBytes: Long,
    interceptors: List<TransferInterceptor>,
    maxInMemoryParts: Int,
    maxConcurrentPartUploads: Int,
): UploadFileResponse = withContext(currentCoroutineContext() + TelemetryProviderContext(client.config.telemetryProvider)) {
    val contentLength = uploadFileRequest.body?.contentLength ?: throw S3TransferManagerException("Body content length must be known")
    val multiPartUpload = contentLength > multipartUploadThresholdBytes
    val logger = coroutineContext.logger<S3TransferManager>()
    val transferContext = TransferContext()

    val mpuUploadId = initiateTransfer(
        multiPartUpload,
        transferContext,
        contentLength,
        uploadFileRequest,
        interceptors,
        client,
    )

    val uploadedParts = transferBytes(
        multiPartUpload,
        contentLength,
        partSizeBytes,
        logger,
        uploadFileRequest,
        transferContext,
        mpuUploadId,
        interceptors,
        client,
        maxInMemoryParts,
        maxConcurrentPartUploads,
    )

    completeTransfer(
        multiPartUpload,
        transferContext,
        uploadFileRequest,
        mpuUploadId,
        uploadedParts,
        interceptors,
        client,
    )

    return@withContext when (transferContext.response) {
        is PutObjectResponse -> (transferContext.response as PutObjectResponse).toUploadFileResponse()
        is CompleteMultipartUploadResponse -> (transferContext.response as CompleteMultipartUploadResponse).toUploadFileResponse()
        else -> throw S3TransferManagerException("Unexpected response type: ${transferContext.response}")
    }
}
