/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.rendering

import aws.sdk.kotlin.hll.codegen.model.*
import aws.sdk.kotlin.hll.codegen.rendering.BuilderRenderer
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.codegen.util.lowercaseFirstChar
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.ItemSourceKind
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.itemSourceKinds
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.paginationInfo

/**
 * Renders the `*Operations` interface and `*OperationsImpl` class which contain a method for each codegenned
 * operation and dispatches to the factory function rendered in [OperationRenderer]
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
    private val entityName = itemSourceKind.name.lowercaseFirstChar
    private val intfName = "${itemSourceKind.name}Operations"

    val interfaceType = TypeRef(ctx.pkg, intfName, listOf(TypeVar("T")))

    override fun generate() {
        renderInterface()
        renderDslOps()
        renderPaginators(forResponses = true)

        if (itemSourceKind.isAbstract) {
            blankLine()
            renderPaginators(forItems = true)
        } else {
            blankLine()
            renderImpl()
        }
    }

    private fun renderDslOps() = operations
        .filterNot { it.appliesToAncestorKind() }
        .forEach(::renderDslOp)

    private fun renderDslOp(op: Operation) {
        val builderType = BuilderRenderer.builderType(op.request.type)
        val generics = op.request.genericVars().asParamsList(" ")

        if (op.paginationInfo != null) renderManualPaginationAnnotation(op) else blankLine()

        withBlock(
            "public suspend inline fun #L#T.#L(crossinline block: #T.() -> Unit): #T =",
            "",
            generics,
            interfaceType,
            op.methodName,
            builderType,
            op.response.type,
        ) {
            write("#L(#T().apply(block).build())", op.methodName, builderType)
        }

        blankLine()
    }

    private fun renderImpl() {
        val implName = "${itemSourceKind.name}OperationsImpl"

        withBlock(
            "internal class #L<T>(private val spec: #T) : #T {",
            "}",
            implName,
            itemSourceKind.getSpecType("T"),
            interfaceType,
        ) {
            operations.forEach { op ->
                if (op.paginationInfo != null) renderManualPaginationAnnotation(op)

                write(
                    "override suspend fun #L(request: #T) = #L(spec).execute(request)",
                    op.methodName,
                    op.request.type,
                    OperationRenderer.factoryFunctionName(op),
                )

                if (op.paginationInfo != null) blankLine()
            }
        }
    }

    private fun renderInterface() {
        withDocs {
            write("Provides access to operations on a particular #L, which will invoke low-level", entityName)
            write("operations after mapping objects to items and vice versa")
            write("@param T The type of objects which will be read from and/or written to this #L", entityName)
        }
        writeInline("public interface #T ", interfaceType)

        parentType?.let { writeInline(": #T ", parentType) }

        withBlock("{", "}") {
            operations.forEach(::renderOp)
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

    private fun renderOp(op: Operation) {
        val overrideModifier = if (op.appliesToAncestorKind()) " override" else ""

        if (op.paginationInfo != null) renderManualPaginationAnnotation(op)

        write(
            "public#L suspend fun #L(request: #T): #T",
            overrideModifier,
            op.methodName,
            op.request.type,
            op.response.type,
        )

        if (op.paginationInfo != null) blankLine()
    }

    private fun renderPaginators(forResponses: Boolean = false, forItems: Boolean = false) = operations
        .filterNot { it.paginationInfo == null }
        .forEach { op -> PaginatorRenderer(ctx, this, op, interfaceType, forResponses, forItems).render() }

    private fun Operation.appliesToAncestorKind() = itemSourceKind.parent?.let { appliesToKindOrAncestor(it) } ?: false
}

private fun Operation.appliesToKindOrAncestor(kind: ItemSourceKind): Boolean = kind in itemSourceKinds || (kind.parent?.let { appliesToKindOrAncestor(it) } ?: false)
