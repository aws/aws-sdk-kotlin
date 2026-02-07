/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.GenericsSet
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
        isKeyed -> listOf(StructureKeyType.PARTITION_KEY, StructureKeyType.COMPOSITE_KEY)
        else -> listOf(StructureKeyType.NONE)
    }

private val unkeyedTypeVars = GenericsSet(TypeVar.T)
private val pkTypeVars = unkeyedTypeVars + MapperTypes.Items.KeyTypeAsPK
private val ckTypeVars = pkTypeVars + MapperTypes.Items.KeyTypeAsSK

internal val StructureKeyType.typeVars: GenericsSet
    get() = when (this) {
        StructureKeyType.NONE -> unkeyedTypeVars
        StructureKeyType.PARTITION_KEY -> pkTypeVars
        StructureKeyType.COMPOSITE_KEY -> ckTypeVars
    }

internal val StructureKeyType.nameSuffix: String
    get() = when (this) {
        StructureKeyType.NONE -> ""
        StructureKeyType.PARTITION_KEY -> ".PartitionKey"
        StructureKeyType.COMPOSITE_KEY -> ".CompositeKey"
    }
