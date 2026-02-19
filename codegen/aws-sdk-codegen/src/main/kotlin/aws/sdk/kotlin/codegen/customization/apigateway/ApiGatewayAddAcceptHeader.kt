/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization.apigateway

import aws.sdk.kotlin.codegen.sdkId
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.rendering.protocol.MutateHeadersMiddleware
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Adds a middleware that sets the "Accept" header to "application/json" for all requests
 */
class ApiGatewayAddAcceptHeader : KotlinIntegration {

    override fun enabledForService(model: Model, settings: KotlinSettings) = model.expectShape<ServiceShape>(settings.service).sdkId.equals("API Gateway", ignoreCase = true)

    private val addAcceptHeaderMiddleware = MutateHeadersMiddleware(extraHeaders = mapOf("Accept" to "application/json"))

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + addAcceptHeaderMiddleware
}
