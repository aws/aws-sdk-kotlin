/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.rendering

import aws.sdk.kotlin.hll.codegen.model.Operation
import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.genericVars
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.*

/**
 * Renders an `*Operations` interface and `*OperationsImpl` class which contain a method for each code-generated
 * operation which dispatches to the factory function rendered in [FactoryRenderer]
 * @param ctx The active [RenderContext]
 * @param itemSourceKind The type of `ItemSource` for which to render operations
 * @param parentType The [Type] of the direct parent interface of the to-be-generated `*Operations` interface (e.g., if
 * [itemSourceKind] is [ItemSourceKind.Table], then [parentType] should be the generated `ItemSourceOperations`
 * interface)
 * @param operations A list of the operations in scope for codegen
 */
internal class OperationsTypeRenderer(
    private val ctx: RenderContext,
    private val itemSourceKind: ItemSourceKind,
    private val parentType: Type?,
    private val operations: List<Operation>,
) : RendererBase(ctx, "${itemSourceKind.name}Operations") {
    private val keyedOperations: List<Operation>
    private val unkeyedOperations: List<Operation>

    init {
        val (keyedOperations, unkeyedOperations) = operations.partition { it.isKeyed() }
        this.keyedOperations = keyedOperations
        this.unkeyedOperations = unkeyedOperations
    }

    val unkeyedType = itemSourceKind.opsType(StructureKeyType.NONE)
    val pkType = itemSourceKind.opsType(StructureKeyType.PARTITION_KEY)
    val ckType = itemSourceKind.opsType(StructureKeyType.COMPOSITE_KEY)

    override fun generate() {
        renderInterface()
        renderDslOps()
        renderResponsePaginators()

        if (itemSourceKind.isAbstract) {
            blankLine()
            renderItemsPaginators()
        } else {
            blankLine()
            renderImpl()
        }
    }

    private fun renderDslOps() {
        unkeyedOperations
            .filterNot { it.appliesToAncestorKind() }
            .forEach { renderDslOp(it, StructureKeyType.NONE) }

        keyedOperations
            .filterNot { it.appliesToAncestorKind() }
            .forEach {
                renderDslOp(it, StructureKeyType.PARTITION_KEY)
                renderDslOp(it, StructureKeyType.COMPOSITE_KEY)
            }
    }

    private fun renderDslOp(op: Operation, keyType: StructureKeyType) {
        val requestDataType = op.request.dataType.leafTypeOrDefault(keyType)
        val responseDataType = op.response.dataType.leafTypeOrDefault(keyType)

        val request = requestDataType.interfaceStruct.type
        val response = responseDataType.interfaceStruct.type

        val generics = (request.genericVars() + response.genericVars()).distinct()

        val paginationInfo = PaginationInfo.forRequestResponse(requestDataType, responseDataType)
        if (paginationInfo != null) renderManualPaginationAnnotation(op) else blankLine()
        withBlock(
            "public suspend inline fun #G#T.#L(crossinline block: #T.() -> Unit): #T =",
            "",
            generics,
            keyType.interfaceType,
            op.methodName,
            requestDataType.builderStruct.type,
            response,
        ) {
            write("#L(#T().apply(block).build())", op.methodName, requestDataType.builderStruct.type)
        }
    }

    private fun renderImpl() {
        val keyTypes = when {
            keyedOperations.isEmpty() -> listOf(StructureKeyType.NONE)
            else -> listOf(StructureKeyType.PARTITION_KEY, StructureKeyType.COMPOSITE_KEY)
        }

        keyTypes.forEach { keyType ->
            val interfaceType = keyType.interfaceType
            val implName = "${interfaceType.shortName.replace(".", "")}Impl"
            val implType = interfaceType.copy(shortName = implName)

            blankLine()
            withBlock(
                "internal class #D(private val spec: #T) : #T {",
                "}",
                implType,
                itemSourceKind.specType(keyType),
                interfaceType,
            ) {
                operations.forEach { op ->
                    val requestDataType = op.request.dataType.leafTypeOrDefault(keyType)
                    val responseDataType = op.response.dataType.leafTypeOrDefault(keyType)

                    val paginationInfo = PaginationInfo.forRequestResponse(requestDataType, responseDataType)
                    if (paginationInfo != null) renderManualPaginationAnnotation(op)

                    write(
                        "override suspend fun #L(request: #T) = #L(spec).execute(request)",
                        op.methodName,
                        requestDataType.interfaceStruct.type,
                        FactoryRenderer.factoryFunctionName(op),
                    )

                    if (paginationInfo != null) blankLine()
                }
            }
        }
    }

    private fun renderInterface() {
        withDocs {
            write("Provides access to operations on a particular [#L], which will invoke", itemSourceKind.name)
            write("low-level operations after mapping objects to items and vice versa")
        }
        writeInline("public interface #T ", unkeyedType)
        parentType?.let { writeInline(": #T ", parentType) }
        withBlock("{", "}") {
            unkeyedOperations.forEach { renderOp(it, StructureKeyType.NONE) }

            if (keyedOperations.isNotEmpty()) {
                docs("Provides access to operations a particular [#L.PartitionKey]", itemSourceKind.name, unkeyedType)
                writeInline("public interface PartitionKey#G : #T", pkType.genericVars(), unkeyedType)

                itemSourceKind.parent?.let { parentType ->
                    writeInline(", #T", parentType.opsType(StructureKeyType.PARTITION_KEY))
                }

                withBlock(" {", "}") {
                    keyedOperations.forEach { renderOp(it, StructureKeyType.PARTITION_KEY) }
                }

                docs("Provides access to operations on a particular [#L.CompositeKey]", itemSourceKind.name, unkeyedType)
                writeInline("public interface CompositeKey#G : #T", ckType.genericVars(), unkeyedType)

                itemSourceKind.parent?.let { parentType ->
                    writeInline(", #T", parentType.opsType(StructureKeyType.COMPOSITE_KEY))
                }

                withBlock(" {", "}") {
                    keyedOperations.forEach { renderOp(it, StructureKeyType.COMPOSITE_KEY) }
                }
            }
        }
    }

    private fun renderManualPaginationAnnotation(op: Operation) {
        blankLine()
        write(
            "@#T(paginatedEquivalent = #S)",
            MapperTypes.Annotations.ManualPagination,
            PaginatorRenderer.paginatorName(op),
        )
    }

    private fun renderOp(op: Operation, keyType: StructureKeyType) {
        if (op.appliesToAncestorKind()) return

        val requestDataType = op.request.dataType.leafTypeOrDefault(keyType)
        val responseDataType = op.response.dataType.leafTypeOrDefault(keyType)

        val paginationInfo = PaginationInfo.forRequestResponse(requestDataType, responseDataType)
        if (paginationInfo != null) renderManualPaginationAnnotation(op)

        write(
            "public suspend fun #L(request: #T): #T",
            op.methodName,
            requestDataType.interfaceStruct.type,
            responseDataType.interfaceStruct.type,
        )

        if (paginationInfo != null) blankLine()
    }

    private fun renderItemsPaginators() = operations
        .forEach { op ->
            val requestDataType = op.request.dataType // abstract/unkeyed type
            val responseDataType = op.response.dataType // abstract/unkeyed type

            val paginationInfo = PaginationInfo.forRequestResponse(requestDataType, responseDataType)
            paginationInfo?.let {
                PaginatorRenderer(ctx, this, requestDataType.interfaceStruct.type, op, it, forItems = true).render()
            }
        }

    private fun renderResponsePaginators() = operations
        .forEach { op ->
            op.keyTypes.forEach { variant ->
                val requestDataType = op.request.dataType.leafTypeOrDefault(variant)
                val responseDataType = op.response.dataType.leafTypeOrDefault(variant)

                val paginationInfo = PaginationInfo.forRequestResponse(requestDataType, responseDataType)
                paginationInfo?.let {
                    PaginatorRenderer(ctx, this, variant.interfaceType, op, it, forResponses = true).render()
                }
            }
        }
    private fun Operation.appliesToAncestorKind() = itemSourceKind.parent?.let { appliesToKindOrAncestor(it) } ?: false

    private val StructureKeyType.interfaceType: TypeRef
        get() = when (this) {
            StructureKeyType.NONE -> unkeyedType
            StructureKeyType.PARTITION_KEY -> pkType
            StructureKeyType.COMPOSITE_KEY -> ckType
        }
}

private fun Operation.appliesToKindOrAncestor(kind: ItemSourceKind): Boolean =
    kind in itemSourceKinds || (kind.parent?.let { appliesToKindOrAncestor(it) } ?: false)
