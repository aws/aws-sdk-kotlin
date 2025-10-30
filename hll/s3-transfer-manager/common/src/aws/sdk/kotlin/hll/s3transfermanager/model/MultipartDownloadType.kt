/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.model

/**
 * Defines the strategy used for multipart downloads in [aws.sdk.kotlin.hll.s3transfermanager.S3TransferManager].
 *
 * A multipart download can either be performed by specifying byte ranges or by requesting individual parts.
 */
public sealed interface MultipartDownloadType

/**
 * Download specific byte ranges from an object.
 */
public object Range : MultipartDownloadType

/**
 * Download individual parts of an object as defined by the multipart upload structure.
 */
public object Part : MultipartDownloadType
