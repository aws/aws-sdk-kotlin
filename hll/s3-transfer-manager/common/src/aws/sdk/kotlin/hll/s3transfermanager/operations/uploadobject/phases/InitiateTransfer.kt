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
import aws.sdk.kotlin.hll.s3transfermanager.model.utils.toCreateMultipartUploadRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.utils.toPutObjectRequest
import aws.sdk.kotlin.hll.s3transfermanager.utils.withTmBusinessMetric
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CreateMultipartUploadRequest
import aws.sdk.kotlin.services.s3.model.CreateMultipartUploadResponse

internal suspend fun initiateTransfer(
    multipartUpload: Boolean,
    context: MutableTransferContext,
    contentLength: Long,
    uploadObjectRequest: UploadObjectRequest,
    interceptors: List<TransferInterceptor>,
    client: S3Client,
): String? {
    context.transferredBytes = 0L
    context.transferableBytes = contentLength
    context.s3Request = if (multipartUpload) {
        uploadObjectRequest.toCreateMultipartUploadRequest()
    } else {
        uploadObjectRequest.toPutObjectRequest()
    }

    var mpuUploadId: String? = null
    executePhase(
        TransferPhase.TransferInitiated,
        context,
        interceptors,
    ) {
        if (multipartUpload) {
            context.s3Response = client.withTmBusinessMetric {
                it.createMultipartUpload(context.s3Request as CreateMultipartUploadRequest)
            }
            mpuUploadId = (context.s3Response as CreateMultipartUploadResponse).uploadId!!
        }
    }
    return mpuUploadId
}
