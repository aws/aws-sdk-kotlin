/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.rendering

import aws.sdk.kotlin.hll.codegen.core.ImportDirective
import aws.sdk.kotlin.hll.codegen.model.Member
import aws.sdk.kotlin.hll.codegen.model.Structure
import aws.sdk.kotlin.hll.codegen.model.genericVars
import aws.sdk.kotlin.hll.codegen.model.lowLevel
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.*

/**
 * Renders an input or output high-level type and conversion method(s) from/to the corresponding low-level type. This
 * renderer models the notion of a "from" type and a "to" type which are based on the request pipeline. Specifically,
 * "from" types are high-level requests and low-level responses whereas "to" types are low-level requests and high-level
 * responses. These distinctions are important for generating the conversion method.
 *
 * ## Example output for unkeyed projection
 *
 * In the simplest case of an unkeyed projection, this renderer will produce code such as:
 *
 * ```kotlin
 * import aws.sdk.kotlin.services.dynamodb.model.DeleteItemResponse as LowLevelDeleteItemResponse
 *
 * // Interface, impl, builder, etc. are produced by `KeyProjectedTypeRenderer` and thus omitted in this example
 *
 * fun <T> LowLevelDeleteItemResponse.convert(schema: ItemSchema<T>) = DeleteItemResponse<T> {
 *     consumedCapacity = this@convert.consumedCapacity
 *     itemCollectionMetrics = this@convert.itemCollectionMetrics
 *     attributes = this@convert.attributes?.let {
 *         schema.converter.convertLeft(it.toItem())
 *     }
 * }
 * ```
 *
 * ## Example output for keyed projections
 *
 * For keyed projections, multiple converters will be generated:
 *
 * ```kotlin
 * import aws.sdk.kotlin.services.dynamodb.model.ScanRequest as LowLevelScanRequest
 *
 * // Interfaces, impls, builders, etc. are produced by `KeyProjectedTypeRenderer` and thus omitted in this example
 *
 * fun <PK : KeyType, T> ScanRequest.PartitionKey<PK>.convert(
 *     indexName: String?,
 *     tableName: String?,
 *     schema: ItemSchema.PartitionKey<T, PK>,
 * ) = LowLevelScanRequest {
 *     consistentRead = this@convert.consistentRead
 *     limit = this@convert.limit
 *     returnConsumedCapacity = this@convert.returnConsumedCapacity
 *     segment = this@convert.segment
 *     select = this@convert.select
 *     totalSegments = this@convert.totalSegments
 *     exclusiveStartKey = keysToItem(schema, this@convert.exclusiveStartPartitionKey)
 *     this.indexName = indexName
 *     this.tableName = tableName
 *
 *     val expressionVisitor = ParameterizingExpressionVisitor()
 *     filterExpression = this@convert.filter?.accept(expressionVisitor)
 *     expressionAttributeNames = expressionVisitor.expressionAttributeNames()
 *     expressionAttributeValues = expressionVisitor.expressionAttributeValues()
 * }
 *
 * internal fun <PK : KeyType, SK : KeyType, T> ScanRequest.CompositeKey<PK, SK>.convert(
 *     indexName: String?,
 *     tableName: String?,
 *     schema: ItemSchema.CompositeKey<T, PK, SK>,
 * ) = LowLevelScanRequest {
 *     consistentRead = this@convert.consistentRead
 *     limit = this@convert.limit
 *     returnConsumedCapacity = this@convert.returnConsumedCapacity
 *     segment = this@convert.segment
 *     select = this@convert.select
 *     totalSegments = this@convert.totalSegments
 *     exclusiveStartKey = keysToItem(
 *         schema,
 *         this@convert.exclusiveStartPartitionKey,
 *         this@convert.exclusiveStartSortKey,
 *     )
 *     this.indexName = indexName
 *     this.tableName = tableName
 *
 *     val expressionVisitor = ParameterizingExpressionVisitor()
 *     filterExpression = this@convert.filter?.accept(expressionVisitor)
 *     expressionAttributeNames = expressionVisitor.expressionAttributeNames()
 *     expressionAttributeValues = expressionVisitor.expressionAttributeValues()
 * }
 * ```
 *
 * @param ctx The active [RenderContext]
 * @param keyProjections The family of key projections for the high-level type
 */
