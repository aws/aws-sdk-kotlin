/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.*
import aws.sdk.kotlin.hll.codegen.util.plus
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes

/**
 * Derives a high-level [Structure] equivalent for this low-level structure
 * @param pkg The Kotlin package to use for the high-level structure
 */
internal fun Structure.toHighLevel(pkg: String): Structure {
    val llStructure = this@toHighLevel

    val hlType = TypeRef(pkg, llStructure.type.shortName, listOf(TypeVar("T")))

    val hlMembers = llStructure.members.mapNotNull { llMember ->
        val nullable = llMember.type.nullable

        val hlMember = when (val behavior = llMember.codegenBehavior) {
            MemberCodegenBehavior.PassThrough -> llMember.copy(
                attributes = attributes + (ModelAttributes.GeneratedApi to true),
            )

            MemberCodegenBehavior.MapAll, MemberCodegenBehavior.MapKeys ->
                llMember.copy(type = TypeVar("T", nullable))

            MemberCodegenBehavior.ListMapAll -> {
                val llListType = llMember.type as? TypeRef ?: error("`ListMapAll` member is required to be a TypeRef")
                val hlListType = llListType.copy(genericArgs = listOf(TypeVar("T")))
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

        hlMember?.copy(attributes = hlMember.attributes + (ModelAttributes.LowLevelMember to llMember))
    }.toSet()

    val hlAttributes = llStructure.attributes + (ModelAttributes.LowLevelStructure to llStructure)

    return Structure(hlType, hlMembers, hlAttributes)
}
