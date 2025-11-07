/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.codegen.renderers

import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.IoMapping
import aws.sdk.kotlin.hll.s3transfermanager.codegen.utils.operationMembers
import com.google.devtools.ksp.processing.Resolver

/**
 * Renders request and response types
 */
internal class IoRenderer(
    ctx: RenderContext,
    val className: String,
    val mapping: IoMapping,
    val resolver: Resolver,
) : RendererBase(ctx, className) {
    override fun generate() {
        val members = resolver
            .operationMembers(
                mapping.sourceOperation,
                mapping.type,
                mapping.members,
            )

        withBlock(
            "public class #L private constructor(builder: Builder) {",
            "}",
            className,
        ) {
            members.forEach { member ->
                member.kDocs?.let { write(it) } // FIXME: KSP isn't detecting KDocs
                write(
                    "public val #1L: #2T = builder.#1L",
                    member.name,
                    member.type,
                )
            }
            blankLine()

            withBlock(
                "public companion object {",
                "}",
            ) {
                write(
                    "public operator fun invoke(block: Builder.() -> Unit): #L = Builder().apply(block).build()",
                    className,
                )
            }
            blankLine()

            withBlock(
                "public class Builder {",
                "}",
            ) {
                members.forEach { member ->
                    write(
                        "public var #L: #T = null",
                        member.name,
                        member.type,
                    )
                }
                blankLine()

                write("@PublishedApi")
                write(
                    "internal fun build(): #1L = #1L(this)",
                    className,
                )
            }
        }
    }
}
