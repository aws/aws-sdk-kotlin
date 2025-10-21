/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.utils

import aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager
import aws.sdk.kotlin.hll.s3transfermanager.model.UploadFileRequest
import aws.sdk.kotlin.services.s3.model.CompleteMultipartUploadRequest
import aws.sdk.kotlin.services.s3.model.CompletedPart
import aws.sdk.kotlin.services.s3.model.UploadPartRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.fromInputStream
import aws.smithy.kotlin.runtime.io.SdkBuffer
import aws.smithy.kotlin.runtime.io.SdkByteReadChannel
import aws.smithy.kotlin.runtime.io.SdkSource
import aws.smithy.kotlin.runtime.telemetry.logging.Logger

// S3 imposed limit for parts in a multipart upload
private const val MAX_NUMBER_PARTS = 10_000L

/**
 * Determines the actual part size to use for a multipart S3 upload.
 *
 * This function calculates the part size based on the total size
 * of the file and the requested part size. If the requested part size is
 * too small to allow the upload to fit within S3's 10,000-part limit, the
 * part size will be automatically increased so that exactly 10,000 parts
 * are uploaded.
 */
internal fun resolvePartSize(contentLength: Long, tm: S3TransferManager, logger: Logger): Long {
    val targetNumberOfParts = contentLength / tm.partSizeBytes
    return if (targetNumberOfParts > MAX_NUMBER_PARTS) {
        ceilDiv(contentLength, MAX_NUMBER_PARTS).also {
            logger.debug { "Target part size is too small to meet the 10,000 S3 part limit. Increasing part size to $it" }
        }
    } else {
        tm.partSizeBytes
    }
}

/**
 * Retrieves the next part of a multipart upload from the given part source.
 */
internal suspend fun SdkBuffer.getNextPart(partSource: Any, partSize: Long, tm: S3TransferManager) {
    when (partSource) {
        is ByteArray -> {
            this.write(
                partSource.sliceArray(
                    tm.context.transferredBytes!!.toInt()..<tm.context.transferredBytes!!.toInt() + partSize.toInt(),
                ),
            )
        }
        is SdkByteReadChannel -> {
            var readBytes = 0L
            while (readBytes < partSize) {
                readBytes += partSource.read(this, partSize - readBytes)
            }
        }
        is SdkSource -> {
            var readBytes = 0L
            while (readBytes < partSize) {
                readBytes += partSource.read(this, partSize - readBytes)
            }
        }
    }
}

/**
 * Builds a low-level S3 upload part request from a high-level upload file request
 * and data from the S3 Transfer Manager.
 */
internal fun buildUploadPartRequest(
    uploadFileRequest: UploadFileRequest,
    currentPart: SdkBuffer,
    currentPartNumber: Long,
    mpuUploadId: String,
): UploadPartRequest =
    UploadPartRequest {
        // From high-level request
        bucket = uploadFileRequest.bucket
        checksumAlgorithm = uploadFileRequest.checksumAlgorithm
        expectedBucketOwner = uploadFileRequest.expectedBucketOwner
        key = uploadFileRequest.key
        requestPayer = uploadFileRequest.requestPayer
        sseCustomerAlgorithm = uploadFileRequest.sseCustomerAlgorithm
        sseCustomerKey = uploadFileRequest.sseCustomerKey
        sseCustomerKeyMd5 = uploadFileRequest.sseCustomerKeyMd5

        // From transfer manager
        uploadId = mpuUploadId
        body = ByteStream.fromInputStream(currentPart.inputStream(), currentPart.size)
        partNumber = currentPartNumber.toInt()
    }

/**
 * Builds a low-level S3 complete multipart upload request from a high-level upload file request
 * and data from the S3 Transfer Manager.
 */
internal fun buildCompleteMultipartUploadRequest(
    uploadFileRequest: UploadFileRequest,
    mpuUploadId: String,
    uploadedParts: List<CompletedPart>,
): CompleteMultipartUploadRequest =
    CompleteMultipartUploadRequest {
        // From high-level request
        bucket = uploadFileRequest.bucket
        checksumCrc32 = uploadFileRequest.checksumCrc32
        checksumCrc32C = uploadFileRequest.checksumCrc32C
        checksumCrc64Nvme = uploadFileRequest.checksumCrc64Nvme
        checksumSha1 = uploadFileRequest.checksumSha1
        checksumSha256 = uploadFileRequest.checksumSha256
        expectedBucketOwner = uploadFileRequest.expectedBucketOwner
        ifMatch = uploadFileRequest.ifMatch
        ifNoneMatch = uploadFileRequest.ifNoneMatch
        key = uploadFileRequest.key
        requestPayer = uploadFileRequest.requestPayer
        sseCustomerAlgorithm = uploadFileRequest.sseCustomerAlgorithm
        sseCustomerKey = uploadFileRequest.sseCustomerKey
        sseCustomerKeyMd5 = uploadFileRequest.sseCustomerKeyMd5

        // From transfer manager
        uploadId = mpuUploadId
        multipartUpload {
            parts = uploadedParts
        }
    }

/**
 * Returns the ceiling of the division
 *
 * This means the result is rounded up to the nearest integer if the dividend is not
 * evenly divisible by the divisor
 */
internal fun ceilDiv(dividend: Long, divisor: Long): Long {
    val div = dividend / divisor
    val remainder = dividend % divisor
    return if (remainder != 0L) div + 1 else div
}
