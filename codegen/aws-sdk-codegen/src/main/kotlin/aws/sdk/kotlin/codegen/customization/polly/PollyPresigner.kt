/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.polly

import aws.sdk.kotlin.codegen.PresignerGenerator
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.*
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.integration.SectionWriter
import aws.smithy.kotlin.codegen.integration.SectionWriterBinding
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.rendering.protocol.HttpStringValuesMapSerializer
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.HttpBinding
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Override the presigner generation for Polly based on customization SEP by lifting HTTP headers/body into URL query
 * parameters and setting the request method to `GET` (vs `POST`).
 */
class PollyPresigner : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).id.toString() == "com.amazonaws.polly#Parrot_v1"

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(PresignerGenerator.UnsignedRequestCustomizationSection, customizeUnsignedRequest),
            SectionWriterBinding(PresignerGenerator.SigningConfigCustomizationSection, customizeAwsSigningConfig),
        )

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        delegator.runtimeDependencies.addAll(KotlinDependency.KOTLIN_TEST.dependencies)
    }

    private val customizeUnsignedRequest = SectionWriter { writer, _ ->
        val ctx = writer.getContextValue(CodegenContext.Key)
        val operation = ctx.model.expectShape<OperationShape>(writer.getContextValue(PresignerGenerator.UnsignedRequestCustomizationSection.OperationId))
        val resolver = writer.getContextValue(PresignerGenerator.UnsignedRequestCustomizationSection.HttpBindingResolver)
        val defaultTimestampFormat = writer.getContextValue(PresignerGenerator.UnsignedRequestCustomizationSection.DefaultTimestampFormat)

        writer.write("unsignedRequest.method = #T.GET", RuntimeTypes.Http.HttpMethod)
        writer.withBlock("unsignedRequest.url.parameters.decodedParameters(#T.SmithyLabel) {", "}", RuntimeTypes.Core.Text.Encoding.PercentEncoding) {
            val bindings = resolver
                .requestBindings(operation)
                .map { it.copy(location = HttpBinding.Location.QUERY_PARAMS) }

            HttpStringValuesMapSerializer(ctx.model, ctx.symbolProvider, ctx.settings, bindings, resolver, defaultTimestampFormat).render(writer)
        }

        // Remove the headers that were created by the default HTTP operation serializer
        writer.write("unsignedRequest.headers.clear()")
        writer.write("")
    }

    private val customizeAwsSigningConfig = SectionWriter { writer, _ ->
        writer.write(
            "hashSpecification = #T.CalculateFromPayload",
            RuntimeTypes.Auth.Signing.AwsSigningCommon.HashSpecification,
        )
    }
}
