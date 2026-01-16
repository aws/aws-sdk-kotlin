/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization.glacier

import aws.sdk.kotlin.codegen.sdkId
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.core.RuntimeTypes
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.buildSymbol
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.model.isStreaming
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import aws.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.StructureShape

public class GlacierBodyChecksum : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).sdkId.equals("Glacier", ignoreCase = true)

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + glacierBodyChecksumMiddleware

    private val glacierBodyChecksumMiddleware = object : ProtocolMiddleware {
        override val order: Byte = 127 // Must come after AwsSignatureVersion4
        override val name: String = "GlacierBodyChecksum"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean {
            val input = op.input.getOrNull()?.let { ctx.model.expectShape<StructureShape>(it) }
            return input?.members()?.any { it.isStreaming || ctx.model.expectShape(it.target).isStreaming } == true
        }

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            writer.addImport(RuntimeTypes.Core.Hashing.Sha256)
            val middleware = glacierSymbol("GlacierBodyChecksum")
            writer.addImport(middleware)
            writer.write("op.install(#T())", middleware)
        }

        private fun glacierSymbol(name: String) = buildSymbol {
            this.name = name
            namespace = "aws.sdk.kotlin.services.glacier.internal"
        }
    }
}
