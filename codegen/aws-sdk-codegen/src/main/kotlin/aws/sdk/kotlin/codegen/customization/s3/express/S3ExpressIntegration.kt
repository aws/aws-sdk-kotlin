/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3.express

import SigV4S3ExpressAuthTrait
import aws.sdk.kotlin.codegen.customization.s3.isS3
import software.amazon.smithy.aws.traits.HttpChecksumTrait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.*
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigProperty
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigPropertyType
import software.amazon.smithy.kotlin.codegen.utils.dq
import software.amazon.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.*
import software.amazon.smithy.model.transform.ModelTransformer

/**
 * An integration which handles codegen for S3 Express, such as:
 * 1. Configure auth scheme by applying a synthetic shape and trait
 * 2. Add ExpressClient and Bucket to execution context
 * 3. Override checksums to use CRC32 instead of MD5
 * 4. Disable all checksums for s3:UploadPart
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

    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).isS3

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

    override fun customizeMiddleware(ctx: ProtocolGenerator.GenerationContext, resolved: List<ProtocolMiddleware>) =
        resolved + listOf(
            AddClientToExecutionContext,
            AddBucketToExecutionContext,
            UseCrc32Checksum,
            UploadPartDisableChecksum,
        )

    private val S3AttributesSymbol = buildSymbol {
        name = "S3Attributes"
        namespace = "aws.sdk.kotlin.services.s3"
    }

    private val AddClientToExecutionContext = object : ProtocolMiddleware {
        override val name: String = "AddClientToExecutionContext"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
            ctx.model.expectShape<ServiceShape>(ctx.settings.service).isS3

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            writer.write("op.context[#T.ExpressClient] = this", S3AttributesSymbol)
        }
    }

    private val AddBucketToExecutionContext = object : ProtocolMiddleware {
        override val name: String = "AddBucketToExecutionContext"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
            ctx.model.expectShape<StructureShape>(op.input.get())
                .members()
                .any { it.memberName == "Bucket" }

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            writer.write("input.bucket?.let { op.context[#T.Bucket] = it }", S3AttributesSymbol)
        }
    }

    /**
     * For any operations that require a checksum, set CRC32 if the user has not already configured a checksum.
     */
    private val UseCrc32Checksum = object : ProtocolMiddleware {
        override val name: String = "UseCrc32Checksum"

        override val order: Byte = -1 // Render before flexible checksums

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean = !op.isS3UploadPart &&
            (op.hasTrait<HttpChecksumRequiredTrait>() || (op.hasTrait<HttpChecksumTrait>() && op.expectTrait<HttpChecksumTrait>().isRequestChecksumRequired))

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            val interceptorSymbol = buildSymbol {
                namespace = "aws.sdk.kotlin.services.s3.express"
                name = "S3ExpressCrc32ChecksumInterceptor"
            }

            val httpChecksumTrait = op.getTrait<HttpChecksumTrait>()

            val checksumAlgorithmMember = ctx.model.expectShape<StructureShape>(op.input.get())
                .members()
                .firstOrNull { it.memberName == httpChecksumTrait?.requestAlgorithmMember?.getOrNull() }

            // S3 models a header name x-amz-sdk-checksum-algorithm representing the name of the checksum algorithm used
            val checksumHeaderName = checksumAlgorithmMember?.getTrait<HttpHeaderTrait>()?.value

            writer.write("op.interceptors.add(#T(${checksumHeaderName?.dq() ?: ""}))", interceptorSymbol)
        }
    }

    /**
     * Disable all checksums for s3:UploadPart
     */
    private val UploadPartDisableChecksum = object : ProtocolMiddleware {
        override val name: String = "UploadPartDisableChecksum"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
            op.isS3UploadPart

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            val interceptorSymbol = buildSymbol {
                namespace = "aws.sdk.kotlin.services.s3.express"
                name = "S3ExpressDisableChecksumInterceptor"
            }
            writer.addImport(interceptorSymbol)
            writer.write("op.interceptors.add(#T())", interceptorSymbol)
        }
    }

    private val OperationShape.isS3UploadPart: Boolean get() = id.name == "UploadPart"

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> = listOf(
        DisableExpressSessionAuth,
        ExpressCredentialsProvider,
    )
}
