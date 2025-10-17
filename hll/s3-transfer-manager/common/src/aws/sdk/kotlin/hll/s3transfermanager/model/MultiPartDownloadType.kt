/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.model

// TODO: KDocs

public sealed interface MultiPartDownloadType
public object Range : MultiPartDownloadType
public object Part : MultiPartDownloadType
