/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.rendering

import aws.sdk.kotlin.hll.codegen.core.ImportDirective
import aws.sdk.kotlin.hll.codegen.model.Member
import aws.sdk.kotlin.hll.codegen.model.Operation
import aws.sdk.kotlin.hll.codegen.model.Structure
import aws.sdk.kotlin.hll.codegen.model.lowLevel
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.codegen.rendering.info
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.*

/**
 * Renders a dedicated file for a high-level operation, including request/response types, converters between low/high
 * types, and a factory method for creating instances of the DDB mapper runtime operation
 * @param ctx The active [RenderContext]
 * @param operation The [Operation] to codegen
 */
internal class OperationRenderer(
    private val ctx: RenderContext,
    private val operation: Operation,
) : RendererBase(ctx, operation.name) {
    private val requestMembers = operation
        .request
        .also { ctx.info("For type ${it.lowLevelName}:") }
        .lowLevel
        .members
        .groupBy { m -> m.codegenBehavior.also { ctx.info("  ${m.name} → $it") } }

    private val responseMembers = operation
        .response
        .also { ctx.info("For type ${it.lowLevelName}:") }
        .lowLevel
        .members
        .groupBy { m -> m.codegenBehavior.also { ctx.info("  ${m.name} → $it") } }

    companion object {
        fun factoryFunctionName(operation: Operation) = "${operation.methodName}Operation"
    }

    override fun generate() {
        renderRequest()
        blankLine()
        renderResponse()

        renderOperationFactory()
    }

    private fun renderOperationFactory() {
        val factoryName = factoryFunctionName(operation)

        operation.itemSourceKinds.filterNot { it.isAbstract }.forEach { itemSourceKind ->
            blankLine()
            withBlock(
                "internal fun <T> #L(spec: #T) = #T(",
                ")",
                factoryName,
                itemSourceKind.getSpecType("T"),
                MapperTypes.PipelineImpl.Operation,
            ) {
                write(
                    "initialize = { highLevelReq: #T -> #T(highLevelReq, spec.schema, #T(spec, #S)) },",
                    operation.request.type,
                    MapperTypes.PipelineImpl.HReqContextImpl,
                    MapperTypes.PipelineImpl.MapperContextImpl,
                    operation.name,
                )

                writeInline("serialize = { highLevelReq, schema -> highLevelReq.convert(")
                requestMembers(MemberCodegenBehavior.Hoist) {
                    if (name in itemSourceKind.hoistedFields) {
                        writeInline("spec.#L, ", name)
                    } else {
                        writeInline("#L = null, ", name)
                    }
                }
                write("schema) },")

                withBlock("lowLevelInvoke = { lowLevelReq ->", "},") {
                    withBlock("spec.mapper.client.#T { client ->", "}", MapperTypes.Internal.withWrappedClient) {
                        write("client.#L(lowLevelReq)", operation.methodName)
                    }
                }

                write("deserialize = #L::convert,", operation.response.lowLevelName)
                write("interceptors = spec.mapper.config.interceptors,")
            }
        }
    }

    private fun renderRequest() {
        DataTypeGenerator(ctx, this, operation.request).generate()
        blankLine()

        imports += ImportDirective(operation.request.lowLevel.type, operation.request.lowLevelName)

        openBlock("private fun <T> #T.convert(", operation.request.type)
        requestMembers(MemberCodegenBehavior.Hoist) { write("#L: #T, ", name, type) }
        write("schema: #T,", MapperTypes.Items.itemSchema("T"))
        closeAndOpenBlock(") = #L {", operation.request.lowLevelName)
        requestMembers(MemberCodegenBehavior.PassThrough) { write("#L = this@convert.#L", name, highLevel.name) }
        requestMembers(MemberCodegenBehavior.MapKeys) {
            write(
                "this@convert.#L?.let { #L = schema.converter.convertTo(it, schema.keyAttributeNames).#T(schema.keyAttributeNames) }",
                highLevel.name,
                name,
                MapperTypes.Model.intersectKeys,
            )
        }
        requestMembers(MemberCodegenBehavior.MapAll) {
            write("this@convert.#L?.let { #L = schema.converter.convertTo(it) }", highLevel.name, name)
        }
        requestMembers(MemberCodegenBehavior.ListMapAll) {
            write("#L = this@convert.#L?.map { schema.converter.convertTo(it) }", name, highLevel.name)
        }
        requestMembers(MemberCodegenBehavior.Hoist) { write("this.#1L = #1L", name) }

        if (requestMembers.hasExpressions) renderRequestExpressions()

        closeBlock("}")
    }

    private fun renderRequestExpressions() {
        blankLine()
        write("val expressionVisitor = #T()", MapperTypes.Expressions.Internal.ParameterizingExpressionVisitor)

        requestMembers(MemberCodegenBehavior.ExpressionLiteral(ExpressionLiteralType.Filter)) {
            write("#L = this@convert.#L?.accept(expressionVisitor)", name, highLevel.name)
        }

        requestMembers(MemberCodegenBehavior.ExpressionLiteral(ExpressionLiteralType.KeyCondition)) {
            write(
                "#L = this@convert.#L?.#T(schema)?.accept(expressionVisitor)",
                name,
                highLevel.name,
                MapperTypes.Expressions.Internal.toExpression,
            )
        }

        requestMembers(MemberCodegenBehavior.ExpressionArguments(ExpressionArgumentsType.AttributeNames)) {
            write("#L = expressionVisitor.expressionAttributeNames()", name)
        }

        requestMembers(MemberCodegenBehavior.ExpressionArguments(ExpressionArgumentsType.AttributeValues)) {
            write("#L = expressionVisitor.expressionAttributeValues()", name)
        }
    }

    private fun renderResponse() {
        DataTypeGenerator(ctx, this, operation.response).generate()
        blankLine()

        imports += ImportDirective(operation.response.lowLevel.type, operation.response.lowLevelName)

        withBlock(
            "private fun <T> #L.convert(schema: #T) = #T {",
            "}",
            operation.response.lowLevelName,
            MapperTypes.Items.itemSchema("T"),
            operation.response.type,
        ) {
            responseMembers(MemberCodegenBehavior.PassThrough) { write("#L = this@convert.#L", highLevel.name, name) }

            responseMembers(MemberCodegenBehavior.MapKeys, MemberCodegenBehavior.MapAll) {
                write(
                    "#L = this@convert.#L?.#T()?.let(schema.converter::convertFrom)",
                    highLevel.name,
                    name,
                    MapperTypes.Model.toItem,
                )
            }

            responseMembers(MemberCodegenBehavior.ListMapAll) {
                write(
                    "#L = this@convert.#L?.map { schema.converter.convertFrom(it.#T()) }",
                    highLevel.name,
                    name,
                    MapperTypes.Model.toItem,
                )
            }
        }
    }

    private val Member.highLevel: Member
        get() =
            operation.request.members.firstOrNull { it.lowLevel == this }
                ?: operation.response.members.first { it.lowLevel == this }
}

private val Map<MemberCodegenBehavior, List<Member>>.hasExpressions: Boolean
    get() = keys.any {
        it is MemberCodegenBehavior.ExpressionLiteral || it is MemberCodegenBehavior.ExpressionArguments
    }

private inline operator fun Map<MemberCodegenBehavior, List<Member>>.invoke(
    vararg behaviors: MemberCodegenBehavior,
    block: Member.() -> Unit,
) {
    behaviors.forEach { behavior ->
        get(behavior)?.forEach(block)
    }
}

private val Structure.lowLevelName: String
    get() = "LowLevel${type.shortName}"
