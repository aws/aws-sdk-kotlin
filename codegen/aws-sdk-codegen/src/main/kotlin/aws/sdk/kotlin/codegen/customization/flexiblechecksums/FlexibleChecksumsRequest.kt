/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.flexiblechecksums

import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.CodegenContext
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.core.RuntimeTypes
import aws.smithy.kotlin.codegen.core.withBlock
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.*
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import aws.smithy.kotlin.codegen.rendering.util.ConfigProperty
import aws.smithy.kotlin.codegen.rendering.util.ConfigPropertyType
import aws.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.aws.traits.HttpChecksumTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.StructureShape

/**
 * Handles flexible checksum requests
 */
class FlexibleChecksumsRequest : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.isTraitApplied(HttpChecksumTrait::class.java)

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> =
        listOf(
            // Allows flexible checksum request configuration
            ConfigProperty {
                name = "requestChecksumCalculation"
                symbol = RuntimeTypes.SmithyClient.Config.RequestHttpChecksumConfig
                baseClass = RuntimeTypes.SmithyClient.Config.HttpChecksumConfig
                useNestedBuilderBaseClass()
                documentation = "Configures request checksum calculation"
                propertyType = ConfigPropertyType.RequiredWithDefault("RequestHttpChecksumConfig.WHEN_SUPPORTED")
            },
        )

    override fun customizeMiddleware(ctx: ProtocolGenerator.GenerationContext, resolved: List<ProtocolMiddleware>) =
        resolved + requestChecksumCalculationBusinessMetric + httpChecksumDefaultAlgorithmMiddleware + flexibleChecksumsRequestMiddleware
}

/**
 * Emits business metric based on `requestChecksumCalculation` client config
 */
private val requestChecksumCalculationBusinessMetric = object : ProtocolMiddleware {
    override val name: String = "requestChecksumCalculationBusinessMetric"

    override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
        op.hasTrait<HttpChecksumTrait>()

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        writer.withBlock("when(config.requestChecksumCalculation) {", "}") {
            // Supported
            writer.write(
                "#T.WHEN_SUPPORTED -> op.context.#T(#T.FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED)",
                RuntimeTypes.SmithyClient.Config.RequestHttpChecksumConfig,
                RuntimeTypes.Core.BusinessMetrics.emitBusinessMetric,
                RuntimeTypes.Core.BusinessMetrics.SmithyBusinessMetric,
            )
            // Required
            writer.write(
                "#T.WHEN_REQUIRED -> op.context.#T(#T.FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED)",
                RuntimeTypes.SmithyClient.Config.RequestHttpChecksumConfig,
                RuntimeTypes.Core.BusinessMetrics.emitBusinessMetric,
                RuntimeTypes.Core.BusinessMetrics.SmithyBusinessMetric,
            )
        }
    }
}

/**
 * Adds default checksum algorithm to the execution context
 */
private val httpChecksumDefaultAlgorithmMiddleware = object : ProtocolMiddleware {
    override val name: String = "httpChecksumDefaultAlgorithmMiddleware"
    override val order: Byte = -2 // Before S3 Express (possibly) changes the default (-1) and before calculating checksum (0)

    override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
        op.hasRequestAlgorithmMember(ctx)

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        writer.write(
            "op.context[#T.DefaultChecksumAlgorithm] = #S",
            RuntimeTypes.HttpClient.Operation.HttpOperationContext,
            "CRC32",
        )
    }
}

/**
 * Adds interceptor to handle flexible checksum request calculation
 */
private val flexibleChecksumsRequestMiddleware = object : ProtocolMiddleware {
    override val name: String = "flexibleChecksumsRequestMiddleware"

    override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
        op.hasRequestAlgorithmMember(ctx)

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        val httpChecksumTrait = op.getTrait<HttpChecksumTrait>()!!
        val requestChecksumRequired = httpChecksumTrait.isRequestChecksumRequired
        val requestAlgorithmMember = ctx.model.expectShape<StructureShape>(op.input.get())
            .members()
            .first { it.memberName == httpChecksumTrait.requestAlgorithmMember.get() }
        val requestAlgorithmMemberName = ctx.symbolProvider.toMemberName(requestAlgorithmMember)

        writer.withBlock(
            "op.interceptors.add(#T(",
            "))",
            RuntimeTypes.HttpClient.Interceptors.FlexibleChecksumsRequestInterceptor,
        ) {
            writer.write("#L,", requestChecksumRequired)
            writer.write("config.requestChecksumCalculation,")
            writer.write("input.#L?.value,", requestAlgorithmMemberName)
        }
    }
}

/**
 * Determines if an operation is set up to send flexible request checksums
 */
private fun OperationShape.hasRequestAlgorithmMember(ctx: ProtocolGenerator.GenerationContext): Boolean {
    val httpChecksumTrait = this.getTrait<HttpChecksumTrait>()
    val inputShape = this.input.getOrNull()?.let { ctx.model.expectShape<StructureShape>(it) }

    return (
        (httpChecksumTrait != null) &&
            (httpChecksumTrait.requestAlgorithmMember?.getOrNull() != null) &&
            (inputShape?.memberNames?.any { it == httpChecksumTrait.requestAlgorithmMember.get() } == true)
        )
}
