/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.CodegenContext
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.core.RuntimeTypes
import aws.smithy.kotlin.codegen.core.withBlock
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.lang.KotlinTypes
import aws.smithy.kotlin.codegen.model.*
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import aws.smithy.kotlin.codegen.rendering.util.ConfigProperty
import aws.smithy.kotlin.codegen.rendering.util.ConfigPropertyType
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.traits.HttpTrait

private const val CONTINUE_PROP = "continueHeaderThresholdBytes"

private val enableContinueProp = ConfigProperty {
    name = CONTINUE_PROP
    symbol = KotlinTypes.Long.asNullable()
    documentation = """
        The minimum content length threshold (in bytes) for which to send `Expect: 100-continue` HTTP headers. PUT
        requests with bodies at or above this length will include this header, as will PUT requests with a null content
        length. Defaults to 2 megabytes.
        
        This property may be set to `null` to disable sending the header regardless of content length.
    """.trimIndent()

    // Need a custom property type because property is nullable but has a non-null default
    propertyType = ConfigPropertyType.Custom(
        render = { _, writer ->
            writer.write("public val $CONTINUE_PROP: Long? = builder.$CONTINUE_PROP")
        },
        renderBuilder = { prop, writer ->
            prop.documentation?.let(writer::dokka)
            writer.write("public var $CONTINUE_PROP: Long? = 2 * 1024 * 1024 // 2MB")
        },
    )
}

class ContinueIntegration : KotlinIntegration {
    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> = listOf(
        enableContinueProp,
    )

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + ContinueMiddleware

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = model.expectShape<ServiceShape>(settings.service).isS3
}

internal object ContinueMiddleware : ProtocolMiddleware {
    override val name: String = "ContinueHeader"

    override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean = op.getTrait<HttpTrait>()?.method == "PUT"

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        writer.withBlock("config.$CONTINUE_PROP?.let { threshold ->", "}") {
            writer.write("op.interceptors.add(#T(threshold))", RuntimeTypes.HttpClient.Interceptors.ContinueInterceptor)
        }
    }
}
