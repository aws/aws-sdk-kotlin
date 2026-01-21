/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization.machinelearning

import aws.sdk.kotlin.codegen.sdkId
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.buildSymbol
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.rendering.protocol.*
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape

class MachineLearningEndpointCustomization : KotlinIntegration {
    // the default endpoint resolver middleware will still execute first, we just
    // need to ensure that the custom resolver runs _after_ the default (i.e. `modifyBeforeSigning`)
    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + endpointResolverMiddleware

    private val endpointResolverMiddleware = object : ProtocolMiddleware {
        override val name: String = "ResolvePredictEndpoint"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean = op.id.name == "Predict"

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            val symbol = machineLearningSymbol("ResolvePredictEndpoint")
            writer.write("op.interceptors.add(#T())", symbol)
        }

        private fun machineLearningSymbol(name: String) = buildSymbol {
            this.name = name
            namespace = "aws.sdk.kotlin.services.machinelearning.internal"
        }
    }

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = model.expectShape<ServiceShape>(settings.service).sdkId.equals("Machine Learning", ignoreCase = true)
}
