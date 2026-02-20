/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.businessmetrics

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.shapes.OperationShape

/**
 * Renders the addition of the [BusinessMetricsInterceptor]
 */
class BusinessMetricsInterceptorIntegration : KotlinIntegration {
    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + userAgentBusinessMetricsMiddleware

    private val userAgentBusinessMetricsMiddleware = object : ProtocolMiddleware {
        override val name: String = "UserAgentBusinessMetrics"
        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            writer.write(
                "op.interceptors.add(#T())",
                AwsRuntimeTypes.Http.Interceptors.BusinessMetrics.BusinessMetricsInterceptor,
            )
        }
    }
}
