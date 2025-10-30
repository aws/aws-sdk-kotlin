/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.utils

/**
 * Exception thrown when an error occurs during S3 transfer operations.
 *
 * @param message Description of the error.
 * @param cause The underlying cause of the exception, if any.
 */
internal class S3TransferManagerException(message: String, cause: Throwable? = null) : Exception(message, cause)
