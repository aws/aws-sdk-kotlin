/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.rendering

import aws.sdk.kotlin.hll.codegen.model.*
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.*
import aws.smithy.kotlin.runtime.util.length

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
    private companion object {
        // FIXME Support unsigned number types?
        val keyDataTypes = listOf(
            Types.Kotlin.Byte,
            Types.Kotlin.ByteArray,
            Types.Kotlin.Int,
            Types.Kotlin.Long,
            Types.Kotlin.Short,
            Types.Kotlin.String,
        )

        enum class PaginationType {
            UNPAGINATED,
            SINGLE_PAGE,
            PAGE_FLOW,
        }
    }

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
        renderKeyExtensions()
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
        val request = requestProjection.interfaceStruct.type
        val response = op.response.keyProjections[keyType].interfaceStruct.type
        val generics = request.genericVars() + response.genericVars()

        if (op.isPaginated) renderManualPaginationAnnotation(op) else blankLine()

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
                    val request = op.request.keyProjections[keyType].interfaceStruct.type

                    val isPaginated = op.isPaginated
                    if (isPaginated) renderManualPaginationAnnotation(op)

                    write(
                        "override suspend fun #L(request: #T) = #L(spec).execute(request)",
                        op.methodName,
                        request,
                        FactoryRenderer.factoryFunctionName(op),
                    )

                    if (isPaginated) blankLine()
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

    private fun renderKeyExtensions() {
        keyedOperations
            .filterNot { it.appliesToAncestorKind() }
            .forEach { op ->
                val paginationType = if (op.isPaginated) PaginationType.SINGLE_PAGE else PaginationType.UNPAGINATED
                renderOpKeyExtensions(op, KeyProjectionType.PARTITION_KEY, paginationType)
                renderOpKeyExtensions(op, KeyProjectionType.COMPOSITE_KEY, paginationType)
            }
    }

    private fun renderOpKeyExtensions(
        op: Operation,
        keyType: KeyProjectionType,
        pagination: PaginationType,
    ) {
        val requestProjection = op.request.keyProjections[keyType]
        val genericReceiver = itemSourceKind.opsType(keyType)
        val genericResponse = op.response.keyProjections[keyType].interfaceStruct.type

        val methodName = when (pagination) {
            PaginationType.PAGE_FLOW -> PaginatorRenderer.paginatorName(op)
            else -> op.methodName
        }

        val keyedMembers = requestProjection.interfaceStruct.members.filter { it.keyType != null }
        val keyPermutations = keyDataTypes.permutations(keyedMembers.size) { permutation ->
            keyedMembers.zip(permutation) { member, dataType ->
                member.copy(type = dataType)
            }
        }

        keyPermutations.forEach { keyPermutation ->
            fun TypeRef.replaceGenericArgs(): TypeRef = copy(
                genericArgs = genericArgs.map { typeArg ->
                    when (typeArg.shortName) {
                        "PK" -> MapperTypes.Items.keyType(listOf(keyPermutation[0].type))
                        "SK" -> MapperTypes.Items.keyType(listOf(keyPermutation[1].type))
                        else -> typeArg
                    }
                },
            )

            val receiver = genericReceiver.replaceGenericArgs()

            val response = when (pagination) {
                PaginationType.PAGE_FLOW -> Types.Kotlinx.Coroutines.Flow.flow(genericResponse.replaceGenericArgs())
                else -> genericResponse.replaceGenericArgs()
            }

            val modifier = if (pagination == PaginationType.PAGE_FLOW) "" else "suspend "

            if (pagination == PaginationType.SINGLE_PAGE) renderManualPaginationAnnotation(op)
            withBlock(
                "public #L fun <T> #T.#L(",
                "}",
                modifier,
                receiver,
                methodName,
            ) {
                keyPermutation.forEach { member ->
                    write("#L: #T,", member.name, member.type)
                }

                closeAndOpenBlock("): #T = #L {", response, methodName)

                keyPermutation.forEach { member ->
                    write("this.#L = #T(#L)", member.name, MapperTypes.Items.Key, member.name)
                }
            }

            blankLine()
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

        val request = op.request.keyProjections[keyType].interfaceStruct.type
        val response = op.response.keyProjections[keyType].interfaceStruct.type

        val isPaginated = op.isPaginated
        if (isPaginated) renderManualPaginationAnnotation(op)

        write(
            "public suspend fun #L(request: #T): #T",
            op.methodName,
            request,
            response,
        )

        if (isPaginated) blankLine()
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
        .filterNot { it.appliesToAncestorKind() }
        .forEach { op ->
            op.keyTypes.forEach { keyType ->
                val requestProjection = op.request.keyProjections[keyType]
                val responseProjection = op.response.keyProjections[keyType]

                val paginationInfo = PaginationInfo.forRequestResponse(requestProjection, responseProjection)
                    ?: return@forEach

                PaginatorRenderer(ctx, this, keyType.interfaceType, op, paginationInfo, forResponses = true).render()

                renderOpKeyExtensions(op, keyType, PaginationType.PAGE_FLOW)
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

private fun Operation.appliesToKindOrAncestor(
    kind: ItemSourceKind,
): Boolean = kind in itemSourceKinds || (kind.parent?.let { appliesToKindOrAncestor(it) } ?: false)

private fun <T, R> List<T>.permutations(length: Int, transform: (List<T>) -> List<R>): List<List<R>> = buildList {
    val values = this@permutations

    fun generate(current: List<T>) {
        if (current.length == length) {
            add(transform(current))
        } else {
            values.forEach { value ->
                generate(current + value)
            }
        }
    }

    generate(listOf())
}
