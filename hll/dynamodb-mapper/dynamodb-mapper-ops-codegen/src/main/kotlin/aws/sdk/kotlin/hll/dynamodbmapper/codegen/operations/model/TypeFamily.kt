/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.*
import aws.sdk.kotlin.hll.codegen.util.plus
import aws.sdk.kotlin.hll.codegen.util.uppercaseFirstChar
import aws.smithy.kotlin.runtime.collections.attributesOf
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.collections.merge
import aws.smithy.kotlin.runtime.collections.mutableAttributes

internal sealed interface TypeFamily {
    companion object {
        fun fromInterface(interfaceStruct: Structure): TypeFamily = TypeFamilyBuilder(interfaceStruct).build()
    }

    val parentInterfaceStruct: Structure?
    val interfaceStruct: Structure

    data class Concrete(
        override val parentInterfaceStruct: Structure?,
        override val interfaceStruct: Structure,
        val implStruct: Structure,
        val builderStruct: Structure,
        val keyType: StructureKeyType,
    ) : TypeFamily

    data class Abstract(
        override val parentInterfaceStruct: Structure?,
        override val interfaceStruct: Structure,
        val children: List<TypeFamily>,
    ) : TypeFamily
}

internal fun TypeFamily.leafTypes(): List<TypeFamily.Concrete> = when (this) {
    is TypeFamily.Concrete -> listOf(this)
    is TypeFamily.Abstract -> children.flatMap { it.leafTypes() }
}

internal fun TypeFamily.leafTypeOrDefault(type: StructureKeyType) =
    leafTypes().run { singleOrNull { it.keyType == type } ?: requireNotNull(singleOrNull()) {
        "TypeFamily ${interfaceStruct.type.shortName} leafTypeOrDefault($type) failed because leaf types found were: ${this.map { it.keyType }}"
    } }

internal val TypeFamily.isKeyed: Boolean
    get() = leafTypes().singleOrNull()?.keyType != StructureKeyType.NONE

private class TypeFamilyBuilder(val interfaceStruct: Structure) {
    private val children = deriveLineage(interfaceStruct)
    private val parents = children.entries.flatMap { (parent, children) ->
        children.map { child -> child to parent }
    }.toMap()

    fun build(): TypeFamily = typeFamilyFor(interfaceStruct)

    private fun typeFamilyFor(interfaceStruct: Structure): TypeFamily {
        val parentStruct = parents[interfaceStruct]
        val children = children[interfaceStruct].orEmpty().map { typeFamilyFor(it) }
        return if (children.isEmpty()) {
            val implStruct = implFor(interfaceStruct)
            val builderStruct = builderFor(interfaceStruct, implStruct)
            TypeFamily.Concrete(parentStruct, interfaceStruct, implStruct, builderStruct, interfaceStruct.keyType)
        } else {
            TypeFamily.Abstract(parentStruct, interfaceStruct, children)
        }
    }

    private fun implFor(interfaceStruct: Structure): Structure {
        val implName = "${interfaceStruct.type.shortName.replace(".", "")}Impl"

        val implType = interfaceStruct.type.copy(
            shortName = implName,
            genericArgs = interfaceStruct.members.genericVars().toList(),
        )
        val reifiedAttributes = mutableAttributes().apply {
            interfaceStruct.parent?.attributes?.let(::merge)
            merge(interfaceStruct.attributes)
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

private fun deriveLineage(struct: Structure) = buildList {
    fun recurse(parent: Structure) {
        parent.projectKeyTypes().forEach { child ->
            add(parent to child)
            recurse(child)
        }
    }

    recurse(struct)
}.groupBy({ it.first }, { it.second })

private fun Structure.projectKeyTypes(): List<Structure> {
    val keyFields = this
        .takeIf { keyType == StructureKeyType.NONE && ModelAttributes.LowLevelStructure in attributes }
        ?.lowLevel
        ?.members
        ?.filter { it.codegenBehavior == MemberCodegenBehavior.MapToKeys }
        .orEmpty()

    return if (keyFields.isEmpty()) {
        // type doesn't vary based on key structure
        listOf()
    } else {
        fun List<Member>.projectedAs(keyType: MemberKeyType): List<Member> = map { llMember ->
            llMember.copy(
                name = keyMemberName(llMember.name, keyType),
                type = keyType.keyTypeVar.nullable(llMember.type.nullable),
                attributes = llMember.attributes + attributesOf {
                    MapperAttributes.MemberKeyType to keyType
                    ModelAttributes.LowLevelMember to llMember
                    MapperAttributes.CodegenBehavior to MemberCodegenBehavior.MapToKeys
                },
            )
        }

        val inheritedMembers = members.map {
            it.copy(attributes = it.attributes + (MapperAttributes.IsInherited to true))
        }.toSet()

        val pkMembers = keyFields.projectedAs(MemberKeyType.PARTITION)
        val pkGenerics = inheritedMembers.genericVars() + pkMembers.genericVars()
        val pkProjection = copy(
            type = type.copy(shortName = "${type.shortName}.PartitionKey", genericArgs = pkGenerics.toList()),
            members = inheritedMembers + pkMembers,
            attributes = attributes + (MapperAttributes.StructureKeyType to StructureKeyType.PARTITION_KEY),
        )

        val skMembers = keyFields.projectedAs(MemberKeyType.SORT)
        val ckGenerics = pkGenerics + skMembers.genericVars()
        val ckProjection = copy(
            type = type.copy(shortName = "${type.shortName}.CompositeKey", genericArgs = ckGenerics.toList()),
            members = inheritedMembers + pkMembers + skMembers,
            attributes = attributes + (MapperAttributes.StructureKeyType to StructureKeyType.COMPOSITE_KEY),
        )

        listOf(pkProjection, ckProjection)
    }
}

private fun keyMemberName(llMemberName: String, keyType: MemberKeyType): String {
    val specialization = keyType.name.lowercase()
    return when {
        llMemberName.startsWith("key") -> "$specialization${llMemberName.uppercaseFirstChar}"
        "Key" in llMemberName -> llMemberName.replace("Key", "${specialization.uppercaseFirstChar}Key")
        else -> error("Unsupported key member name '$llMemberName'")
    }
}

internal val Structure.typeFamily: TypeFamily
    get() = attributes[MapperAttributes.TypeFamily]
