/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization.s3

import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.RuntimeTypes
import aws.smithy.kotlin.codegen.integration.AppendingSectionWriter
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.integration.SectionWriterBinding
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.rendering.protocol.HttpProtocolClientGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.putIfAbsent
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Wires the S3-only `payloadSigningEnabled` client config property (see [ClientConfigIntegration]) into the default
 * signing context. This is kept separate from [S3SigningConfig] because that integration also applies to S3 Control,
 * which does not expose the `payloadSigningEnabled` config property.
 */
class S3PayloadSigningConfig : KotlinIntegration {
    override val order: Byte
        get() = 127

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = model.expectShape<ServiceShape>(settings.service).isS3

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(HttpProtocolClientGenerator.MergeServiceDefaults, renderPayloadSigningContext),
        )

    private val renderPayloadSigningContext = AppendingSectionWriter { writer ->
        val signingAttrs = RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigningAttributes
        writer.putIfAbsent(signingAttrs, "PayloadSigningEnabled", "config.payloadSigningEnabled")
    }
}
