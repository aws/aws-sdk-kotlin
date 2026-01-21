/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.ServiceClientCompanionObjectWriter
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.CodegenContext
import aws.smithy.kotlin.codegen.integration.AppendingSectionWriter
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.integration.SectionWriterBinding
import aws.smithy.kotlin.codegen.lang.KotlinTypes
import aws.smithy.kotlin.codegen.model.buildSymbol
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.rendering.util.ConfigProperty
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.transform.ModelTransformer
import software.amazon.smithy.rulesengine.traits.ClientContextParamsTrait

/**
 * Integration to inject s3-related client config builtins for endpoint resolution & multi-region access points in place of the corresponding client
 * context params.
 */
class ClientConfigIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = model.expectShape<ServiceShape>(settings.service).isS3

    companion object {
        val EnableAccelerateProp: ConfigProperty = ConfigProperty {
            name = "enableAccelerate"
            useSymbolWithNullableBuilder(KotlinTypes.Boolean, "false")
            documentation = """
                Flag to support [S3 transfer acceleration](https://docs.aws.amazon.com/AmazonS3/latest/userguide/transfer-acceleration.html)
                with this client.
            """.trimIndent()
        }

        val ForcePathStyleProp: ConfigProperty = ConfigProperty {
            name = "forcePathStyle"
            useSymbolWithNullableBuilder(KotlinTypes.Boolean, "false")
            documentation = """
                Flag to use legacy path-style addressing when making requests.
            """.trimIndent()
        }

        val UseArnRegionProp: ConfigProperty = ConfigProperty {
            name = "useArnRegion"
            useSymbolWithNullableBuilder(KotlinTypes.Boolean, "false")
            documentation = """
                Flag to enforce using a bucket arn with a region matching the client config when making requests with
                [S3 access points](https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-points.html).
            """.trimIndent()
        }

        val DisableMrapProp: ConfigProperty = ConfigProperty {
            name = "disableMrap"
            useSymbolWithNullableBuilder(KotlinTypes.Boolean, "false")
            documentation = """
                Flag to disable [S3 multi-region access points](https://docs.aws.amazon.com/AmazonS3/latest/userguide/MultiRegionAccessPoints.html).
            """.trimIndent()
        }

        val EnableAwsChunked: ConfigProperty = ConfigProperty {
            name = "enableAwsChunked"
            useSymbolWithNullableBuilder(KotlinTypes.Boolean, "true")
            documentation = "Flag to enable [aws-chunked](https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-streaming.html) content encoding."
        }
    }

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val transformer = ModelTransformer.create()
        return transformer.mapTraits(model) { _, trait ->
            if (trait is ClientContextParamsTrait) {
                ClientContextParamsTrait.builder()
                    .parameters(trait.parameters)
                    .removeParameter("ForcePathStyle")
                    .removeParameter("UseArnRegion")
                    .removeParameter("DisableMultiRegionAccessPoints")
                    .removeParameter("Accelerate")
                    .build()
            } else {
                trait
            }
        }
    }

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> = listOf(
        EnableAccelerateProp,
        ForcePathStyleProp,
        UseArnRegionProp,
        DisableMrapProp,
        EnableAwsChunked,
    )

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(
                ServiceClientCompanionObjectWriter.FinalizeEnvironmentalConfig,
                finalizeS3ConfigWriter,
            ),
        )

    // add S3-specific config finalization
    private val finalizeS3ConfigWriter = AppendingSectionWriter { writer ->
        val finalizeS3Config = buildSymbol {
            name = "finalizeS3Config"
            namespace = "aws.sdk.kotlin.services.s3.internal"
        }
        writer.write("#T(builder, sharedConfig)", finalizeS3Config)
    }
}
