/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.uploadobject

import aws.sdk.kotlin.hll.s3transfermanager.utils.S3TransferManagerException
import aws.sdk.kotlin.hll.s3transfermanager.utils.ceilDiv
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.io.SdkBuffer
import aws.smithy.kotlin.runtime.io.SdkByteReadChannel
import aws.smithy.kotlin.runtime.io.SdkSource
import aws.smithy.kotlin.runtime.io.readFully
import aws.smithy.kotlin.runtime.io.readRemaining
import aws.smithy.kotlin.runtime.telemetry.logging.Logger

// S3 imposed limit for parts in a multipart upload
private const val MAX_NUMBER_PARTS = 10_000L

/**
 * Determines the actual part size to use for a multipart S3 upload.
 *
 * This function calculates the part size based on the total size
 * of the object and the requested part size. If the requested part size is
 * too small to allow the upload to fit within S3's 10,000-part limit, the
 * part size will be automatically increased so that exactly 10,000 parts
 * are uploaded.
 */
internal fun resolvePartSize(contentLength: Long, targetPartSize: Long, objectName: String?, logger: Logger): Long {
    val targetNumberOfParts = ceilDiv(contentLength, targetPartSize)
    return if (targetNumberOfParts > MAX_NUMBER_PARTS) {
        ceilDiv(contentLength, MAX_NUMBER_PARTS).also {
            logger.info {
                buildString {
                    append("The target part size of $targetPartSize bytes is too small to upload $objectName in $MAX_NUMBER_PARTS parts ")
                    append("(the maximum allowed by S3). ")
                    append("The object will be uploaded in parts of $it bytes instead.")
                }
            }
        }
    } else {
        targetPartSize
    }
}

/**
 * Determines what part source an S3 body will have:
 *  [ByteStream.Buffer]
 *  [ByteStream.ChannelStream]
 *  [ByteStream.SourceStream]
 */
internal fun resolveSource(body: ByteStream): Any = when (body) {
    is ByteStream.Buffer -> body.bytes()
    is ByteStream.ChannelStream -> body.readFrom()
    is ByteStream.SourceStream -> body.readFrom()
    else ->
        throw S3TransferManagerException(
            "Unhandled body type: ${body::class.simpleName }",
        )
}

/**
 * Retrieves the bytes for the next part of a multipart upload from the given part source into a [SdkBuffer]
 */
internal suspend fun nextPartBytes(
    partSource: Any,
    partSize: Long,
    lastPart: Boolean,
    readBytes: Long,
    readableBytes: Long,
): SdkBuffer {
    val buffer = SdkBuffer()

    when (partSource) {
        is ByteArray -> {
            // Long to Int is safe here because the ByteArray max size is Int.MAX_VALUE, its size is managed as an Int.
            val readBytes = readBytes.toInt()
            val readableBytes = readableBytes.toInt()
            val partSize = partSize.toInt()

            if (lastPart) {
                buffer.write(
                    partSource.sliceArray(readBytes..<readableBytes),
                )
            } else {
                buffer.write(
                    partSource.sliceArray(
                        readBytes..<readBytes + partSize,
                    ),
                )
            }
        }
        is SdkByteReadChannel -> {
            if (lastPart) {
                partSource.readRemaining(buffer)
            } else {
                partSource.readFully(buffer, partSize)
            }
        }
        is SdkSource -> {
            if (lastPart) {
                partSource.readRemaining(buffer)
            } else {
                partSource.readFully(buffer, partSize)
            }
        }
    }

    return buffer
}
