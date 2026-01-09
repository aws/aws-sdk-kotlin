/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.route53

import aws.sdk.kotlin.codegen.sdkId
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.*
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.model.hasTrait
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import aws.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.HttpLabelTrait

class TrimResourcePrefix : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).sdkId.equals("route 53", ignoreCase = true)

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> =
        resolved + TrimResourcePrefixMiddleware()
}

private class TrimResourcePrefixMiddleware : ProtocolMiddleware {
    override val name: String
        get() = "TrimResourcePrefix"

    override val order: Byte = -128

    override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean {
        val input = op.input.getOrNull()?.let { ctx.model.expectShape<StructureShape>(it) }
        return input?.members()?.any(MemberShape::shouldTrimResourcePrefix) ?: false
    }

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        val pathMember = ctx.model.expectShape<StructureShape>(op.input.get()).members().first(
            MemberShape::shouldTrimResourcePrefix,
        ).defaultName()

        writer.withBlock("op.execution.initialize.intercept { req, next -> ", "}") {
            write("""val prefix = "^/?.*?/".toRegex()""")
            withBlock("if (req.subject.#L?.contains(prefix) == true) {", "}", pathMember) {
                write(
                    """val updated = req.subject.copy { #L = req.subject.#L?.replace(prefix, "") }""",
                    pathMember,
                    pathMember,
                )
                write("next.call(#T(req.context, updated))", RuntimeTypes.HttpClient.Operation.OperationRequest)
            }
            withBlock("else {", "}") {
                write("next.call(req)")
            }
        }
    }
}

private fun MemberShape.shouldTrimResourcePrefix(): Boolean =
    (target.name == "ResourceId" || target.name == "ChangeId") &&
        hasTrait<HttpLabelTrait>()
