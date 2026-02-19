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

/**
 * Represents the projection of a [KeyProjectionType] onto a low-level DynamoDB type, which results in a new set of
 * related structures used by high-level codegen
 * @param parentInterfaceStruct The parent (i.e., container) type for this projection's [interfaceStruct]
 * @param interfaceStruct A structure which defines the public immutable type for this projection
 * @param implStruct A structure which defines the private implementation type for this projection
 * @param builderStruct A structure which defines the public builder type for this projection
 */
internal data class KeyProjection(
    val parentInterfaceStruct: Structure?,
    val interfaceStruct: Structure,
    val implStruct: Structure,
    val builderStruct: Structure,
)

/**
 * The type of this key projection
 */
internal val KeyProjection.keyType: KeyProjectionType
    get() = interfaceStruct.keyType

/**
 * A family of [KeyProjection] instances for a low-level DynamoDB type. Depending on the low-level type, this set will
 * contain:
 * * A single projection ([KeyProjectionType.NONE]) for _unkeyed structures_ (e.g., the low-level type
 *   [aws.sdk.kotlin.services.dynamodb.model.GetItemResponse] is unkeyed because it contains no field that represents an
 *   item key)
 * * All projections ([KeyProjectionType.NONE], [KeyProjectionType.PARTITION_KEY], and
 *   [KeyProjectionType.COMPOSITE_KEY]) for _keyed structures_ (e.g., the low-level type
 *   [aws.sdk.kotlin.services.dynamodb.model.GetItemRequest] is keyed because it contains the field `key` which is used
 *   for fetching an item)
 */
internal class KeyProjections(private val projections: Map<KeyProjectionType, KeyProjection>) {
    companion object {
        internal fun fromInterface(interfaceStruct: Structure): KeyProjections = KeyProjectionsBuilder(interfaceStruct).build()
    }

    /**
     * Identifies whether this family of projections contains keyed projections (`true`) or only contains the unkeyed
     * projection (`false`)
     */
    val isKeyed = projections.keys.any { it != KeyProjectionType.NONE }

    /**
     * Gets the unkeyed projection from this projection family
     */
    val unkeyedProjection = projections.getValue(KeyProjectionType.NONE)

    /**
     * Gets a projection based on the _desired_ projection type. If no projection of that type exists, returns the
     * unkeyed projection.
     */
    operator fun get(keyType: KeyProjectionType) = projections[keyType] ?: unkeyedProjection
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
            .takeIf { it.keyType == KeyProjectionType.NONE && ModelAttributes.LowLevelStructure in it.attributes }
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
                attributes = baseStruct.attributes + (MapperAttributes.KeyProjectionType to KeyProjectionType.PARTITION_KEY),
            )

            val skMembers = keyFields.projectedAs(MemberKeyType.SORT)
            val ckGenerics = pkGenerics + skMembers.genericVars()
            val ckInterface = baseStruct.copy(
                type = baseStruct.type.copy(shortName = "${baseStruct.type.shortName}.CompositeKey", genericArgs = ckGenerics.toList()),
                members = inheritedMembers + pkMembers + skMembers,
                attributes = baseStruct.attributes + (MapperAttributes.KeyProjectionType to KeyProjectionType.COMPOSITE_KEY),
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

/**
 * Gets the family of associated key type projections for this structure. Throws an error if this structure has not been
 * associated with any key projections.
 */
internal val Structure.keyProjections: KeyProjections
    get() = attributes[MapperAttributes.KeyProjections]

/**
 * Determines if this is a keyed structure. For example, the low-level type
 * [aws.sdk.kotlin.services.dynamodb.model.GetItemResponse] is unkeyed because it contains no field that represents an
 * item key. By contrast, the low-level type [aws.sdk.kotlin.services.dynamodb.model.GetItemRequest] is keyed because it
 * contains the field `key` which is used for fetching the item.
 */
internal val Structure.isKeyed: Boolean
    get() = keyProjections.isKeyed

/**
 * Determines if this is a keyed operation. A keyed operation has a request and/or a response structure which itself is
 * keyed (i.e., [Structure.isKeyed]). For example, the `PutItem` operation is unkeyed because neither its request type
 * nor response type contain fields which represent an item key. By contrast, the `DeleteItem` operation is keyed
 * because its request type contains the field `key` which is used for deleting the item.
 */
internal val Operation.isKeyed: Boolean
    get() = request.isKeyed || response.isKeyed
