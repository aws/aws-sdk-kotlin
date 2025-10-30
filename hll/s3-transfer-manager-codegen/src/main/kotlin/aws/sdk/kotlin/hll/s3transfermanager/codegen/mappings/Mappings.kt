/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings

import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.uploadfile.uploadFileConversions
import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.uploadfile.uploadFileIoMappings

internal val ioMappings = uploadFileIoMappings
internal val conversionMappings = uploadFileConversions
