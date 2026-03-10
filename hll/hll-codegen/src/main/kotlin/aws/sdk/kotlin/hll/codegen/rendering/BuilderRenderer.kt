/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.codegen.rendering

import aws.sdk.kotlin.hll.codegen.core.CodeGenerator
import aws.sdk.kotlin.hll.codegen.model.*
import aws.sdk.kotlin.hll.codegen.util.visibility
import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * A DSL-style builder renderer.
 * @param ctx The rendering context
 * @param generator The generator in which the builder will be written
 * @param builtType The [TypeRef] representing the type for which a builder will be generated. This type can be a class
 * or an interface.
 * @param implementationType The [TypeRef] representing the implementing type whose constructor will be called by the
 * generated `build` method. This type must expose a constructor which accepts each element of [members] as parameters.
 * Note that this type doesn't have to be public (merely accessible to the `build` method) and may be the same as
 * [builtType] if it has an appropriate constructor.
 * @param members The [Set] of members of [builtType] which will be included in the builder
 * @param builderNameOverride May be set to override the name for the builder. If not set, the builder name will be
 * the same as the interface name concatenated with "Builder".
 */
@InternalSdkApi
public class BuilderRenderer(
    private val ctx: RenderContext,
    private val generator: CodeGenerator,
    private val builtType: TypeRef,
    private val implementationType: TypeRef,
    private val members: Set<Member>,
    builderNameOverride: String? = null,
) : CodeGenerator by generator {
    @InternalSdkApi
    public companion object {
        public fun defaultBuilderName(builtType: TypeRef): String = "${builtType.shortName.replace(".", "")}Builder"
    }

    private val builderName = builderNameOverride ?: defaultBuilderName(builtType)

    public fun render() {
        docs("A DSL-style builder for instances of [#T]", builtType)

        val generics = members.genericVars()
        withBlock("#Lclass #L#G {", "}", ctx.attributes.visibility, builderName, generics) {
            members.forEach(::renderProperty)
            blankLine()

            withBlock("#Lfun build(): #T {", "}", ctx.attributes.visibility, builtType) {
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
        val dsls = member.dsls

        if (dsls.isNotEmpty()) {
            blankLine()
        }

        write("#Lvar #L: #T = null", ctx.attributes.visibility, member.name, member.type.nullable())

        dsls.forEach { dslInfo ->
            val dslBlockResultType = when (dslInfo.implFinalizer) {
                null -> member.type
                else -> Types.Kotlin.Unit
            }

            val dslName = dslInfo.nameOverride ?: member.name

            blankLine()
            writeInline("#Lfun #G#L(", ctx.attributes.visibility, dslInfo.interfaceType.genericVars(), dslName)

            dslInfo.dslMethodParams.forEach { arg ->
                writeInline("#L: #T, ", arg.name, arg.type)
            }

            withBlock(
                "block: #T.() -> #T) {",
                "}",

                dslInfo.interfaceType,
                dslBlockResultType,
            ) {
                val scopeMethod = when (dslInfo.implFinalizer) {
                    null -> "run" // The result of DSL method call is the DSL block return value
                    else -> "apply" // The result of the DSL method call is provided by the finalizer
                }

                write(
                    "#L = #T#L.#L(block)#L",
                    member.name,
                    dslInfo.implType,
                    dslInfo.implInvocationStyle.invocationString,
                    scopeMethod,
                    dslInfo.implFinalizer ?: "",
                )
            }
            blankLine()
        }

        // TODO add DSL methods for low-level structure members
    }
}
