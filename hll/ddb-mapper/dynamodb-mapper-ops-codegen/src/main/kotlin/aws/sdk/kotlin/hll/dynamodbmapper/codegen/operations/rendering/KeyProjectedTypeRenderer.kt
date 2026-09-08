/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.rendering

import aws.sdk.kotlin.hll.codegen.core.CodeGenerator
import aws.sdk.kotlin.hll.codegen.model.GenericsSet
import aws.sdk.kotlin.hll.codegen.model.genericVars
import aws.sdk.kotlin.hll.codegen.rendering.BuilderRenderer
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RenderOptions
import aws.sdk.kotlin.hll.codegen.rendering.Visibility
import aws.sdk.kotlin.hll.codegen.util.plus
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.*

/**
 * Renders a type's key projections into code. Specifically, this includes for each projection:
 * * A `public interface` type
 * * A `private data class` implementation of the interface
 * * A builder (delegated to [BuilderRenderer])
 * * A `toBuilder` extension method on the interface
 * * A `copy` extension method on the interface
 * * A DSL factory method for instantiating new instances of the interface
 *
 * ## Example output for unkeyed projection
 *
 * The following code is an example of output for an unkeyed type:
 *
 * ```kotlin
 * interface DeleteItemResponse<T> {
 *     companion object { }
 *
 *     val attributes: T?
 *     val consumedCapacity: ConsumedCapacity?
 *     val itemCollectionMetrics: ItemCollectionMetrics?
 * }
 *
 * data class DeleteItemResponseImpl<T>(
 *     val attributes: T?,
 *     val consumedCapacity: ConsumedCapacity?,
 *     val itemCollectionMetrics: ItemCollectionMetrics?,
 * ): DeleteItemResponse<T>
 *
 * // Builder is rendered by `BuilderRenderer` and thus omitted here
 *
 * fun <T> DeleteItemResponse<T>.toBuilder(): DeleteItemResponseBuilder<T> = DeleteItemResponseBuilder<T>().apply {
 *     attributes = this@toBuilder.attributes
 *     consumedCapacity = this@toBuilder.consumedCapacity
 *     itemCollectionMetrics = this@toBuilder.itemCollectionMetrics
 * }
 *
 * fun <T> DeleteItemResponse<T>.copy(block: DeleteItemResponseBuilder<T>.() -> Unit): DeleteItemResponse<T> =
 *     toBuilder().apply(block).build()
 *
 * fun <T> DeleteItemResponse(block: DeleteItemResponseBuilder<T>.() -> Unit): DeleteItemResponse<T> =
 *     DeleteItemResponseBuilder<T>().apply(block).build()
 * ```
 *
 * ## Example output for keyed projection
 *
 * The following code is an example of output for a keyed type:
 *
 * ```kotlin
 * sealed interface DeleteItemRequest {
 *     companion object { }
 *
 *     val returnConsumedCapacity: ReturnConsumedCapacity?
 *     val returnItemCollectionMetrics: ReturnItemCollectionMetrics?
 *     val returnValues: ReturnValue?
 *     val returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?
 *
 *     interface PartitionKey<PK : KeyType> : DeleteItemRequest {
 *         companion object { }
 *
 *         val partitionKey: PK?
 *     }
 *
 *     interface CompositeKey<PK : KeyType, SK : KeyType> : DeleteItemRequest {
 *         companion object { }
 *
 *         val partitionKey: PK?
 *         val sortKey: SK?
 *     }
 * }
 *
 * data class DeleteItemRequestPartitionKeyImpl<PK : KeyType>(
 *     override val returnConsumedCapacity: ReturnConsumedCapacity?,
 *     override val returnItemCollectionMetrics: ReturnItemCollectionMetrics?,
 *     override val returnValues: ReturnValue?,
 *     override val returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?,
 *     override val partitionKey: PK?,
 * ): DeleteItemRequest.PartitionKey<PK>
 *
 * // Builder is rendered by `BuilderRenderer` and thus omitted here
 *
 * fun <PK : KeyType> DeleteItemRequest.PartitionKey<PK>.toBuilder(): DeleteItemRequestPartitionKeyBuilder<PK> =
 *     DeleteItemRequestPartitionKeyBuilder<PK>().apply {
 *         returnConsumedCapacity = this@toBuilder.returnConsumedCapacity
 *         returnItemCollectionMetrics = this@toBuilder.returnItemCollectionMetrics
 *         returnValues = this@toBuilder.returnValues
 *         returnValuesOnConditionCheckFailure = this@toBuilder.returnValuesOnConditionCheckFailure
 *         partitionKey = this@toBuilder.partitionKey
 *     }
 *
 * fun <PK : KeyType> DeleteItemRequest.PartitionKey<PK>.copy(
 *     block: DeleteItemRequestPartitionKeyBuilder<PK>.() -> Unit,
 * ): DeleteItemRequest.PartitionKey<PK> = toBuilder().apply(block).build()
 *
 * fun <PK : KeyType> DeleteItemRequest.Companion.PartitionKey(
 *     block: DeleteItemRequestPartitionKeyBuilder<PK>.() -> Unit,
 * ): DeleteItemRequest.PartitionKey<PK> = DeleteItemRequestPartitionKeyBuilder<PK>().apply(block).build()
 *
 * data class DeleteItemRequestCompositeKeyImpl<PK : KeyType, SK : KeyType>(
 *     override val returnConsumedCapacity: ReturnConsumedCapacity?,
 *     override val returnItemCollectionMetrics: ReturnItemCollectionMetrics?,
 *     override val returnValues: ReturnValue?,
 *     override val returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?,
 *     override val partitionKey: PK?,
 *     override val sortKey: SK?,
 * ): DeleteItemRequest.CompositeKey<PK, SK>
 *
 * // Builder is rendered by `BuilderRenderer` and thus omitted here
 *
 * fun <PK : KeyType, SK : KeyType> DeleteItemRequest.CompositeKey<PK, SK>.toBuilder(): DeleteItemRequestCompositeKeyBuilder<PK, SK> =
 *     DeleteItemRequestCompositeKeyBuilder<PK, SK>().apply {
 *         returnConsumedCapacity = this@toBuilder.returnConsumedCapacity
 *         returnItemCollectionMetrics = this@toBuilder.returnItemCollectionMetrics
 *         returnValues = this@toBuilder.returnValues
 *         returnValuesOnConditionCheckFailure = this@toBuilder.returnValuesOnConditionCheckFailure
 *         partitionKey = this@toBuilder.partitionKey
 *         sortKey = this@toBuilder.sortKey
 *     }
 *
 * fun <PK : KeyType, SK : KeyType> DeleteItemRequest.CompositeKey<PK, SK>.copy(
 *     block: DeleteItemRequestCompositeKeyBuilder<PK, SK>.() -> Unit,
 * ): DeleteItemRequest.CompositeKey<PK, SK> = toBuilder().apply(block).build()
 *
 * fun <PK : KeyType, SK : KeyType> DeleteItemRequest.Companion.CompositeKey(
 *     block: DeleteItemRequestCompositeKeyBuilder<PK, SK>.() -> Unit,
 * ): DeleteItemRequest.CompositeKey<PK, SK> = DeleteItemRequestCompositeKeyBuilder<PK, SK>().apply(block).build()
 * ```
 */
