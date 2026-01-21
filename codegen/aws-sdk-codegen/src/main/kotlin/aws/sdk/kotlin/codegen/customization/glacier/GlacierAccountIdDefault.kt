/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization.glacier

import aws.sdk.kotlin.codegen.sdkId
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.*
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import aws.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.*

/**
 * Adds a middleware for Glacier to autofill accountId when not set
 * See: https://github.com/awslabs/aws-sdk-kotlin/issues/246
 */
class GlacierAccountIdDefault : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = model.expectShape<ServiceShape>(settings.service).sdkId.equals("Glacier", ignoreCase = true)

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + GlacierAccountIdMiddleware()
}

private class GlacierAccountIdMiddleware : ProtocolMiddleware {
    override val name: String
        get() = "GlacierAccountIdAutoFill"

    override val order: Byte = -100

    override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean {
        val input = op.input.getOrNull()?.let { ctx.model.expectShape<StructureShape>(it) }
        return input?.memberNames?.any { it.lowercase() == "accountid" } ?: false
    }

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        val accountId = ctx.model.expectShape<StructureShape>(op.input.get()).members().first { it.memberName.lowercase() == "accountid" }
        writer.addImport(RuntimeTypes.HttpClient.Operation.OperationRequest)

        writer.withBlock("op.execution.initialize.intercept { req, next -> ", "}") {
            openBlock("if (req.subject.#L.isNullOrEmpty()) {", accountId.defaultName())
                .write("val updated = req.subject.copy { #L = #S }", accountId.defaultName(), "-")
                .write("next.call(#T(req.context, updated))", RuntimeTypes.HttpClient.Operation.OperationRequest)
                .closeAndOpenBlock("} else {")
                .write("next.call(req)")
                .closeBlock("}")
        }
    }
}
