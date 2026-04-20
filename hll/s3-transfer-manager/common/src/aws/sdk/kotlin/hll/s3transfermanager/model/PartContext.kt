/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.model

/**
 * Represents a part of a multipart download.
 *
 * @param partNumber The part number.
 * @param bytes The bytes of the part.
 * @param offset The byte offset of this part within the overall object.
 */
public data class PartContext(
    val partNumber: Int,
    val bytes: ByteArray,
    val offset: Long,
)
