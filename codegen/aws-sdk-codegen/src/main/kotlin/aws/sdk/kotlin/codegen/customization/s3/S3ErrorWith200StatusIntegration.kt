/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.buildSymbol
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.model.isStreaming
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Register interceptor to handle S3 error responses returned with an HTTP 200 status code.
 * see [aws-sdk-kotlin#199](https://github.com/awslabs/aws-sdk-kotlin/issues/199)
 */
class S3ErrorWith200StatusIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).isS3

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + listOf(S3HandleError200ResponseMiddleware)
}

private object S3HandleError200ResponseMiddleware : ProtocolMiddleware {
    override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean {
        // we don't know for sure what operations S3 does this on. Go customized this for only a select few
        // like CopyObject/UploadPartCopy/CompleteMultipartUpload but Rust hit it on additional operations
        // (DeleteObjects).
        // Instead of playing whack-a-mole broadly apply this interceptor to everything but streaming responses
        // which adds a small amount of overhead to response processing.
        val output = ctx.model.expectShape(op.output.get())
        return output.members().none {
            val isBlob = it.isBlobShape || ctx.model.expectShape(it.target).isBlobShape
            val isStreaming = it.isStreaming || ctx.model.expectShape(it.target).isStreaming
            isBlob && isStreaming
        }
    }

    override val name: String = "Handle200ErrorsInterceptor"
    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        val symbol = buildSymbol {
            name = this@S3HandleError200ResponseMiddleware.name
            namespace = ctx.settings.pkg.subpackage("internal")
        }

        writer.write("op.interceptors.add(#T)", symbol)
    }
}
