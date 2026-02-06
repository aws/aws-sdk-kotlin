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
    private val dataType: DataType,
) : RendererBase(ctx, dataType.interfaceStruct.type.shortName) {
    protected abstract val fromType: DataType
    protected abstract val toType: DataType
    protected abstract fun Member.fromMember(fromStruct: Structure): Member
    protected abstract fun Member.fromMembers(fromStruct: Structure): List<Member>

    protected abstract fun renderKeyConversion(fromStruct: Structure, toStruct: Structure)
    protected abstract fun renderSingleItemConversion()

    final override fun generate() {
        DataTypeGenerator(ctx, this, dataType).generate()

        // Manually import the low-level type with a specific alias
        val llType = dataType.interfaceStruct.lowLevel.type
        imports += ImportDirective(llType, "LowLevel${llType.shortName}")

        val isKeyed = fromType.interfaceStruct.isKeyed() || toType.interfaceStruct.isKeyed()
        val keyTypes = when {
            isKeyed -> listOf(StructureKeyType.PARTITION_KEY, StructureKeyType.COMPOSITE_KEY)
            else -> listOf(StructureKeyType.NONE)
        }

        keyTypes.forEach { keyType ->
            val fromVariant = fromType.leafTypeOrDefault(keyType)
            val toVariant = toType.leafTypeOrDefault(keyType)
            renderConversion(keyType, fromVariant.interfaceStruct, toVariant.interfaceStruct)
        }
    }

    private fun renderConversion(keyType: StructureKeyType, fromStruct: Structure, toStruct: Structure) {
        val schemaType = when (keyType) {
            StructureKeyType.NONE -> MapperTypes.Items.ItemSchema
            StructureKeyType.PARTITION_KEY -> MapperTypes.Items.ItemSchemaPartitionKey
            StructureKeyType.COMPOSITE_KEY -> MapperTypes.Items.ItemSchemaCompositeKey
        }

        val generics = (listOf(TypeVar.T) + fromStruct.type.genericVars() + toStruct.type.genericVars()).distinct()

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

internal class RequestRenderer(ctx: RenderContext, request: DataType) : IoRenderer(ctx, request) {
    override val fromType = request
    override val toType = DataType.fromInterface(request.interfaceStruct.lowLevel)

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

internal class ResponseRenderer(ctx: RenderContext, response: DataType) : IoRenderer(ctx, response) {
    override val fromType = DataType.fromInterface(response.interfaceStruct.lowLevel)
    override val toType = response

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
