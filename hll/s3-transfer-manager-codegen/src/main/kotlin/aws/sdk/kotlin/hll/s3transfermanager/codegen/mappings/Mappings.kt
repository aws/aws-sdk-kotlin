/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings

import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.downloadobject.downloadObjectConversions
import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.downloadobject.downloadObjectIoMappings
import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.uploadobject.uploadObjectConversions
import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.uploadobject.uploadObjectIoMappings

internal val ioMappings = uploadObjectIoMappings + downloadObjectIoMappings
internal val conversionMappings = uploadObjectConversions + downloadObjectConversions
