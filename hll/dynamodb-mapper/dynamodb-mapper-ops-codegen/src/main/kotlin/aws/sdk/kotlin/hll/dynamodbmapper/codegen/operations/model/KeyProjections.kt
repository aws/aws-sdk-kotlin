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

internal data class KeyProjection(
    val parentInterfaceStruct: Structure?,
    val interfaceStruct: Structure,
    val implStruct: Structure,
    val builderStruct: Structure,
)

internal val KeyProjection.keyType: StructureKeyType
    get() = interfaceStruct.keyType

internal class KeyProjections(private val projections: Map<StructureKeyType, KeyProjection>) {
    companion object {
        internal fun fromInterface(interfaceStruct: Structure): KeyProjections =
            KeyProjectionsBuilder(interfaceStruct).build()
    }

    val isKeyed = projections.keys.any { it != StructureKeyType.NONE }
    val unkeyedProjection = projections.getValue(StructureKeyType.NONE)

    operator fun get(keyType: StructureKeyType) = projections[keyType] ?: unkeyedProjection
}

private class KeyProjectionsBuilder(val baseStruct: Structure) {
    fun build(): KeyProjections = KeyProjections(projections().associateBy { it.keyType })

    private fun List<Member>.projectedAs(keyType: MemberKeyType): List<Member> = map { member ->
        member.copy(
            name = keyMemberName(member.name, keyType),
            type = keyType.keyTypeVar.nullable(member.type.nullable),
            attributes = member.attributes + attributesOf {
                MapperAttributes.MemberKeyType to keyType
                ModelAttributes.LowLevelMember to member
                MapperAttributes.CodegenBehavior to MemberCodegenBehavior.MapToKeys
            },
        )
    }

    private fun projection(interfaceStruct: Structure, parent: Structure? = null): KeyProjection {
        val implStruct = implFor(interfaceStruct)
        val builderStruct = builderFor(interfaceStruct, implStruct)
        return KeyProjection(parent, interfaceStruct, implStruct, builderStruct)
    }

    private fun projections(): List<KeyProjection> {
        val keyFields = baseStruct
            .takeIf { it.keyType == StructureKeyType.NONE && ModelAttributes.LowLevelStructure in it.attributes }
            ?.lowLevel
            ?.members
            ?.filter { it.codegenBehavior == MemberCodegenBehavior.MapToKeys }
            .orEmpty()

        return if (keyFields.isEmpty()) {
            listOf(projection(baseStruct))
        } else {
            val inheritedMembers = baseStruct.members.map {
                it.copy(attributes = it.attributes + (MapperAttributes.IsInherited to true))
            }.toSet()

            val pkMembers = keyFields.projectedAs(MemberKeyType.PARTITION)
            val pkGenerics = inheritedMembers.genericVars() + pkMembers.genericVars()
            val pkInterface = baseStruct.copy(
                type = baseStruct.type.copy(shortName = "${baseStruct.type.shortName}.PartitionKey", genericArgs = pkGenerics.toList()),
                members = inheritedMembers + pkMembers,
                attributes = baseStruct.attributes + (MapperAttributes.StructureKeyType to StructureKeyType.PARTITION_KEY),
            )

            val skMembers = keyFields.projectedAs(MemberKeyType.SORT)
            val ckGenerics = pkGenerics + skMembers.genericVars()
            val ckInterface = baseStruct.copy(
                type = baseStruct.type.copy(shortName = "${baseStruct.type.shortName}.CompositeKey", genericArgs = ckGenerics.toList()),
                members = inheritedMembers + pkMembers + skMembers,
                attributes = baseStruct.attributes + (MapperAttributes.StructureKeyType to StructureKeyType.COMPOSITE_KEY),
            )

            listOf(
                projection(baseStruct),
                projection(pkInterface, baseStruct),
                projection(ckInterface, baseStruct),
            )
        }
    }

    private fun implFor(interfaceStruct: Structure): Structure {
        val implName = "${interfaceStruct.type.shortName.replace(".", "")}Impl"

        val implType = interfaceStruct.type.copy(
            shortName = implName,
            genericArgs = interfaceStruct.members.genericVars().toList(),
        )

        return interfaceStruct.copy(members = interfaceStruct.members, type = implType)
    }

    private fun builderFor(interfaceStruct: Structure, implStruct: Structure): Structure {
        val builderName = "${interfaceStruct.type.shortName.replace(".", "")}Builder"
        return implStruct.copy(type = implStruct.type.copy(shortName = builderName))
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

internal val Structure.keyProjections: KeyProjections
    get() = attributes[MapperAttributes.KeyProjections]

internal val Structure.isKeyed: Boolean
    get() = keyProjections.isKeyed

internal val Operation.isKeyed: Boolean
    get() = request.isKeyed || response.isKeyed
