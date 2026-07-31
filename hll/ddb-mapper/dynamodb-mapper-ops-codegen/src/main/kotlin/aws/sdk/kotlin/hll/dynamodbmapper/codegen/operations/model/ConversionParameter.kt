/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.Structure
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes

internal data class ConversionParameter(val name: String, val type: TypeRef, val argValue: String) {
    companion object {
        fun fromInterface(interfaceStruct: Structure) = deriveParams(interfaceStruct)
    }
}

private fun deriveParams(struct: Structure) = when (struct.type.shortName) {
    "BatchGetItemResponse" -> listOf(
        ConversionParameter(
            "requestTables",
            Types.Kotlin.list(MapperTypes.Operations.BatchGetItemRequestTable),
            "ctx.highLevelRequest.tables",
        ),
    )

    "BatchWriteItemResponse" -> listOf(
        ConversionParameter(
            "requestTables",
            Types.Kotlin.list(MapperTypes.Operations.BatchWriteItemRequestTable),
            "ctx.highLevelRequest.tables",
        ),
    )

    "TransactGetItemsResponse" -> listOf(
        ConversionParameter(
            "requestTables",
            Types.Kotlin.list(MapperTypes.Operations.TransactGetItemsRequestTable),
            "ctx.highLevelRequest.tables",
        ),
    )

    else -> listOf()
}

internal val Structure.conversionParameters: List<ConversionParameter>
    get() = attributes.getOrNull(MapperAttributes.ConversionParameters).orEmpty()
