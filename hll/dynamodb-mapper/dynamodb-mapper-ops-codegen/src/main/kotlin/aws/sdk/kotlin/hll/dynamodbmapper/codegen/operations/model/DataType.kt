/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.Structure
import aws.sdk.kotlin.hll.codegen.model.genericVars
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.collections.merge
import aws.smithy.kotlin.runtime.collections.mutableAttributes

internal sealed interface DataType {
    companion object {
        fun fromInterface(interfaceStruct: Structure): DataType = DataTypeBuilder(interfaceStruct).build()
    }

    val parentInterfaceStruct: Structure?
    val interfaceStruct: Structure

    data class Concrete(
        override val parentInterfaceStruct: Structure?,
        override val interfaceStruct: Structure,
        val implStruct: Structure,
        val builderStruct: Structure,
        val keyType: StructureKeyType,
    ) : DataType

    data class Abstract(
        override val parentInterfaceStruct: Structure?,
        override val interfaceStruct: Structure,
        val variants: List<DataType>,
    ) : DataType
}

internal fun DataType.leafTypes(): List<DataType.Concrete> = when (this) {
    is DataType.Concrete -> listOf(this)
    is DataType.Abstract -> variants.flatMap { it.leafTypes() }
}

internal fun DataType.leafTypeOrDefault(type: StructureKeyType) =
    leafTypes().run { singleOrNull { it.keyType == type } ?: single() }

private class DataTypeBuilder(val interfaceStruct: Structure) {
    private val parents = deriveLineage(interfaceStruct)

    fun build(): DataType = dataTypeFor(null, interfaceStruct)

    private val Structure.isConcreteUnkeyed: Boolean
        get() = variants.isEmpty() || (variants.singleOrNull()?.keyType == StructureKeyType.NONE)

    private fun dataTypeFor(parentStruct: Structure?, interfaceStruct: Structure): DataType =
        if (interfaceStruct.isConcreteUnkeyed) {
            val implStruct = implFor(interfaceStruct)
            val builderStruct = builderFor(interfaceStruct, implStruct)
            DataType.Concrete(parentStruct, interfaceStruct, implStruct, builderStruct, interfaceStruct.keyType)
        } else {
            val variants = interfaceStruct.variants.map { dataTypeFor(interfaceStruct, it) }
            DataType.Abstract(parentStruct, interfaceStruct, variants)
        }

    private fun implFor(interfaceStruct: Structure): Structure {
        val implName = "${interfaceStruct.type.shortName.replace(".", "")}Impl"

        val implType = interfaceStruct.type.copy(
            shortName = implName,
            genericArgs = interfaceStruct.members.genericVars(),
        )
        val reifiedAttributes = mutableAttributes().apply {
            interfaceStruct.parent?.attributes?.let(::merge)
            merge(interfaceStruct.attributes)
            remove(MapperAttributes.Variants)
        }

        return interfaceStruct.copy(
            members = interfaceStruct.members,
            type = implType,
            attributes = reifiedAttributes,
        )
    }

    private fun builderFor(interfaceStruct: Structure, implStruct: Structure): Structure {
        val builderName = "${interfaceStruct.type.shortName.replace(".", "")}Builder"
        return implStruct.copy(type = implStruct.type.copy(shortName = builderName))
    }

    private val Structure.parent: Structure? get() = parents[this]
}

private fun deriveLineage(struct: Structure) = buildMap {
    fun recurse(parent: Structure) {
        parent.variants.forEach { child ->
            put(child, parent)
            recurse(child)
        }
    }

    recurse(struct)
}

internal val Structure.dataType: DataType
    get() = attributes[MapperAttributes.DataType]
