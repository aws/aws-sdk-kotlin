/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen

import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.shapes.OperationShape

/**
 * Adds AWS specific retry headers
 */
class AwsRetryHeaderIntegration : KotlinIntegration {
    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + middleware

    private val middleware = object : ProtocolMiddleware {
        override val name: String = AwsRuntimeTypes.Http.Middleware.AwsRetryHeaderMiddleware.name

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            writer.write(
                "op.install(#T())",
                AwsRuntimeTypes.Http.Middleware.AwsRetryHeaderMiddleware,
            )
        }
    }
}
