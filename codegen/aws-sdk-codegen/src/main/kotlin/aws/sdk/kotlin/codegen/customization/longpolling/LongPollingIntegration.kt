/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.longpolling

import aws.sdk.kotlin.codegen.sdkId
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.core.RuntimeTypes
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Hard-coded long-polling operations per New Retry Behavior.
 * These operations always back off when retryable, even if the retry quota is exhausted.
 */
private val LONG_POLLING_OPERATIONS: Map<String, Set<String>> = mapOf(
    "sqs" to setOf("ReceiveMessage"),
    "sfn" to setOf("GetActivityTask"),
    "swf" to setOf("PollForActivityTask", "PollForDecisionTask"),
)

class LongPollingIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape(settings.service, ServiceShape::class.java).sdkId.lowercase() in LONG_POLLING_OPERATIONS

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> {
        val sdkId = ctx.model.expectShape(ctx.settings.service, ServiceShape::class.java).sdkId.lowercase()
        val ops = LONG_POLLING_OPERATIONS[sdkId] ?: return resolved
        return resolved + LongPollingMiddleware(ops)
    }
}

private class LongPollingMiddleware(private val operationNames: Set<String>) : ProtocolMiddleware {
    override val name: String = "LongPolling"

    override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
        op.id.name in operationNames

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        writer.write(
            "op.context[#T.LongPolling] = true",
            RuntimeTypes.HttpClient.Operation.HttpOperationContext,
        )
    }
}
