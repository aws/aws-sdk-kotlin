/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering

import aws.sdk.kotlin.hll.codegen.model.*
import aws.sdk.kotlin.hll.codegen.rendering.BuilderRenderer
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.codegen.util.generatedAnnotation
import aws.sdk.kotlin.hll.codegen.util.plus
import aws.sdk.kotlin.hll.codegen.util.visibility
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.BuilderSpec
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.GeneratedAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.ItemConverterSpec
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.KeyAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.MappedItem
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import aws.smithy.kotlin.codegen.core.RuntimeTypes

/**
 * Renders the classes and objects required to make a class usable with the DynamoDbMapper such as schemas, builders,
 * and converters. All decisions have already been made in [item]; this class only emits code.
 * @param ctx the [RenderContext] of the renderer
 * @param item The resolved IR for the annotated class
 */
internal class SchemaRenderer(
    private val ctx: RenderContext,
    private val item: MappedItem,
) : RendererBase(ctx, "${item.name}Schema") {
    private val className = item.name
    private val classType = item.type

    private val schemaName = "${className}Schema"

    private val partitionKeyTypeRefs = item.metadata.partitionKeys.map { it.type }
    private val sortKeyTypeRefs = item.metadata.sortKeys.map { it.type }

    override fun generate() {
        item.builder?.let(::renderBuilder)

        (item.itemConverter as? ItemConverterSpec.Generated)?.let(::renderItemConverter)

        item.valueConverter?.let(::renderItemValueConverter)

        if (item.kind == MappedItem.Kind.Table) {
            renderSchema()
        }

        if (item.generateGetTable) {
            renderGetTable()
        }
    }

    private fun renderBuilder(builder: BuilderSpec) {
        val builderCtx = ctx.copy(
            attributes = ctx.attributes + (ModelAttributes.GeneratedApi to true),
        )
        BuilderRenderer(builderCtx, this, builder.builtStructure, builder.implementationType, builder.members).render()
    }

    private fun renderItemConverter(converter: ItemConverterSpec.Generated) {
        generatedAnnotation()
        withBlock(
            "#Lobject #L : #T by #T(",
            ")",
            ctx.attributes.visibility,
            converter.converter.shortName,
            MapperTypes.Items.itemConverter(classType),
            MapperTypes.Items.SimpleItemConverter,
        ) {
            val builder = item.builder
            if (builder != null) {
                write("builderFactory = ::#L,", builder.name)
                write("build = #L::build,", builder.name)
            } else {
                write("builderFactory = { $className() },")
                write("build = { this },")
            }

            withBlock("descriptors = arrayOf(", "),") {
                converter.attributes.forEach(::renderAttributeDescriptor)
            }
        }
        blankLine()
    }

    /**
     * Render a `ValueConverter` for the current class by wrapping the generated/user-provided `ItemConverter`
     * with our `ItemValueConverter`.
     */
    private fun renderItemValueConverter(valueConverter: TypeRef) {
        generatedAnnotation()
        write(
            "#Lval #L: #T = #T(#T, #T)",
            ctx.attributes.visibility,
            valueConverter.shortName,
            MapperTypes.Values.valueConverter(classType),
            HllTypes.MappingCore.Converters.ConverterChain,
            item.itemConverter.converter,
            MapperTypes.Values.ItemValueConverter,
        )
        blankLine()
    }

    private fun renderAttributeDescriptor(attribute: GeneratedAttribute) {
        val prop = attribute.property
        ctx.logger.info("Rendering an attribute descriptor for ${prop.name}")
        withBlock("#T(", "),", MapperTypes.Items.AttributeDescriptor) {
            write("#S,", prop.ddbName) // key
            write("#L,", "$className::${prop.name}") // getter

            // setter
            write("#L,", "${item.builder?.name ?: className}::${prop.name}::set")

            // converter
            renderValueConverter(attribute.converter)
            write(",")
        }
    }

    private fun renderSchema() {
        val metadata = item.metadata
        val schemaType = if (metadata.sortKeys.isEmpty()) {
            MapperTypes.Items.itemSchemaPartitionKey(classType, partitionKeyTypeRefs)
        } else {
            MapperTypes.Items.itemSchemaCompositeKey(classType, partitionKeyTypeRefs, sortKeyTypeRefs)
        }

        generatedAnnotation()
        withBlock("#Lobject #L : #T {", "}", ctx.attributes.visibility, schemaName, schemaType) {
            write("override val converter: #1T = #1T", item.itemConverter.converter)

            writeInline("override val partitionKey: #T = ", MapperTypes.Items.keySpec(partitionKeyTypeRefs))
            keySpecInstantiation(metadata.partitionKeys)
            newline()

            if (metadata.sortKeys.isNotEmpty()) {
                writeInline("override val sortKey: #T = ", MapperTypes.Items.keySpec(sortKeyTypeRefs))
                keySpecInstantiation(metadata.sortKeys)
                newline()
            }

            val ttlFields = metadata.ttlFields
            val counterFields = metadata.counterFields
            val hasAttributes = ttlFields.isNotEmpty() || counterFields.isNotEmpty()

            if (hasAttributes) {
                if (ttlFields.isNotEmpty()) {
                    val ttlFieldsCode = ttlFields.entries.joinToString { (fieldName, lifetime) ->
                        format("#S to #LL", fieldName, lifetime)
                    }
                    write("private val ttlFields = #T(#L)", Types.Kotlin.Collections.mapOf, ttlFieldsCode)
                }

                withBlock(
                    "override val attributes: #T = #T {",
                    "}",
                    Type.from(RuntimeTypes.Core.Collections.Attributes),
                    Type.from(RuntimeTypes.Core.Collections.attributesOf),
                ) {
                    if (ttlFields.isNotEmpty()) {
                        write("#T.TtlFields to ttlFields", MapperTypes.Model.SchemaAttributes)
                    }
                    if (counterFields.isNotEmpty()) {
                        val counterFieldsCode = counterFields.joinToString { format("#S", it) }

                        write(
                            "#T.CounterFields to #T(#L)",
                            MapperTypes.Model.SchemaAttributes,
                            Types.Kotlin.Collections.setOf,
                            counterFieldsCode,
                        )
                    }
                }
            } else {
                write(
                    "override val attributes: #T = #T()",
                    Type.from(RuntimeTypes.Core.Collections.Attributes),
                    Type.from(RuntimeTypes.Core.Collections.emptyAttributes),
                )
            }
        }

        blankLine()
    }

    private fun keySpecInstantiation(keys: List<KeyAttribute>) {
        val first = keys.first()
        writeInline("#T(#S)", first.keySpecType, first.ddbName)

        keys.drop(1).forEach { key ->
            writeInline(".#T(#S)", key.thenType, key.ddbName)
        }
    }

    private fun renderGetTable() {
        docs("Returns a reference to a table named [name] containing items representing [#T]", classType)

        val tableType = if (item.metadata.sortKeys.isEmpty()) {
            MapperTypes.Model.tablePartitionKey(classType, partitionKeyTypeRefs)
        } else {
            MapperTypes.Model.tableCompositeKey(classType, partitionKeyTypeRefs, sortKeyTypeRefs)
        }

        generatedAnnotation()
        val fnName = "get${className}Table"
        write(
            "#Lfun #T.#L(name: String): #T = #L(name, #L)",
            ctx.attributes.visibility,
            MapperTypes.DynamoDbMapper,
            fnName,
            tableType,
            "getTable",
            schemaName,
        )
    }
}
