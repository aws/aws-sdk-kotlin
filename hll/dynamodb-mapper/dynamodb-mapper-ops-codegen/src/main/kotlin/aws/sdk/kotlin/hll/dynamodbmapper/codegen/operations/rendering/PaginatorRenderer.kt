/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.rendering

import aws.sdk.kotlin.hll.codegen.core.CodeGenerator
import aws.sdk.kotlin.hll.codegen.model.Operation
import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeVar
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.hll.codegen.rendering.BuilderRenderer
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.util.capitalizeFirstChar
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.paginationInfo

/**
 * Renders paginator methods for an operation. There are two types of paginators handled by this renderer:
 * * response paginators (i.e., each response is an element in a `Flow`)
 * * item paginators (i.e., each item from each response is an element in a `Flow`)
 *
 * Rendering these paginators is enabled by setting [forResponses] or [forItems], respectively.
 *
 * ## Response paginators
 *
 * When [forResponses] is true, this paginator will render code such as the following:
 *
 * ```kotlin
 * public fun <T> ItemSourceOperations<T>.queryPaginated(initialRequest: QueryRequest<T>): Flow<QueryResponse<T>> = flow {
 *     var cursor = initialRequest.exclusiveStartKey
 *     var hasNextPage = true
 *
 *     while (hasNextPage) {
 *         val req = initialRequest.copy { exclusiveStartKey = cursor }
 *
 *         @OptIn(ManualPagination::class)
 *         val res = this@queryPaginated.query(req)
 *
 *         cursor = res.lastEvaluatedKey
 *         hasNextPage = cursor != null
 *         emit(res)
 *     }
 * }
 *
 * public inline fun <T> ItemSourceOperations<T>.queryPaginated(crossinline block: QueryRequestBuilder<T>.() -> Unit): Flow<QueryResponse<T>> =
 *     queryPaginated(QueryRequestBuilder<T>().apply(block).build())
 * ```
 *
 * ## Item paginators
 *
 * When [forItems] is true, this paginator will render code such as the following:
 *
 * ```kotlin
 * @JvmName("queryItems")
 * public fun <T> Flow<QueryResponse<T>>.items(): Flow<T> =
 *     transform { page ->
 *         page.items?.forEach { item ->
 *             emit(item)
 *         }
 *     }
 * ```
 *
 * @param ctx The context for this renderer
 * @param generator The underlying code generator to use when rendering
 * @param op The operation for which paginators will be rendered
 * @param extensionOf The type from which paginators will be extension methods. This type is optional—if `null` is
 * passed then the rendered paginators will be simple functions instead of extension methods. This only applies to
 * **response** paginators.
 * @param forResponses Enables generating response paginators
 * @param forItems Enables generating item paginators
 */
internal class PaginatorRenderer(
    private val ctx: RenderContext,
    private val generator: CodeGenerator,
    private val op: Operation,
    private val extensionOf: Type?,
    private val forResponses: Boolean,
    private val forItems: Boolean,
) : CodeGenerator by generator {
    init {
        require(forResponses || forItems) { "One of `forResponses` or `forItems` must be set to true" }
    }

    internal companion object {
        fun paginatorName(op: Operation) = "${op.methodName}Paginated"
    }

    private val paginationInfo = requireNotNull(op.paginationInfo) { "Operation ${op.name} is not paginatable" }
    private val name = paginatorName(op)

    private val requestType = op.request.type
    private val requestBuilderType = BuilderRenderer.builderType(requestType)
    private val responseType = op.response.type

    private val itemFlowType = Types.Kotlinx.Coroutines.Flow.flow(TypeVar("T"))
    private val pageFlowType = Types.Kotlinx.Coroutines.Flow.flow(responseType)

    fun render() {
        if (forResponses) {
            blankLine()
            renderPaginatorWithRequest()
            blankLine()
            renderPaginatorWithDsl()
        }

        if (forItems) {
            blankLine()
            renderItemsPaginator()
        }
    }

    private fun renderItemsPaginator() {
        val jvmName = "${op.methodName}${paginationInfo.items.name.capitalizeFirstChar}"
        write("@#T(#S)", Types.Kotlin.Jvm.JvmName, jvmName)
        withBlock("public fun <T> #T.items(): #T =", "", pageFlowType, itemFlowType) {
            withBlock("#T { page ->", "}", Types.Kotlinx.Coroutines.Flow.transform) {
                withBlock("page.#L?.forEach { item ->", "}", paginationInfo.items.name) {
                    write("emit(item)")
                }
            }
        }
    }

    private fun renderPaginatorWithDsl() {
        writeInline("public inline fun <T> ")

        extensionOf?.let { writeInline("#T.", extensionOf) }

        withBlock(
            "#L(crossinline block: #T.() -> Unit): #T =",
            "",
            name,
            requestBuilderType,
            pageFlowType,
        ) {
            write("#L(#T().apply(block).build())", name, requestBuilderType)
        }
    }

    private fun renderPaginatorWithRequest() {
        writeInline("public fun <T> ")

        extensionOf?.let { writeInline("#T.", extensionOf) }

        withBlock(
            "#L(initialRequest: #T): #T = #T {",
            "}",
            name,
            requestType,
            pageFlowType,
            Types.Kotlinx.Coroutines.Flow.flow,
        ) {
            write("var cursor = initialRequest.#L", paginationInfo.inputToken.name)
            write("var hasNextPage = true")
            blankLine()
            withBlock("while (hasNextPage) {", "}") {
                write("val req = initialRequest.copy { #L = cursor }", paginationInfo.inputToken.name)

                blankLine()
                write("@#T(#T::class)", Types.Kotlin.OptIn, MapperTypes.Annotations.ManualPagination)
                write("val res = this@#L.#L(req)", name, op.methodName)

                blankLine()
                write("cursor = res.#L", paginationInfo.outputToken.name)
                write("hasNextPage = cursor != null")
                write("emit(res)")
            }
        }
    }
}
