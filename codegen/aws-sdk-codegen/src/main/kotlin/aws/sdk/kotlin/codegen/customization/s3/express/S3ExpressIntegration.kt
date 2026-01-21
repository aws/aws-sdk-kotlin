/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3.express

import SigV4S3ExpressAuthTrait
import aws.sdk.kotlin.codegen.customization.s3.isS3
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.*
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.lang.KotlinTypes
import aws.smithy.kotlin.codegen.model.*
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import aws.smithy.kotlin.codegen.rendering.util.ConfigProperty
import aws.smithy.kotlin.codegen.rendering.util.ConfigPropertyType
import software.amazon.smithy.aws.traits.HttpChecksumTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.*
import software.amazon.smithy.model.transform.ModelTransformer

/**
 * An integration which handles codegen for S3 Express, such as:
 * 1. Configure auth scheme by applying a synthetic shape and trait
 * 2. Add ExpressClient and Bucket to execution context
 * 3. Configuring the default checksum algorithm
 */
class S3ExpressIntegration : KotlinIntegration {
    companion object {
        val DisableExpressSessionAuth: ConfigProperty = ConfigProperty {
            name = "disableS3ExpressSessionAuth"
            useSymbolWithNullableBuilder(KotlinTypes.Boolean, "false")
            documentation = """
                Flag to disable S3 Express One Zone's bucket-level session authentication method.  
            """.trimIndent()
        }

        val ExpressCredentialsProvider: ConfigProperty = ConfigProperty {
            name = "expressCredentialsProvider"
            symbol = RuntimeTypes.Auth.Credentials.AwsCredentials.CredentialsProvider
            documentation = """
                Credentials provider to be used for making requests to S3 Express.   
            """.trimIndent()

            propertyType = ConfigPropertyType.Custom(
                render = { _, writer ->
                    writer.write(
                        "public val #1L: #2T = builder.#1L ?: #3T()",
                        name,
                        symbol,
                        buildSymbol {
                            name = "DefaultS3ExpressCredentialsProvider"
                            namespace = "aws.sdk.kotlin.services.s3.express"
                        },
                    )
                },
                renderBuilder = { prop, writer ->
                    prop.documentation?.let(writer::dokka)
                    writer.write("public var #L: #T? = null", name, symbol)
                },
            )
        }
    }

    override fun enabledForService(model: Model, settings: KotlinSettings) = model.expectShape<ServiceShape>(settings.service).isS3

    /**
     * Add a synthetic SigV4 S3 Express auth trait and shape
     */
    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val transformer = ModelTransformer.create()

        // AuthIndex.getAuthSchemes looks for shapes with an AuthDefinitionTrait, so need to make one for SigV4 S3Express
        val authDefinitionTrait = AuthDefinitionTrait.builder().addTrait(SigV4S3ExpressAuthTrait.ID).build()
        val sigV4S3ExpressAuthShape = StructureShape.builder()
            .addTrait(authDefinitionTrait)
            .id(SigV4S3ExpressAuthTrait.ID)
            .build()

        val serviceShape = settings.getService(model)
        val serviceShapeBuilder = serviceShape.toBuilder()

        serviceShapeBuilder.addTrait(SigV4S3ExpressAuthTrait())

        val authTrait = AuthTrait(serviceShape.expectTrait(AuthTrait::class.java).valueSet + mutableSetOf(SigV4S3ExpressAuthTrait.ID))
        serviceShapeBuilder.addTrait(authTrait)

        // Add the new shape and update the service shape's AuthTrait
        return transformer.replaceShapes(model, listOf(sigV4S3ExpressAuthShape, serviceShapeBuilder.build()))
    }

    override fun customizeMiddleware(ctx: ProtocolGenerator.GenerationContext, resolved: List<ProtocolMiddleware>) = resolved + listOf(
        addClientToExecutionContext,
        addBucketToExecutionContext,
        s3ExpressDefaultChecksumAlgorithm,
    )

    private val s3AttributesSymbol = buildSymbol {
        name = "S3Attributes"
        namespace = "aws.sdk.kotlin.services.s3"
    }

    private val addClientToExecutionContext = object : ProtocolMiddleware {
        override val name: String = "AddClientToExecutionContext"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean = ctx.model.expectShape<ServiceShape>(ctx.settings.service).isS3

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            writer.write("op.context[#T.ExpressClient] = this", s3AttributesSymbol)
        }
    }

    private val addBucketToExecutionContext = object : ProtocolMiddleware {
        override val name: String = "AddBucketToExecutionContext"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean = ctx.model.expectShape<StructureShape>(op.input.get())
            .members()
            .any { it.memberName == "Bucket" }

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            writer.write("input.bucket?.let { op.context[#T.Bucket] = it }", s3AttributesSymbol)
        }
    }

    /**
     * Re-configures the default checksum algorithm for S3 Express.
     */
    private val s3ExpressDefaultChecksumAlgorithm = object : ProtocolMiddleware {
        override val name: String = "s3ExpressDefaultChecksumAlgorithm"
        override val order: Byte = -1 // After setting the modeled default (-2) and before calculating the checksum (0)

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean = op.hasTrait<HttpChecksumTrait>() || op.hasTrait<HttpChecksumRequiredTrait>()

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            writer.write(
                "op.interceptors.add(#T(#L))",
                buildSymbol {
                    namespace = "aws.sdk.kotlin.services.s3.express"
                    name = "S3ExpressDefaultChecksumAlgorithm"
                },
                op.isS3UploadPart,
            )
        }
    }

    private val OperationShape.isS3UploadPart: Boolean get() = id.name == "UploadPart"

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> = listOf(
        DisableExpressSessionAuth,
        ExpressCredentialsProvider,
    )
}
