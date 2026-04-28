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

internal enum class KeyProjectionType {
    NONE,
    PARTITION_KEY,
    COMPOSITE_KEY,
}

internal val Structure.keyType: KeyProjectionType
    get() = attributes.getOrNull(MapperAttributes.KeyProjectionType) ?: KeyProjectionType.NONE

internal val Operation.keyTypes: List<KeyProjectionType>
    get() = when {
        isKeyed -> listOf(KeyProjectionType.PARTITION_KEY, KeyProjectionType.COMPOSITE_KEY)
        else -> listOf(KeyProjectionType.NONE)
    }

private val unkeyedTypeVars = GenericsSet(TypeVar.T)
private val pkTypeVars = unkeyedTypeVars + MapperTypes.Items.KeyTypeAsPK
private val ckTypeVars = pkTypeVars + MapperTypes.Items.KeyTypeAsSK

internal val KeyProjectionType.typeVars: GenericsSet
    get() = when (this) {
        KeyProjectionType.NONE -> unkeyedTypeVars
        KeyProjectionType.PARTITION_KEY -> pkTypeVars
        KeyProjectionType.COMPOSITE_KEY -> ckTypeVars
    }

internal val KeyProjectionType.nameSuffix: String
    get() = when (this) {
        KeyProjectionType.NONE -> ""
        KeyProjectionType.PARTITION_KEY -> ".PartitionKey"
        KeyProjectionType.COMPOSITE_KEY -> ".CompositeKey"
    }
