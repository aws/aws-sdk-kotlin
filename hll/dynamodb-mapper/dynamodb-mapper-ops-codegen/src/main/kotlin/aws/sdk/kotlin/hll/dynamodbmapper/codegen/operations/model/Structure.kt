/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.*
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.info
import aws.sdk.kotlin.hll.codegen.util.plus
import aws.sdk.kotlin.hll.codegen.util.uppercaseFirstChar
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import aws.smithy.kotlin.runtime.collections.attributesOf

/**
 * Derives a high-level [Structure] equivalent for this low-level structure
 * @param pkg The Kotlin package to use for the high-level structure
 */
internal fun Structure.toHighLevel(pkg: String, ctx: RenderContext): Structure {
    val llStructure = this@toHighLevel
    ctx.info("For ${llStructure.type.fullName}:")

    val hlMembers = llStructure.members.mapNotNull { llMember ->
        val nullable = llMember.type.nullable
        val behavior = llMember.codegenBehavior
        ctx.info("  ${llMember.name} -> $behavior")
        val hlMember = when (behavior) {
            MemberCodegenBehavior.PassThrough -> llMember
            MemberCodegenBehavior.MapToObject -> llMember.copy(type = TypeVar.T.nullable(nullable))
            MemberCodegenBehavior.MapToKeys -> null // will be added by addHighLevelVariants()

            MemberCodegenBehavior.ListMapToObject -> {
                val llListType = llMember.type as? TypeRef ?:
                    error("`ListMapToObject` member is required to be a TypeRef")
                val hlListType = llListType.copy(genericArgs = listOf(TypeVar.T))
                llMember.copy(type = hlListType)
            }

            is MemberCodegenBehavior.ExpressionLiteral -> {
                val expressionType = when (behavior.type) {
                    ExpressionLiteralType.Filter -> MapperTypes.Expressions.BooleanExpr
                    ExpressionLiteralType.KeyCondition -> MapperTypes.Expressions.KeyFilter

                    // TODO add support for other expression types
                    else -> return@mapNotNull null
                }.nullable(nullable)

                val dslInfo = when (behavior.type) {
                    ExpressionLiteralType.Filter -> DslInfo(
                        interfaceType = MapperTypes.Expressions.Filter,
                        implType = MapperTypes.Expressions.Internal.FilterImpl,
                        implSingleton = true,
                    )

                    // KeyCondition doesn't use a top-level DSL (SortKeyCondition is nested)
                    ExpressionLiteralType.KeyCondition -> null

                    // TODO add support for other expression types
                    else -> return@mapNotNull null
                }

                llMember.copy(
                    name = llMember.name.removeSuffix("Expression"),
                    type = expressionType,
                    attributes = llMember.attributes + (ModelAttributes.DslInfo to dslInfo),
                )
            }

            else -> null
        }

        hlMember?.copy(attributes = hlMember.attributes + attributesOf {
            ModelAttributes.LowLevelMember to llMember
            MapperAttributes.CodegenBehavior to behavior
        })
    }.toSet()

    val genericArgs = hlMembers.genericVars()
    val hlType = TypeRef(pkg, llStructure.type.shortName, genericArgs)
    val hlAttributes = llStructure.attributes + (ModelAttributes.LowLevelStructure to llStructure)
    return Structure(hlType, hlMembers, hlAttributes)
        .addHighLevelVariants(ctx)
        .addTypeFamily()
}

private fun Structure.addHighLevelVariants(ctx: RenderContext): Structure {
    val keyFields = lowLevel.members.filter { it.codegenBehavior == MemberCodegenBehavior.MapToKeys }

    val variants = if (keyFields.isEmpty()) {
        // no additional variants needed
        listOf(
            copy(attributes = attributes + (MapperAttributes.StructureKeyType to StructureKeyType.NONE)),
        )
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
        val pkVariant = copy(
            type = type.copy(shortName = "${type.shortName}.PartitionKey", genericArgs = pkGenerics.toList()),
            members = inheritedMembers + pkMembers,
            attributes = attributes + (MapperAttributes.StructureKeyType to StructureKeyType.PARTITION_KEY),
        )

        val skMembers = keyFields.projectedAs(MemberKeyType.SORT)
        val ckGenerics = pkGenerics + skMembers.genericVars()
        val ckVariant = copy(
            type = type.copy(shortName = "${type.shortName}.CompositeKey", genericArgs = ckGenerics.toList()),
            members = inheritedMembers + pkMembers + skMembers,
            attributes = attributes + (MapperAttributes.StructureKeyType to StructureKeyType.COMPOSITE_KEY),
        )

        listOf(pkVariant, ckVariant)
    }

    val newAttributes =  attributes + (MapperAttributes.Variants to variants.also {
        ctx.info("Variants for ${type.fullName}: ${variants.map { it.keyType }}")
    })
    return copy(attributes = newAttributes)
}

private fun Structure.addTypeFamily(): Structure =
    copy(attributes = attributes + (MapperAttributes.TypeFamily to TypeFamily.fromInterface(this)))

private fun keyMemberName(llMemberName: String, keyType: MemberKeyType): String {
    val specialization = keyType.name.lowercase()
    return when {
        llMemberName.startsWith("key") -> "$specialization${llMemberName.uppercaseFirstChar}"
        "Key" in llMemberName -> llMemberName.replace("Key", "${specialization.uppercaseFirstChar}Key")
        else -> error("Unsupported key member name '$llMemberName'")
    }
}

internal val Structure.variants: List<Structure>
    get() = attributes.getOrNull(MapperAttributes.Variants).orEmpty()

internal inline fun Structure.members(
    memberCodegenBehavior: MemberCodegenBehavior? = null,
    crossinline block: Member.() -> Unit,
) {
    val list = if (memberCodegenBehavior == null) {
        members
    } else {
        members.filter { it.codegenBehavior == memberCodegenBehavior }
    }
    list.forEach { it.block() }
}

internal fun Structure.isKeyed() = variants.any { it.keyType != StructureKeyType.NONE }
