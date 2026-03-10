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
 * Renders an `*Operations` interface and `*OperationsImpl` class which contain methods for each code-generated
 * operation projection, which dispatches to the factory function rendered in [FactoryRenderer]
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
        val (keyedOperations, unkeyedOperations) = operations.partition { it.isKeyed }
        this.keyedOperations = keyedOperations
        this.unkeyedOperations = unkeyedOperations
    }

    val unkeyedType = itemSourceKind.opsType(KeyProjectionType.NONE)
    private val pkType = itemSourceKind.opsType(KeyProjectionType.PARTITION_KEY)
    private val ckType = itemSourceKind.opsType(KeyProjectionType.COMPOSITE_KEY)

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
            .forEach { renderDslOp(it, KeyProjectionType.NONE) }

        keyedOperations
            .filterNot { it.appliesToAncestorKind() }
            .forEach {
                renderDslOp(it, KeyProjectionType.PARTITION_KEY)
                renderDslOp(it, KeyProjectionType.COMPOSITE_KEY)
            }
    }

    private fun renderDslOp(op: Operation, keyType: KeyProjectionType) {
        val requestProjection = op.request.keyProjections[keyType]
        val responseProjection = op.response.keyProjections[keyType]

        val request = requestProjection.interfaceStruct.type
        val response = responseProjection.interfaceStruct.type
        val generics = request.genericVars() + response.genericVars()

        val paginationInfo = PaginationInfo.forRequestResponse(requestProjection, responseProjection)
        if (paginationInfo != null) renderManualPaginationAnnotation(op) else blankLine()
        withBlock(
            "public suspend inline fun #G#T.#L(crossinline block: #T.() -> Unit): #T =",
            "",
            generics,
            keyType.interfaceType,
            op.methodName,
            requestProjection.builderStruct.type,
            response,
        ) {
            write("#L(#T().apply(block).build())", op.methodName, requestProjection.builderStruct.type)
        }
    }

    private fun renderImpl() {
        val keyTypes = when {
            keyedOperations.isEmpty() -> listOf(KeyProjectionType.NONE)
            else -> listOf(KeyProjectionType.PARTITION_KEY, KeyProjectionType.COMPOSITE_KEY)
        }

        keyTypes.forEach { keyType ->
            val interfaceType = keyType.interfaceType
            val implName = "${interfaceType.shortName.replace(".", "")}Impl"
            val implType = interfaceType.copy(shortName = implName)
            val specType = itemSourceKind.specType(keyType)

            val (abstractModifier, argsList) = when (itemSourceKind.isSchemaless) {
                true -> "abstract " to ""
                else -> "" to format("private val spec: #T", specType)
            }

            blankLine()
            withBlock(
                "internal #Lclass #D(#L) : #T {",
                "}",
                abstractModifier,
                implType,
                argsList,
                interfaceType,
            ) {
                if (itemSourceKind.isSchemaless) {
                    write("abstract val spec: #T", specType) // Spec must be provided by subtype on demand
                    blankLine()
                }

                operations.forEach { op ->
                    val requestProjection = op.request.keyProjections[keyType]
                    val responseProjection = op.response.keyProjections[keyType]

                    val paginationInfo = PaginationInfo.forRequestResponse(requestProjection, responseProjection)
                    if (paginationInfo != null) renderManualPaginationAnnotation(op)

                    write(
                        "override suspend fun #L(request: #T) = #L(spec).execute(request)",
                        op.methodName,
                        requestProjection.interfaceStruct.type,
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
            unkeyedOperations.forEach { renderOp(it, KeyProjectionType.NONE) }

            if (keyedOperations.isNotEmpty()) {
                docs("Provides access to operations a particular [#L.PartitionKey]", itemSourceKind.name, unkeyedType)
                writeInline("public interface PartitionKey#G : #T", pkType.genericVars(), unkeyedType)

                itemSourceKind.parent?.let { parentType ->
                    writeInline(", #T", parentType.opsType(KeyProjectionType.PARTITION_KEY))
                }

                withBlock(" {", "}") {
                    keyedOperations.forEach { renderOp(it, KeyProjectionType.PARTITION_KEY) }
                }

                docs("Provides access to operations on a particular [#L.CompositeKey]", itemSourceKind.name, unkeyedType)
                writeInline("public interface CompositeKey#G : #T", ckType.genericVars(), unkeyedType)

                itemSourceKind.parent?.let { parentType ->
                    writeInline(", #T", parentType.opsType(KeyProjectionType.COMPOSITE_KEY))
                }

                withBlock(" {", "}") {
                    keyedOperations.forEach { renderOp(it, KeyProjectionType.COMPOSITE_KEY) }
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

    private fun renderOp(op: Operation, keyType: KeyProjectionType) {
        if (op.appliesToAncestorKind()) return

        val requestProjection = op.request.keyProjections[keyType]
        val responseProjection = op.response.keyProjections[keyType]

        val paginationInfo = PaginationInfo.forRequestResponse(requestProjection, responseProjection)
        if (paginationInfo != null) renderManualPaginationAnnotation(op)

        write(
            "public suspend fun #L(request: #T): #T",
            op.methodName,
            requestProjection.interfaceStruct.type,
            responseProjection.interfaceStruct.type,
        )

        if (paginationInfo != null) blankLine()
    }

    private fun renderItemsPaginators() = operations
        .forEach { op ->
            val requestProjection = op.request.keyProjections.unkeyedProjection
            val responseProjection = op.response.keyProjections.unkeyedProjection

            val paginationInfo = PaginationInfo.forRequestResponse(requestProjection, responseProjection)
            paginationInfo?.let {
                PaginatorRenderer(ctx, this, requestProjection.interfaceStruct.type, op, it, forItems = true).render()
            }
        }

    private fun renderResponsePaginators() = operations
        .forEach { op ->
            op.keyTypes.forEach { keyType ->
                val requestProjection = op.request.keyProjections[keyType]
                val responseProjection = op.response.keyProjections[keyType]

                val paginationInfo = PaginationInfo.forRequestResponse(requestProjection, responseProjection)
                paginationInfo?.let {
                    PaginatorRenderer(ctx, this, keyType.interfaceType, op, it, forResponses = true).render()
                }
            }
        }
    private fun Operation.appliesToAncestorKind() = itemSourceKind.parent?.let { appliesToKindOrAncestor(it) } ?: false

    private val KeyProjectionType.interfaceType: TypeRef
        get() = when (this) {
            KeyProjectionType.NONE -> unkeyedType
            KeyProjectionType.PARTITION_KEY -> pkType
            KeyProjectionType.COMPOSITE_KEY -> ckType
        }
}

private fun Operation.appliesToKindOrAncestor(kind: ItemSourceKind): Boolean = kind in itemSourceKinds || (kind.parent?.let { appliesToKindOrAncestor(it) } ?: false)
