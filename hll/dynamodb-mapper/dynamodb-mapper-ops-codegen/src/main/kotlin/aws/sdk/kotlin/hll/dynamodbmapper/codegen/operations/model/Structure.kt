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
 * Derives a high-level, unprojected [Structure] equivalent for this low-level structure. The applicable set of keyed
 * projections will be present in the returned [Structure]'s [keyProjections] extension property.
 * @param ctx The active [RenderContext]
 */
internal fun Structure.toHighLevel(ctx: RenderContext): Structure {
    val llStructure = this@toHighLevel
    ctx.info("For ${llStructure.type.fullName}:")

    val hlMembers = llStructure.members.mapNotNull { llMember ->
        val behavior = llMember.codegenBehavior
        ctx.info("  ${llMember.name} -> $behavior")
        val hlMember = when (behavior) {
            MemberCodegenBehavior.PassThrough -> llMember
            MemberCodegenBehavior.MapToObject -> llMember.copy(type = TypeVar.T.nullable(llMember.type.nullable))
            MemberCodegenBehavior.MapToKeys -> null // will be added later by TypeFamily
            MemberCodegenBehavior.ListMapToObject -> deriveListMapToObject(llMember)
            is MemberCodegenBehavior.ExpressionLiteral -> deriveExpressionLiteral(llMember, behavior.type)
            else -> null
        }

        hlMember?.copy(
            attributes = hlMember.attributes + attributesOf {
                ModelAttributes.LowLevelMember to llMember
                MapperAttributes.CodegenBehavior to behavior
            },
        )
    }.toSet()

    val genericArgs = hlMembers.genericVars()
    val hlType = TypeRef(ctx.pkg, llStructure.type.shortName, genericArgs)
    val hlAttributes = llStructure.attributes + (ModelAttributes.LowLevelStructure to llStructure)
    return Structure(hlType, hlMembers, hlAttributes).addKeyProjections()
}

private fun deriveExpressionLiteral(llMember: Member, type: ExpressionLiteralType): Member? {
    val expressionType = when (type) {
        ExpressionLiteralType.Filter -> MapperTypes.Expressions.BooleanExpr
        ExpressionLiteralType.KeyCondition -> MapperTypes.Expressions.KeyFilter

        // TODO add support for other expression types
        else -> return null
    }.nullable(llMember.type.nullable)

    val dslInfo = when (type) {
        ExpressionLiteralType.Filter -> DslInfo(
            interfaceType = MapperTypes.Expressions.Filter,
            implType = MapperTypes.Expressions.Internal.FilterImpl,
            implSingleton = true,
        )

        // KeyCondition doesn't use a top-level DSL (SortKeyCondition is nested)
        ExpressionLiteralType.KeyCondition -> null

        // TODO add support for other expression types
        else -> return null
    }

    return llMember.copy(
        name = llMember.name.removeSuffix("Expression"),
        type = expressionType,
        attributes = llMember.attributes + (ModelAttributes.DslInfo to dslInfo),
    )
}

private fun deriveListMapToObject(llMember: Member): Member {
    val llListType = requireNotNull(llMember.type as? TypeRef) {
        "`ListMapToObject` member is required to be a TypeRef"
    }
    val hlListType = llListType.copy(genericArgs = listOf(TypeVar.T))
    return llMember.copy(type = hlListType)
}

private fun Structure.addKeyProjections(): Structure =
    copy(attributes = attributes + (MapperAttributes.KeyProjections to KeyProjections.fromInterface(this)))

/**
 * Iterate over the members of this structure and execute the inner [block]. If a [MemberCodegenBehavior] is passed,
 * only matching members are iterated.
 * @param memberCodegenBehavior The optional behavior by which to filter members.
 * @param block The lambda to execute for each member
 */
internal inline fun Structure.members(
    memberCodegenBehavior: MemberCodegenBehavior? = null,
    crossinline block: Member.() -> Unit,
) {
    val list = when (memberCodegenBehavior) {
        null -> members
        else -> members.filter { it.codegenBehavior == memberCodegenBehavior }
    }
    list.forEach { it.block() }
}
