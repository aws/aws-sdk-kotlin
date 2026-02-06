/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.rendering

import aws.sdk.kotlin.hll.codegen.model.Operation
import aws.sdk.kotlin.hll.codegen.model.Structure
import aws.sdk.kotlin.hll.codegen.model.genericVars
import aws.sdk.kotlin.hll.codegen.model.lowLevel
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.*

/**
 * Renders a file for a high-level operation factory method, which creates instances of the DDB Mapper runtime
 * operation.
 * @param ctx The active [RenderContext]
 * @param operation The [Operation] to codegen
 */
internal class FactoryRenderer(
    private val ctx: RenderContext,
    private val operation: Operation,
) : RendererBase(ctx, operation.name) {
    companion object {
        fun factoryFunctionName(operation: Operation) = "${operation.methodName}Operation"
    }

    override fun generate() {
        val factoryName = factoryFunctionName(operation)

        operation.itemSourceKinds.filterNot { it.isAbstract }.forEach { itemSourceKind ->
            operation.keyTypes.forEach { keyType ->
                val request = operation.request.dataType.leafTypeOrDefault(keyType).interfaceStruct
                val response = operation.response.dataType.leafTypeOrDefault(keyType).interfaceStruct

                blankLine()
                val generics = (request.type.genericVars() + response.type.genericVars()).distinct()

                withBlock(
                    "internal fun #G#L(spec: #T) = #T(",
                    ")",
                    generics,
                    factoryName,
                    itemSourceKind.specType(keyType),
                    MapperTypes.PipelineImpl.Operation,
                ) {
                    write("interceptors = spec.mapper.config.interceptors,")

                    blankLine()
                    write(
                        "initialize = { highLevelReq: #T -> #T(highLevelReq, spec.schema, #T(spec, #S)) },",
                        request.type,
                        MapperTypes.PipelineImpl.HReqContextImpl,
                        MapperTypes.PipelineImpl.MapperContextImpl,
                        operation.name,
                    )

                    blankLine()
                    withBlock("serialize = { highLevelReq, schema ->", "},") {
                        withBlock("highLevelReq.convert(", ")") {
                            renderHoistedFields(request, itemSourceKind)
                            write("schema,")
                        }
                    }

                    blankLine()
                    withBlock("lowLevelInvoke = { lowLevelReq ->", "},") {
                        withBlock("spec.mapper.client.#T { client ->", "}", MapperTypes.Internal.withWrappedClient) {
                            write("client.#L(lowLevelReq)", operation.methodName)
                        }
                    }

                    blankLine()
                    withBlock("deserialize = { lowLevelRes, schema ->", "},") {
                        withBlock("lowLevelRes.convert(", ")") {
                            renderHoistedFields(response, itemSourceKind)
                            write("schema,")
                        }
                    }
                }
            }
        }
    }

    private fun renderHoistedFields(structure: Structure, itemSourceKind: ItemSourceKind) {
        structure
            .lowLevel
            .members
            .filter { it.codegenBehavior == MemberCodegenBehavior.Hoist }
            .forEach { member ->
                if (member.name in itemSourceKind.hoistedFields) {
                    write("spec.#L,", member.name)
                } else {
                    write("#L = null,", member.name)
                }
            }
    }
}