internal abstract class IoRenderer(
    protected val ctx: RenderContext,
    private val keyProjections: KeyProjections,
) : RendererBase(ctx, keyProjections.unkeyedProjection.interfaceStruct.type.shortName) {
    /**
     * The key projections for the "from" type
     */
    protected abstract val fromProjections: KeyProjections

    /**
     * The key projections for the "to" type
     */
    protected abstract val toProjections: KeyProjections

    /**
     * Derives the equivalent "from" [Member] for this "to" [Member]. This method should only be used for members with a
     * codegen behavior _other than_ [MemberCodegenBehavior.MapToKeys].
     * @param fromStruct The "from" structure
     */
    protected abstract fun Member.fromMember(fromStruct: Structure): Member

    /**
     * Derives the list of "from" [Member] instances for this "to" [Member]. This method should only be used for members
     * with a codegen behavior of [MemberCodegenBehavior.MapToKeys].
     * @param fromStruct The "from" structure
     */
    protected abstract fun Member.fromMembers(fromStruct: Structure): List<Member>

    /**
     * Renders the key conversion lines for the overall conversion method
     * @param fromStruct The "from" structure
     * @param toStruct The "to" structure
     */
    protected abstract fun renderKeyConversion(fromStruct: Structure, toStruct: Structure)

    /**
     * Renders a single item conversion inline
     */
    protected abstract fun renderSingleItemConversion()

    final override fun generate() {
        KeyProjectedTypeRenderer(ctx, this, keyProjections).generate()

        // Manually import the low-level type with a specific alias
        val llType = keyProjections.unkeyedProjection.interfaceStruct.lowLevel.type
        imports += ImportDirective(llType, "LowLevel${llType.shortName}")

        val isKeyed = fromProjections.isKeyed || toProjections.isKeyed
        val keyTypes = when {
            isKeyed -> listOf(KeyProjectionType.PARTITION_KEY, KeyProjectionType.COMPOSITE_KEY)
            else -> listOf(KeyProjectionType.NONE)
        }

        keyTypes.forEach { keyType ->
            val fromKeyType = fromProjections[keyType]
            val toKeyType = toProjections[keyType]
            renderConversion(keyType, fromKeyType.interfaceStruct, toKeyType.interfaceStruct)
        }
    }

    private fun renderConversion(keyType: KeyProjectionType, fromStruct: Structure, toStruct: Structure) {
        val isSchemaless = fromStruct.type.genericVars().isEmpty() && toStruct.type.genericVars().isEmpty()

        val schemaType = when {
            isSchemaless -> null
            keyType == KeyProjectionType.NONE -> MapperTypes.Items.ItemSchema
            keyType == KeyProjectionType.PARTITION_KEY -> MapperTypes.Items.ItemSchemaPartitionKey
            keyType == KeyProjectionType.COMPOSITE_KEY -> MapperTypes.Items.ItemSchemaCompositeKey
            else -> error("Unknown key/schema constraints")
        }

        val generics = fromStruct.type.genericVars() + toStruct.type.genericVars() + schemaType.genericVars()

        blankLine()
        withBlock("internal fun #G#T.convert(", "}", generics, fromStruct.type) {
            toStruct.members(MemberCodegenBehavior.Hoist) { write("#L: #T,", name, type) }

            val extraParams = fromStruct.conversionParameters + toStruct.conversionParameters
            extraParams.forEach { parameter -> write("#L: #T,", parameter.name, parameter.type) }

            schemaType?.let { write("schema: #T,", it) }

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

            toStruct.members<MemberCodegenBehavior.CustomTransformation> {
                val transform = codegenBehavior as MemberCodegenBehavior.CustomTransformation
                val fromMemberString = format("this@convert.#L", fromMember(fromStruct).name)
                val conversionString = transform.renderConversion(this@IoRenderer, fromMemberString)
                write("this.#L = #L", name, conversionString)
            }

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

                toStruct.members(MemberCodegenBehavior.ExpressionLiteral(ExpressionLiteralType.Update)) {
                    write("#L = this@convert.#L?.accept(expressionVisitor)", name, fromMember(fromStruct).name)
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

/**
 * Renders a request high-level type and conversion method(s) to the corresponding low-level type
 * @param ctx The active [RenderContext]
 * @param keyProjections The family of key projections for the high-level request type
 */
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

/**
 * Renders a response high-level type and conversion method(s) from the corresponding low-level type
 * @param ctx The active [RenderContext]
 * @param keyProjections The family of key projections for the high-level response type
 */
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
