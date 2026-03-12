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
import aws.smithy.kotlin.runtime.collections.AttributeKey
import aws.smithy.kotlin.runtime.collections.attributesOf
import aws.smithy.kotlin.runtime.collections.toMutableAttributes

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
            is MemberCodegenBehavior.CustomTransformation -> behavior.replacementMember
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
    val hlStructure = Structure(hlType, hlMembers, hlAttributes)

    return hlStructure
        .withAttribute(ModelAttributes.LowLevelStructure, llStructure)
        .withAttribute(MapperAttributes.ConversionParameters, ConversionParameter::fromInterface)
        .withAttribute(MapperAttributes.KeyProjections, KeyProjections::fromInterface)
}

private fun <T : Any> Structure.withAttribute(key: AttributeKey<T>, value: T) = copy(
    attributes = attributes.toMutableAttributes().apply { set(key, value) },
)

private fun <T : Any> Structure.withAttribute(
    key: AttributeKey<T>,
    valueSupplier: (Structure) -> T,
) = withAttribute(key, valueSupplier(this))

private fun deriveExpressionLiteral(llMember: Member, type: ExpressionLiteralType): Member? {
    val expressionType = when (type) {
        ExpressionLiteralType.Filter -> MapperTypes.Expressions.BooleanExpr
        ExpressionLiteralType.KeyCondition -> MapperTypes.Expressions.KeyFilter
        ExpressionLiteralType.Update -> MapperTypes.Expressions.UpdateExpr

        // TODO add support for other expression types
        else -> return null
    }.nullable(llMember.type.nullable)

    val dslInfo = when (type) {
        ExpressionLiteralType.Filter -> DslInfo(
            interfaceType = MapperTypes.Expressions.FilterDsl,
            implType = MapperTypes.Expressions.Internal.FilterDslImpl,
            implInvocationStyle = DslInvocationStyle.Singleton,
        )

        // KeyCondition doesn't use a top-level DSL (SortKeyCondition is nested)
        ExpressionLiteralType.KeyCondition -> null

        ExpressionLiteralType.Update -> DslInfo(
            interfaceType = MapperTypes.Expressions.UpdateDsl,
            implType = MapperTypes.Expressions.Internal.UpdateDslImpl,
            implFinalizer = ".toExpression()",
        )
    }

    return llMember.copy(
        name = llMember.name.removeSuffix("Expression"),
        type = expressionType,
        attributes = llMember.attributes.toMutableAttributes().apply {
            if (dslInfo != null) dsls += dslInfo
        },
    )
}

private fun deriveListMapToObject(llMember: Member): Member {
    val llListType = requireNotNull(llMember.type as? TypeRef) {
        "`ListMapToObject` member is required to be a TypeRef"
    }
    val hlListType = llListType.copy(genericArgs = listOf(TypeVar.T))
    return llMember.copy(type = hlListType)
}

/**
 * Iterate over the members of this structure and execute the inner [block]. If a [MemberCodegenBehavior] is passed,
 * only matching members are iterated.
 * @param block The lambda to execute for each member
 */
@JvmName("membersAll")
internal inline fun Structure.members(crossinline block: Member.() -> Unit) = members({ true }, block)

/**
 * Iterate over the members of this structure and execute the inner [block]. If a [MemberCodegenBehavior] is passed,
 * only matching members are iterated.
 * @param memberCodegenBehavior The behavior by which to filter members
 * @param block The lambda to execute for each member
 */
@JvmName("membersByBehavior")
internal inline fun Structure.members(
    memberCodegenBehavior: MemberCodegenBehavior,
    crossinline block: Member.() -> Unit,
) = members({ it == memberCodegenBehavior }, block)

/**
 * Iterate over the members of this structure and execute the inner [block]. If a [MemberCodegenBehavior] is passed,
 * only matching members are iterated.
 * @param block The lambda to execute for each member
 */
@JvmName("membersByGenericType")
internal inline fun <reified T : MemberCodegenBehavior> Structure.members(
    crossinline block: Member.() -> Unit,
) = members({ it is T }, block)

private inline fun Structure.members(
    behaviorPredicate: (MemberCodegenBehavior) -> Boolean,
    crossinline block: Member.() -> Unit,
) = members.filter { behaviorPredicate(it.codegenBehavior) }.forEach(block)
