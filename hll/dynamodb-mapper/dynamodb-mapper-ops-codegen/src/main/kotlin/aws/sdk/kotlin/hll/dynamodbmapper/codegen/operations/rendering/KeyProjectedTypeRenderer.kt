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
        renderInterface(StructureKeyType.NONE) {
            if (keyProjections.isKeyed) {
                blankLine()
                renderInterface(StructureKeyType.PARTITION_KEY)
                blankLine()
                renderInterface(StructureKeyType.COMPOSITE_KEY)
            }
        }
    }

    private fun renderInterface(keyType: StructureKeyType, innerBlock: () -> Unit = { }) {
        val projection = keyProjections[keyType]
        val isAbstract = keyType == StructureKeyType.NONE && keyProjections.isKeyed
        val interfaceModifier = if (isAbstract) "sealed " else ""

        writeInline("public #Linterface #D ", interfaceModifier, projection.interfaceStruct.type)

        if (keyType != StructureKeyType.NONE) {
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
            renderImpl(StructureKeyType.PARTITION_KEY)
            blankLine()
            renderImpl(StructureKeyType.COMPOSITE_KEY)
        } else {
            renderImpl(StructureKeyType.NONE)
        }
    }

    private fun renderImpl(keyType: StructureKeyType) {
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
            generator = this@KeyProjectedTypeRenderer,
            builtType = projection.interfaceStruct.type,
            implementationType = projection.implStruct.type,
            members = projection.builderStruct.members,
            ctx = builderCtx,
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
