/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.uploadfile.hooks

import aws.sdk.kotlin.hll.s3transfermanager.TransferContext
import aws.sdk.kotlin.hll.s3transfermanager.TransferInitiated
import aws.sdk.kotlin.hll.s3transfermanager.TransferInterceptor
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.utils.toCreateMultipartUploadRequest
import aws.sdk.kotlin.hll.s3transfermanager.model.utils.toPutObjectRequest
import aws.sdk.kotlin.hll.s3transfermanager.operationHook
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CreateMultipartUploadRequest
import aws.sdk.kotlin.services.s3.model.CreateMultipartUploadResponse

internal suspend fun initiateTransfer(
    multipartUpload: Boolean,
    context: TransferContext,
    contentLength: Long,
    uploadFileRequest: UploadFileRequest,
    interceptors: List<TransferInterceptor>,
    client: S3Client,
): String? {
    context.transferredBytes = 0L
    context.transferableBytes = contentLength
    context.request = if (multipartUpload) {
        uploadFileRequest.toCreateMultipartUploadRequest()
    } else {
        uploadFileRequest.toPutObjectRequest()
    }

    var mpuUploadId: String? = null
    operationHook(
        TransferInitiated,
        context,
        interceptors,
    ) {
        if (multipartUpload) {
            context.response = client.createMultipartUpload(context.request as CreateMultipartUploadRequest)
            mpuUploadId = (context.response as CreateMultipartUploadResponse).uploadId!!
        }
    }
    return mpuUploadId
}
