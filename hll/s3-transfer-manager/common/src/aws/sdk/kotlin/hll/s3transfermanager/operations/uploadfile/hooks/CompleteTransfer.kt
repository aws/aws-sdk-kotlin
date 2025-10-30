/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.uploadfile.hooks

import aws.sdk.kotlin.hll.s3transfermanager.TransferCompleted
import aws.sdk.kotlin.hll.s3transfermanager.TransferContext
import aws.sdk.kotlin.hll.s3transfermanager.TransferInterceptor
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.utils.toCompleteMultipartUploadRequest
import aws.sdk.kotlin.hll.s3transfermanager.operationHook
import aws.sdk.kotlin.hll.s3transfermanager.utils.S3TransferManagerException
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CompleteMultipartUploadRequest
import aws.sdk.kotlin.services.s3.model.CompletedPart

internal suspend fun completeTransfer(
    multipartUpload: Boolean,
    context: TransferContext,
    uploadFileRequest: UploadFileRequest,
    mpuUploadId: String?,
    uploadedParts: List<CompletedPart>,
    interceptors: List<TransferInterceptor>,
    client: S3Client,
) {
    if (multipartUpload) {
        context.request =
            uploadFileRequest.toCompleteMultipartUploadRequest(
                mpuUploadId!!,
                uploadedParts,
            )
    }

    operationHook(
        TransferCompleted,
        context,
        interceptors,
    ) {
        if (multipartUpload) {
            try {
                context.response = client.completeMultipartUpload(context.request as CompleteMultipartUploadRequest)
            } catch (e: Exception) {
                throw S3TransferManagerException("Unable to complete multipart upload with ID: $mpuUploadId", e)
            }
        }
    }
}
