/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.model

import aws.smithy.kotlin.runtime.io.SdkBuffer

/**
 * Represents a part in a multipart upload.
 *
 * @param number The part number.
 * @param bytes The bytes of the part.
 */
internal data class Part(
    val number: Int,
    val bytes: SdkBuffer,
)
