/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3.express

import aws.sdk.kotlin.codegen.customization.s3.isS3
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.*
import aws.smithy.kotlin.codegen.integration.AppendingSectionWriter
import aws.smithy.kotlin.codegen.integration.AuthSchemeHandler
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.integration.SectionWriterBinding
import aws.smithy.kotlin.codegen.model.buildSymbol
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.model.hasTrait
import aws.smithy.kotlin.codegen.model.knowledge.AwsSignatureVersion4
import aws.smithy.kotlin.codegen.rendering.endpoints.EndpointCustomization
import aws.smithy.kotlin.codegen.rendering.endpoints.EndpointPropertyRenderer
import aws.smithy.kotlin.codegen.rendering.endpoints.ExpressionRenderer
import aws.smithy.kotlin.codegen.rendering.protocol.*
import aws.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.aws.traits.auth.UnsignedPayloadTrait
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.codegen.core.SymbolReference
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression
import java.util.*

/**
 * Register support for the `sigv4-s3express` auth scheme.
 */
class SigV4S3ExpressAuthSchemeIntegration : KotlinIntegration {
    // Needs to run after `SigV4AuthSchemeIntegration`
    override val order: Byte = -51

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = model.expectShape<ServiceShape>(settings.service).isS3

    override fun authSchemes(ctx: ProtocolGenerator.GenerationContext): List<AuthSchemeHandler> = listOf(SigV4S3ExpressAuthSchemeHandler())

    override fun customizeEndpointResolution(ctx: ProtocolGenerator.GenerationContext): EndpointCustomization = SigV4S3ExpressEndpointCustomization

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(SectionWriterBinding(HttpProtocolClientGenerator.ClientInitializer, renderClientInitializer))

    // add S3 Express credentials provider to managed resources in the service client initializer
    private val renderClientInitializer = AppendingSectionWriter { writer ->
        writer.write("managedResources.#T(config.expressCredentialsProvider)", RuntimeTypes.Core.IO.addIfManaged)
    }
}

internal val sigV4S3ExpressSymbol = buildSymbol {
    name = "sigV4S3Express"
    namespace = "aws.sdk.kotlin.services.s3.express"
}

internal val SigV4S3ExpressAuthSchemeSymbol = buildSymbol {
    name = "SigV4S3ExpressAuthScheme"
    namespace = "aws.sdk.kotlin.services.s3.express"
}

private object SigV4S3ExpressEndpointCustomization : EndpointCustomization {
    override val propertyRenderers: Map<String, EndpointPropertyRenderer> = mapOf(
        "authSchemes" to ::renderAuthScheme,
    )
}

class SigV4S3ExpressAuthSchemeHandler : AuthSchemeHandler {
    override val authSchemeId: ShapeId = ShapeId.from("aws.auth#sigv4s3express")

    override val authSchemeIdSymbol: Symbol = buildSymbol {
        name = "AuthSchemeId(\"aws.auth#sigv4s3express\")"
        val ref = RuntimeTypes.Auth.Identity.AuthSchemeId
        objectRef = ref
        namespace = ref.namespace
        reference(ref, SymbolReference.ContextOption.USE)
    }

    override fun identityProviderAdapterExpression(writer: KotlinWriter) {
        writer.write("config.#L", S3ExpressIntegration.ExpressCredentialsProvider.propertyName)
    }

    override fun authSchemeProviderInstantiateAuthOptionExpr(
        ctx: ProtocolGenerator.GenerationContext,
        op: OperationShape?,
        writer: KotlinWriter,
    ) {
        val expr = if (op?.hasTrait<UnsignedPayloadTrait>() == true) {
            "#T(unsignedPayload = true)"
        } else {
            "#T()"
        }
        writer.write(expr, sigV4S3ExpressSymbol)
    }

    override fun instantiateAuthSchemeExpr(ctx: ProtocolGenerator.GenerationContext, writer: KotlinWriter) {
        val signingService = AwsSignatureVersion4.signingServiceName(ctx.service)
        writer.write("#T(#T, #S)", SigV4S3ExpressAuthSchemeSymbol, RuntimeTypes.Auth.Signing.AwsSigningStandard.DefaultAwsSigner, signingService)
    }
}

private fun renderAuthScheme(writer: KotlinWriter, authSchemes: Expression, expressionRenderer: ExpressionRenderer) {
    val expressScheme = authSchemes.toNode().expectArrayNode().find {
        it.expectObjectNode().expectStringMember("name").value == "sigv4-s3express"
    }?.expectObjectNode()

    expressScheme?.let {
        writer.writeInline("#T to ", RuntimeTypes.SmithyClient.Endpoints.SigningContextAttributeKey)
        writer.withBlock("listOf(", ")") {
            withBlock("#T(", "),", sigV4S3ExpressSymbol) {
                // we delegate back to the expression visitor for each of these fields because it's possible to
                // encounter template strings throughout

                writeInline("serviceName = ")
                renderOrElse(expressionRenderer, expressScheme.getStringMember("signingName"), "null")

                writeInline("disableDoubleUriEncode = ")
                renderOrElse(expressionRenderer, expressScheme.getBooleanMember("disableDoubleEncoding"), "false")

                writeInline("signingRegion = ")
                renderOrElse(expressionRenderer, expressScheme.getStringMember("signingRegion"), "null")
            }
        }
    }
}

private fun KotlinWriter.renderOrElse(
    expressionRenderer: ExpressionRenderer,
    optionalNode: Optional<out Node>,
    whenNullValue: String,
) {
    val nullableNode = optionalNode.getOrNull()
    when (nullableNode) {
        null -> writeInline(whenNullValue)
        else -> expressionRenderer.renderExpression(Expression.fromNode(nullableNode))
    }
    write(",")
}
