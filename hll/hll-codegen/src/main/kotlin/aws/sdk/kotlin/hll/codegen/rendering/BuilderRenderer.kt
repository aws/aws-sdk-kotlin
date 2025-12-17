/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.codegen.rendering

import aws.sdk.kotlin.hll.codegen.core.CodeGenerator
import aws.sdk.kotlin.hll.codegen.model.*
import aws.sdk.kotlin.hll.codegen.util.generatedAnnotation
import aws.sdk.kotlin.hll.codegen.util.visibility
import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * A DSL-style builder renderer.
 * @param generator The generator in which the builder will be written
 * @param builtType The [Structure] for which a builder will be generated
 * @param implementationType The [TypeRef] representing the implementing type whose constructor will be called by the
 * generated `build` method. This type must expose a constructor which accepts each element of [members] as parameters.
 * Note that this type doesn't have to be public (merely accessible to the `build` method) and may be the same as
 * [builtType] if it has an appropriate constructor.
 * @param members The [Set] of members of [builtType] which will be included in the builder
 * @param ctx The rendering context
 */
@InternalSdkApi
public class BuilderRenderer(
    private val generator: CodeGenerator,
    private val builtStructure: Structure,
    private val implementationType: TypeRef,
    private val members: Set<Member>,
    private val ctx: RenderContext,
) : CodeGenerator by generator {
    @InternalSdkApi
    public companion object {
        public fun builderName(builtType: Structure): String = "${builtType.type.shortName}Builder"
        public fun builderType(builtType: Structure): TypeRef = builtType.type.copy(shortName = builderName(builtType))
    }

    private val builtType = builtStructure.type
    private val builderName = builderName(builtStructure)

    public fun render() {
        docs("A DSL-style builder for instances of [#T]", builtType)

        generatedAnnotation(builtStructure, ctx)
        val genericParams = members.flatMap { it.type.genericVars() }.asParamsList()
        withBlock("#Lclass #L#L {", "}", ctx.attributes.visibility, builderName, genericParams) {
            members.forEach(::renderProperty)
            blankLine()

            generatedAnnotation(builtStructure, ctx)
            withBlock("#Lfun build(): #T {", "}", ctx.attributes.visibility, builtType, genericParams) {
                members.forEach {
                    if (it.type.nullable) {
                        write("val #1L = #1L", it.name)
                    } else {
                        write("val #1L = requireNotNull(#1L) { #2S }", it.name, "Missing value for ${it.name}")
                    }
                }
                blankLine()
                withBlock("return #T(", ")", implementationType) {
                    members.forEach {
                        write("#L,", it.name)
                    }
                }
            }
        }
        blankLine()
    }

    private fun renderProperty(member: Member) {
        val dslInfo = member.dslInfo

        if (dslInfo != null) {
            blankLine()
        }

        generatedAnnotation(member, ctx)
        write("#Lvar #L: #T = null", ctx.attributes.visibility, member.name, member.type.nullable())

        if (dslInfo != null) {
            blankLine()
            generatedAnnotation(member, ctx)
            withBlock(
                "#Lfun #L(block: #T.() -> #T) {",
                "}",
                ctx.attributes.visibility,
                member.name,
                dslInfo.interfaceType,
                member.type,
            ) {
                val constructorIfNecessary = if (dslInfo.implSingleton) "" else "()"
                write("#L = #T#L.run(block)", member.name, dslInfo.implType, constructorIfNecessary)
            }
            blankLine()
        }

        // TODO add DSL methods for low-level structure members
    }
}
