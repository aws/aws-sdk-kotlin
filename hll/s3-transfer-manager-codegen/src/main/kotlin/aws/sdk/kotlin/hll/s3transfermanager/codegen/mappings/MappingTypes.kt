/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings

import aws.sdk.kotlin.hll.codegen.model.TypeRef

/**
 * Converts one type to another
 */
internal data class ConversionMapping(
    val source: TypeRef,
    val destination: TypeRef,
    val members: Set<String>,
    val additionalImports: List<TypeRef> = emptyList(),
    val additionalParameters: List<String> = emptyList(),
    val additionalLogic: String = "",
)

/**
 * High level S3 TM request/response from low level S3 operation members
 */
internal data class IoMapping(
    val type: MappingType,
    val className: String,
    val sourceOperation: String,
    val members: Set<String>,
)

internal enum class MappingType {
    /**
     * Maps high level operation request members to low level request members
     */
    REQUEST,

    /**
     * Maps high level operation response members to low level response members
     */
    RESPONSE,
}
