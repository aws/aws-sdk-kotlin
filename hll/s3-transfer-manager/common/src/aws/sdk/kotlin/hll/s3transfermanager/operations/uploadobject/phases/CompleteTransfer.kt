/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.uploadobject.phases

import aws.sdk.kotlin.hll.s3transfermanager.interceptors.MutableTransferContext
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferInterceptor
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferPhase
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.executePhase
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadObjectRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.utils.toCompleteMultipartUploadRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.utils.toUploadObjectResponse
import aws.sdk.kotlin.hll.s3transfermanager.utils.S3TransferManagerException
import aws.sdk.kotlin.hll.s3transfermanager.utils.withTmBusinessMetric
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CompleteMultipartUploadRequest
import aws.sdk.kotlin.services.s3.model.CompleteMultipartUploadResponse
import aws.sdk.kotlin.services.s3.model.CompletedPart
import aws.sdk.kotlin.services.s3.model.PutObjectResponse

internal suspend fun completeTransfer(
    multipartUpload: Boolean,
    context: MutableTransferContext,
    uploadObjectRequest: UploadObjectRequest,
    mpuUploadId: String?,
    uploadedParts: List<CompletedPart>,
    interceptors: List<TransferInterceptor>,
    client: S3Client,
) {
    if (multipartUpload) {
        context.s3Request =
            uploadObjectRequest.toCompleteMultipartUploadRequest(
                mpuUploadId!!,
                uploadedParts,
            )
    }

    executePhase(
        TransferPhase.TransferCompleted,
        context,
        interceptors,
    ) {
        if (multipartUpload) {
            try {
                context.s3Response = client.withTmBusinessMetric {
                    it.completeMultipartUpload(context.s3Request as CompleteMultipartUploadRequest)
                }
            } catch (e: Exception) {
                throw S3TransferManagerException("Unable to complete multipart upload with ID: $mpuUploadId", e)
            }
        }

        when (context.s3Response) {
            is PutObjectResponse -> (context.s3Response as PutObjectResponse).toUploadObjectResponse().also {
                context.tmResponse = it
            }
            is CompleteMultipartUploadResponse -> (context.s3Response as CompleteMultipartUploadResponse).toUploadObjectResponse().also {
                context.tmResponse = it
            }
            else -> throw S3TransferManagerException("Unexpected response type: ${context.s3Response}")
        }
    }
}
