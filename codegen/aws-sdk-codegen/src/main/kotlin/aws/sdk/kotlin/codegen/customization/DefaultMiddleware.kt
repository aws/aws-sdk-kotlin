/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import aws.sdk.kotlin.codegen.middleware.AwsSpanInterceptorMiddleware
import aws.sdk.kotlin.codegen.middleware.RecursionDetectionMiddleware
import aws.sdk.kotlin.codegen.middleware.UserAgentMiddleware
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware

/**
 * Registers default HTTP protocol middleware used by every SDK
 */
class DefaultMiddleware : KotlinIntegration {
    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved +
        listOf(
            UserAgentMiddleware(),
            RecursionDetectionMiddleware(),
            AwsSpanInterceptorMiddleware(),
        )
}
