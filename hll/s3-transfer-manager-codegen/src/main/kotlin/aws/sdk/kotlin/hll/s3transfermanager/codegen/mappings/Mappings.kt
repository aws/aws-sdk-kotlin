/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings

import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.uploadObject.uploadObjectConversions
import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.uploadObject.uploadObjectIoMappings

internal val ioMappings = uploadObjectIoMappings
internal val conversionMappings = uploadObjectConversions
