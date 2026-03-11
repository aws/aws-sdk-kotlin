/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.glacier

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
 * Adds a middleware that sets the "X-Amz-Glacier-Version" header to the service model version for all requests
 * See https://docs.aws.amazon.com/amazonglacier/latest/dev/api-common-request-headers.html
 */
class GlacierAddVersionHeader : KotlinIntegration {

    override fun enabledForService(model: Model, settings: KotlinSettings) = model.expectShape<ServiceShape>(settings.service).sdkId.equals("Glacier", ignoreCase = true)

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ) = resolved + MutateHeadersMiddleware(
        extraHeaders = mapOf(
            "X-Amz-Glacier-Version" to ctx.model.expectShape<ServiceShape>(ctx.settings.service).version,
        ),
    )
}