internal class KeyProjectedTypeRenderer(
    private val ctx: RenderContext,
    generator: CodeGenerator,
    private val keyProjections: KeyProjections,
) : CodeGenerator by generator {
    private val unkeyedProjection = keyProjections.unkeyedProjection

    fun generate() {
        renderInterfaces()
        blankLine()
        renderImpls()
    }

    private fun renderInterfaces() {
        renderInterface(KeyProjectionType.NONE) {
            if (keyProjections.isKeyed) {
                blankLine()
                renderInterface(KeyProjectionType.PARTITION_KEY)
                blankLine()
                renderInterface(KeyProjectionType.COMPOSITE_KEY)
            }
        }
    }

    private fun renderInterface(keyType: KeyProjectionType, innerBlock: () -> Unit = { }) {
        val projection = keyProjections[keyType]
        val isAbstract = keyType == KeyProjectionType.NONE && keyProjections.isKeyed
        val interfaceModifier = if (isAbstract) "sealed " else ""

        writeInline("public #Linterface #D ", interfaceModifier, projection.interfaceStruct.type)

        if (keyType != KeyProjectionType.NONE) {
            writeInline(": #T ", unkeyedProjection.interfaceStruct.type)
        }

        withBlock("{", "}") {
            write("public companion object { }")
            blankLine()
            projection.interfaceStruct.members {
                if (!isInherited) write("public val #L: #T", name, type)
            }

            innerBlock()
        }
    }

    private fun renderImpls() {
        if (keyProjections.isKeyed) {
            renderImpl(KeyProjectionType.PARTITION_KEY)
            blankLine()
            renderImpl(KeyProjectionType.COMPOSITE_KEY)
        } else {
            renderImpl(KeyProjectionType.NONE)
        }
    }

    private fun renderImpl(keyType: KeyProjectionType) {
        val projection = keyProjections[keyType]
        val generics = projection.implStruct.type.genericVars()
        val receiver = when {
            keyProjections.isKeyed -> "${unkeyedProjection.interfaceStruct.type.shortName}.Companion."
            else -> ""
        }

        renderDataClass(projection)
        blankLine()
        renderBuilder(projection)
        blankLine()
        renderToBuilder(generics, projection)
        blankLine()
        renderCopy(generics, projection)
        blankLine()
        renderFactory(generics, receiver, projection)
    }

    private fun renderDataClass(projection: KeyProjection) {
        openBlock("private data class #D(", projection.implStruct.type)
        projection.implStruct.members { write("override val #L: #T,", name, type) }
        closeBlock("): #T", projection.interfaceStruct.type)
    }

    private fun renderBuilder(projection: KeyProjection) {
        val builderCtx = ctx.copy(
            attributes = ctx.attributes + (RenderOptions.VisibilityAttribute to Visibility.PUBLIC),
        )
        BuilderRenderer(
            ctx = builderCtx,
            generator = this@KeyProjectedTypeRenderer,
            builtStructure = projection.interfaceStruct,
            implementationType = projection.implStruct.type,
            members = projection.builderStruct.members,
            builderNameOverride = projection.builderStruct.type.shortName,
        ).render()
    }

    private fun renderToBuilder(
        generics: GenericsSet,
        projection: KeyProjection,
    ) {
        withBlock(
            "public fun #1G#2T.toBuilder(): #3T = #3T().apply {",
            "}",
            generics,
            projection.interfaceStruct.type,
            projection.builderStruct.type,
        ) {
            projection.builderStruct.members { write("#1L = this@toBuilder.#1L", name) }
        }
    }

    private fun renderCopy(
        generics: GenericsSet,
        projection: KeyProjection,
    ) {
        withBlock(
            "public fun #1G#2T.copy(block: #3T.() -> Unit): #2T =",
            "",
            generics,
            projection.interfaceStruct.type,
            projection.builderStruct.type,
        ) {
            write("toBuilder().apply(block).build()")
        }
    }

    private fun renderFactory(
        generics: GenericsSet,
        receiver: String,
        projection: KeyProjection,
    ) {
        withBlock(
            "public fun #G#L#L(block: #T.() -> Unit): #T =",
            "",
            generics,
            receiver,
            projection.interfaceStruct.type.leafName,
            projection.builderStruct.type,
            projection.interfaceStruct.type,
        ) {
            write("#T().apply(block).build()", projection.builderStruct.type)
        }
    }
}
