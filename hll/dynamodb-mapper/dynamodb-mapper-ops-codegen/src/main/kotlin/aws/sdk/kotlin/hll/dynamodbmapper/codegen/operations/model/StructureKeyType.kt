/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.Operation
import aws.sdk.kotlin.hll.codegen.model.Structure
import aws.sdk.kotlin.hll.codegen.model.TypeVar
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes

internal enum class StructureKeyType {
    NONE,
    PARTITION_KEY,
    COMPOSITE_KEY,
}

internal val Structure.keyType: StructureKeyType
    get() = attributes.getOrNull(MapperAttributes.StructureKeyType) ?: StructureKeyType.NONE

internal val Operation.keyTypes: List<StructureKeyType>
    get() = when {
        isKeyed() -> listOf(StructureKeyType.PARTITION_KEY, StructureKeyType.COMPOSITE_KEY)
        else -> listOf(StructureKeyType.NONE)
    }

private val unkeyedTypeArgs = listOf(TypeVar.T)
private val pkTypeArgs = unkeyedTypeArgs + MapperTypes.Items.KeyTypeAsPK
private val ckTypeArgs = pkTypeArgs + MapperTypes.Items.KeyTypeAsSK

internal val StructureKeyType.typeArgs: List<TypeVar>
    get() = when (this) {
        StructureKeyType.NONE -> unkeyedTypeArgs
        StructureKeyType.PARTITION_KEY -> pkTypeArgs
        StructureKeyType.COMPOSITE_KEY -> ckTypeArgs
    }

internal val StructureKeyType.nameSuffix: String
    get() = when (this) {
        StructureKeyType.NONE -> ""
        StructureKeyType.PARTITION_KEY -> ".PartitionKey"
        StructureKeyType.COMPOSITE_KEY -> ".CompositeKey"
    }
