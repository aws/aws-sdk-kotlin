/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.businessmetrics

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import software.amazon.smithy.aws.traits.auth.SigV4ATrait
import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.ServiceIndex
import software.amazon.smithy.model.shapes.OperationShape

/**
 * Renders the addition of some of the credentials related business metrics.
 */
class CredentialsBusinessMetricsIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        val serviceIndex = ServiceIndex.of(model)
        val schemes = serviceIndex.getAuthSchemes(settings.service)

        return schemes.values.any {
            it.javaClass == SigV4ATrait::class.java || it.javaClass == SigV4Trait::class.java
        }
    }

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + credentialsBusinessMetricsMiddleware

    private val credentialsBusinessMetricsMiddleware = object : ProtocolMiddleware {
        override val name: String = "credentialsOverrideBusinessMetricsMiddleware"
        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            writer.withBlock(
                "if (config.credentialsProvider is #T) {",
                "}",
                AwsRuntimeTypes.Config.Credentials.StaticCredentialsProvider,
            ) {
                write(
                    "op.context.#T(#T.Credentials.CREDENTIALS_CODE)",
                    RuntimeTypes.Core.BusinessMetrics.emitBusinessMetric,
                    AwsRuntimeTypes.Http.Interceptors.BusinessMetrics.AwsBusinessMetric,
                )
            }
        }
    }
}
