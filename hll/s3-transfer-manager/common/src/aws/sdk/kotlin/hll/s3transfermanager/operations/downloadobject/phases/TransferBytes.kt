/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject.phases

import aws.sdk.kotlin.hll.s3transfermanager.interceptors.MutableTransferContext
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferInterceptor
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.TransferPhase
import aws.sdk.kotlin.hll.s3transfermanager.interceptors.executePhase
import aws.sdk.kotlin.hll.s3transfermanager.model.MultipartDownloadType
import aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject.writeObject
import aws.sdk.kotlin.hll.s3transfermanager.utils.S3TransferManagerException
import aws.sdk.kotlin.hll.s3transfermanager.utils.withTmBusinessMetric
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectResponse
import aws.smithy.kotlin.runtime.telemetry.logging.Logger

internal suspend fun <T> transferBytes(
    multipartDownloadType: MultipartDownloadType,
    context: MutableTransferContext,
    s3Client: S3Client,
    downloadPath: String?,
    interceptors: List<TransferInterceptor>,
    objectHandler: (suspend (GetObjectResponse) -> T)?,
    logger: Logger,
) {
    var done = false

    executePhase(
        TransferPhase.BytesTransferred,
        context,
        interceptors,
    ) {
        s3Client.withTmBusinessMetric {
            it.getObject(context.s3Request as GetObjectRequest) { getObjectResponse ->
                context.s3Response = getObjectResponse
                context.transferredBytes = getObjectResponse.contentLength
                context.transferableBytes = getObjectResponse
                    .contentRange
                    ?.split("/") // e.g. "ContentRange=bytes 0-1/5" where 5 is the content length
                    ?.last()
                    ?.toLong()
                    ?: throw S3TransferManagerException("Content range not found in GetObjectResponse")

                if (getObjectResponse.partsCount == 1 || getObjectResponse.contentLength == context.transferableBytes) {
                    done = true

                    // TODO: Call these multiple times for each part (for file, add logic to write in parts)
                    objectHandler?.invoke(getObjectResponse)
                    downloadPath?.let {
                        writeObject(downloadPath, getObjectResponse, logger)
                    }
                }
            }
        }
    }

    if (done) {
        return
        // TODO: Build S3 TM response in complete transfer phase
    }

    when (multipartDownloadType) {
        MultipartDownloadType.Part -> {
        }
        MultipartDownloadType.Range -> {
        }
    }
}
