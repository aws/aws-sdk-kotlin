/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.model

/**
 * TODO
 */
public sealed interface MultiPartDownloadType

/**
 * TODO
 */
public object Range : MultiPartDownloadType

/**
 * TODO
 */
public object Part : MultiPartDownloadType
