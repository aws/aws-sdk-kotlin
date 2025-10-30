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

            imports += ImportDirective(conversion.source)
            imports += ImportDirective(conversion.destination)

            conversion.additionalImports.forEach {
                imports += ImportDirective(it)
            }

            withBlock(
                "internal fun ${conversion.source.shortName}.$functionName(${conversion.additionalParameters.joinToString(", ")}): ${conversion.destination.shortName} = ${conversion.destination.shortName} {",
                "}",
            ) {
                conversion.members.forEach { member ->
                    write("$member = this@$functionName.$member")
                }
                write(conversion.additionalLogic)
            }
            blankLine()
        }
    }
}
