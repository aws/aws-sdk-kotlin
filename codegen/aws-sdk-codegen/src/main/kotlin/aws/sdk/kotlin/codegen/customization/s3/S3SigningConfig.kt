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
 * Set default signing attributes in the execution context (which ultimately becomes the signing context) for S3.
 */
class S3SigningConfig : KotlinIntegration {
    override val order: Byte
        get() = 127

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        val service = model.expectShape<ServiceShape>(settings.service)
        return (service.isS3 || service.isS3Control)
    }

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(HttpProtocolClientGenerator.MergeServiceDefaults, renderDefaultSigningContext),
        )

    private val renderDefaultSigningContext = AppendingSectionWriter { writer ->
        val signingAttrs = RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigningAttributes
        // https://github.com/awslabs/aws-sdk-kotlin/issues/200
        writer.putIfAbsent(signingAttrs, "NormalizeUriPath", "false")
        writer.putIfAbsent(signingAttrs, "UseDoubleUriEncode", "false")
        writer.putIfAbsent(signingAttrs, "SignedBodyHeader", writer.format("#T.X_AMZ_CONTENT_SHA256", RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSignedBodyHeader))
        writer.putIfAbsent(signingAttrs, "EnableAwsChunked", "config.enableAwsChunked")
    }
}
