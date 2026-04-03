/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.operations.downloadobject

import aws.sdk.kotlin.services.s3.model.GetObjectResponse
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

// TODO: Run a download test in windows/mac/linux...that means we need common file functions
// TODO: Implement this the right way
internal fun writeObject(
    path: String,
    response: GetObjectResponse,
    logger: Logger,
) {
    // TODO: Don't create it if it doesn't exist?
    val tempFile = "$path.s3tmp.${Random.nextInt(0, 10_000_000)}" // e.g. Users/bob/downloads/object.s3tmp.314
    val system = PlatformProvider.System

    try {
        runBlocking {
            system.writeFile(tempFile, response.body?.toByteArray() ?: byteArrayOf()) // TODO: This needs to include offsets
            // TODO: Append file implementation

            if (system.fileExists(path)) {
                logger.warn { "Overwriting file: $path" }
            }

            // TODO: Rename file implementation
        }
    } finally {
        if (system.fileExists(tempFile)) {
            // TODO: Delete file implementation
        }
    }
}

// TODO: Share between operations (this is duplicated from upload object)
/**
 * Returns the ceiling of the division
 *
 * This means the result is rounded up to the nearest integer if the dividend is not
 * evenly divisible by the divisor
 */
internal fun ceilDiv(dividend: Long, divisor: Long): Long {
    val div = dividend / divisor
    val remainder = dividend % divisor
    return if (remainder != 0L) {
        div + 1
    } else {
        div
    }
}