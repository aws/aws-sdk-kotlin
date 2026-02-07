/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.rendering

import aws.sdk.kotlin.hll.codegen.core.ImportDirective
import aws.sdk.kotlin.hll.codegen.model.*
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.*

internal abstract class IoRenderer(
    protected val ctx: RenderContext,
    private val keyProjections: KeyProjections,
) : RendererBase(ctx, keyProjections.unkeyedProjection.interfaceStruct.type.shortName) {
    protected abstract val fromProjections: KeyProjections
    protected abstract val toProjections: KeyProjections
    protected abstract fun Member.fromMember(fromStruct: Structure): Member
    protected abstract fun Member.fromMembers(fromStruct: Structure): List<Member>

    protected abstract fun renderKeyConversion(fromStruct: Structure, toStruct: Structure)
    protected abstract fun renderSingleItemConversion()

    final override fun generate() {
        KeyProjectedTypeRenderer(ctx, this, keyProjections).generate()

        // Manually import the low-level type with a specific alias
        val llType = keyProjections.unkeyedProjection.interfaceStruct.lowLevel.type
        imports += ImportDirective(llType, "LowLevel${llType.shortName}")

        val isKeyed = fromProjections.isKeyed || toProjections.isKeyed
        val keyTypes = when {
            isKeyed -> listOf(StructureKeyType.PARTITION_KEY, StructureKeyType.COMPOSITE_KEY)
            else -> listOf(StructureKeyType.NONE)
        }

        keyTypes.forEach { keyType ->
            val fromKeyType = fromProjections[keyType]
            val toKeyType = toProjections[keyType]
            renderConversion(keyType, fromKeyType.interfaceStruct, toKeyType.interfaceStruct)
        }
    }

    private fun renderConversion(keyType: StructureKeyType, fromStruct: Structure, toStruct: Structure) {
        val schemaType = when (keyType) {
            StructureKeyType.NONE -> MapperTypes.Items.ItemSchema
            StructureKeyType.PARTITION_KEY -> MapperTypes.Items.ItemSchemaPartitionKey
            StructureKeyType.COMPOSITE_KEY -> MapperTypes.Items.ItemSchemaCompositeKey
        }

        val generics = fromStruct.type.genericVars() + toStruct.type.genericVars() + TypeVar.T

        blankLine()
        withBlock("internal fun #G#T.convert(", "}", generics, fromStruct.type) {
            toStruct.members(MemberCodegenBehavior.Hoist) { write("#L: #T,", name, type) }
            write("schema: #T,", schemaType)
            closeAndOpenBlock(") = #T {", toStruct.type)

            toStruct.members(MemberCodegenBehavior.PassThrough) {
                write("#L = this@convert.#L", name, fromMember(fromStruct).name)
            }

            renderKeyConversion(fromStruct, toStruct)

            toStruct.members(MemberCodegenBehavior.MapToObject) {
                withBlock("#L = this@convert.#L?.let {", "}", name, fromMember(fromStruct).name) {
                    writeInline("schema.converter.")
                    renderSingleItemConversion()
                    blankLine()
                }
            }

            toStruct.members(MemberCodegenBehavior.ListMapToObject) {
                withBlock("#L = this@convert.#L?.map {", "}", name, fromMember(fromStruct).name) {
                    writeInline("schema.converter.")
                    renderSingleItemConversion()
                    blankLine()
                }
            }

            toStruct.members(MemberCodegenBehavior.Hoist) { write("this.#1L = #1L", name) }

            if (toStruct.members.any { it.codegenBehavior.isExpression }) {
                blankLine()
                write("val expressionVisitor = #T()", MapperTypes.Expressions.Internal.ParameterizingExpressionVisitor)

                toStruct.members(MemberCodegenBehavior.ExpressionLiteral(ExpressionLiteralType.Filter)) {
                    write("#L = this@convert.#L?.accept(expressionVisitor)", name, fromMember(fromStruct).name)
                }

                toStruct.members(MemberCodegenBehavior.ExpressionLiteral(ExpressionLiteralType.KeyCondition)) {
                    write(
                        "#L = this@convert.#L?.#T(schema)?.accept(expressionVisitor)",
                        name,
                        fromMember(fromStruct).name,
                        MapperTypes.Expressions.Internal.toExpression,
                    )
                }

                toStruct.members(MemberCodegenBehavior.ExpressionArguments(ExpressionArgumentsType.AttributeNames)) {
                    write("#L = expressionVisitor.expressionAttributeNames()", name)
                }

                toStruct.members(MemberCodegenBehavior.ExpressionArguments(ExpressionArgumentsType.AttributeValues)) {
                    write("#L = expressionVisitor.expressionAttributeValues()", name)
                }
            }
        }
    }
}

internal class RequestRenderer(ctx: RenderContext, request: KeyProjections) : IoRenderer(ctx, request) {
    override val fromProjections = request
    override val toProjections = KeyProjections.fromInterface(request.unkeyedProjection.interfaceStruct.lowLevel)

    override fun renderKeyConversion(fromStruct: Structure, toStruct: Structure) {
        toStruct.members(MemberCodegenBehavior.MapToKeys) {
            val keysAsArgs = fromMembers(fromStruct).joinToString(", ") { "this@convert.${it.name}" }
            write("#L = #T(schema, #L)", name, MapperTypes.Items.keysToItem, keysAsArgs)
        }
    }

    override fun renderSingleItemConversion() = writeInline("convertRight(it)")

    override fun Member.fromMember(fromStruct: Structure) = fromStruct.members.single { it.lowLevel == this }
    override fun Member.fromMembers(fromStruct: Structure) = fromStruct.members.filter { it.lowLevel == this }
}

internal class ResponseRenderer(ctx: RenderContext, response: KeyProjections) : IoRenderer(ctx, response) {
    override val fromProjections = KeyProjections.fromInterface(response.unkeyedProjection.interfaceStruct.lowLevel)
    override val toProjections = response

    override fun renderKeyConversion(fromStruct: Structure, toStruct: Structure) {
        toStruct.members(MemberCodegenBehavior.MapToKeys) {
            val conversionFn = when (this.keyType) {
                MemberKeyType.PARTITION -> MapperTypes.Items.itemToPk
                MemberKeyType.SORT -> MapperTypes.Items.itemToSk
                else -> error("Cannot find key type for ${toStruct.type.shortName} member $name")
            }
            val keysAsArgs = fromMembers(fromStruct).joinToString(", ") {
                format("this@convert.${it.name}?.#T()", MapperTypes.Model.toItem)
            }
            write("#L = #T(schema, #L)", name, conversionFn, keysAsArgs)
        }
    }

    override fun renderSingleItemConversion() = writeInline("convertLeft(it.#T())", MapperTypes.Model.toItem)

    override fun Member.fromMember(fromStruct: Structure) = lowLevel
    override fun Member.fromMembers(fromStruct: Structure) = listOf(lowLevel)
}
