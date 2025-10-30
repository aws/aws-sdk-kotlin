/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.codegen.renderers

import aws.sdk.kotlin.hll.codegen.core.ImportDirective
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.IOMapping
import aws.sdk.kotlin.hll.s3transfermanager.codegen.utils.operationMembers
import aws.sdk.kotlin.hll.s3transfermanager.codegen.utils.renderMember
import com.google.devtools.ksp.processing.Resolver

/**
 * Renders request and response types
 */
internal class IORenderer(
    ctx: RenderContext,
    val className: String,
    val mapping: IOMapping,
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
            "public class $className private constructor(builder: Builder) {",
            "}",
        ) {
            members.forEach { member ->
                val memberType = member.type as TypeRef

                imports += ImportDirective(memberType) // Type: SomeType
                memberType.genericArgs.forEach { genericArg ->
                    imports += ImportDirective(genericArg as TypeRef) // Type: Map<SomeType, SomeType>
                }

                member.kDocs?.let { write(it) } // FIXME: KSP isn't detecting KDocs
                write(
                    "public val ${member.name}: ${member.type.renderMember()}? = builder.${member.name}",
                )
            }
            blankLine()

            withBlock(
                "public companion object {",
                "}",
            ) {
                write("public operator fun invoke(block: Builder.() -> Unit): $className = Builder().apply(block).build()")
            }
            blankLine()

            withBlock(
                "public class Builder {",
                "}",
            ) {
                members.forEach { member ->
                    write(
                        "public var ${member.name}: ${(member.type as TypeRef).renderMember()}? = null",
                    )
                }
                blankLine()

                write("@PublishedApi")
                write("internal fun build(): $className = $className(this)")
            }
        }
    }
}
