/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.*
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.info
import aws.sdk.kotlin.hll.codegen.util.plus
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
            MemberCodegenBehavior.MapToKeys -> null // will be added later by TypeFamily

            MemberCodegenBehavior.ListMapToObject -> {
                val llListType = requireNotNull(llMember.type as? TypeRef) {
                    "`ListMapToObject` member is required to be a TypeRef"
                }
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
    return Structure(hlType, hlMembers, hlAttributes).addKeyProjections()
}

private fun Structure.addKeyProjections(): Structure =
    copy(attributes = attributes + (MapperAttributes.KeyProjections to KeyProjections.fromInterface(this)))

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
