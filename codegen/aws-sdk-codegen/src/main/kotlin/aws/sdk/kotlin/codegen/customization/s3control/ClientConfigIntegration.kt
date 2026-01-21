/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3control

import aws.sdk.kotlin.codegen.customization.s3.isS3Control
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.CodegenContext
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.lang.KotlinTypes
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.rendering.util.ConfigProperty
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.transform.ModelTransformer
import software.amazon.smithy.rulesengine.traits.ClientContextParamsTrait

/**
 * Integration to inject s3control-related client config builtins for endpoint resolution in place of the corresponding
 * client context params.
 */
class ClientConfigIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = model.expectShape<ServiceShape>(settings.service).isS3Control

    companion object {
        val UseArnRegionProp: ConfigProperty = ConfigProperty.Boolean(
            "useArnRegion",
            defaultValue = false,
            documentation = """
            Flag to enforce using a bucket arn with a region matching the client config when making requests with
            [S3 access points](https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-points.html).
            """.trimIndent(),
        )

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
                    .removeParameter("UseArnRegion")
                    .build()
            } else {
                trait
            }
        }
    }

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> = listOf(
        UseArnRegionProp,
        EnableAwsChunked,
    )
}
