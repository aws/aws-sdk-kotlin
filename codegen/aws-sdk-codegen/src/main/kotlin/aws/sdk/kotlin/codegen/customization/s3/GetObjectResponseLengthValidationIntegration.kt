/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.core.RuntimeTypes
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape

class GetObjectResponseLengthValidationIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).isS3

    override fun customizeMiddleware(ctx: ProtocolGenerator.GenerationContext, resolved: List<ProtocolMiddleware>) =
        resolved + responseLengthValidationMiddleware
}

internal val responseLengthValidationMiddleware = object : ProtocolMiddleware {
    override val name: String = "ResponseLengthValidationMiddleware"

    override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape) = op.id.name == "GetObject"

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        val interceptorSymbol = RuntimeTypes.HttpClient.Interceptors.ResponseLengthValidationInterceptor
        writer.write("op.interceptors.add(#T())", interceptorSymbol)
    }
}
