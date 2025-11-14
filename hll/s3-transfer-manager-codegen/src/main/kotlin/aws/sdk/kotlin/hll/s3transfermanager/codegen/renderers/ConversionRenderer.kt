/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.codegen.renderers

import aws.sdk.kotlin.hll.codegen.core.ImportDirective
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.ConversionMapping
import com.google.devtools.ksp.processing.Resolver

internal class ConversionRenderer(
    ctx: RenderContext,
    fileName: String,
    val conversions: List<ConversionMapping>,
    val resolver: Resolver,
) : RendererBase(ctx, fileName) {
    override fun generate() {
        conversions.forEach { conversion ->
            val functionName = "to${conversion.destination.shortName}"

            conversion.additionalImports.forEach {
                imports += ImportDirective(it)
            }

            withBlock(
                "internal fun #1T.#2L(#3L): #4T = #4T {",
                "}",
                conversion.source,
                functionName,
                conversion.additionalParameters.joinToString(", "),
                conversion.destination,
            ) {
                conversion.members.forEach { member ->
                    write(
                        "#1L = this@#2L.#1L",
                        member,
                        functionName,
                    )
                }
                write(conversion.additionalLogic)
            }
            blankLine()
        }
    }
}
