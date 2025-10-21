/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.model

// TODO: KDocs

public sealed interface MultipartDownloadType
public object Range : MultipartDownloadType
public object Part : MultipartDownloadType
