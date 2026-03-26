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
internal fun writeObject(
    path: String,
    response: GetObjectResponse,
    logger: Logger,
) {
    val system = PlatformProvider.System

    val name = path.substringAfterLast("/")
    val dir = path.substringBeforeLast("/")

    val temp = "$dir$name.s3tmp.${Random.nextInt(0, 10_000_000)}" // e.g. Users/bob/downloads/object.s3tmp.314

    try {
        runBlocking {
            system.writeFile(temp, response.body?.toByteArray() ?: byteArrayOf())
            // TODO: Append file implementation

            if (system.fileExists(path)) {
                logger.warn { "Overwriting file: $path" }
            }

            // TODO: Rename file implementation
        }
    } finally {
        if (system.fileExists(temp)) {
            // TODO: Delete file implementation
        }
    }
}
