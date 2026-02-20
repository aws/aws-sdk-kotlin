/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.middleware

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.model.buildSymbol
import aws.smithy.kotlin.codegen.model.namespace
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.shapes.OperationShape

/**
 * HTTP middleware to add the recursion detection header where required.
 */
class RecursionDetectionMiddleware : ProtocolMiddleware {
    override val name: String = "RecursionDetection"
    override val order: Byte = 30

    private val middlewareSymbol = buildSymbol {
        name = "RecursionDetection"
        namespace(AwsKotlinDependency.AWS_HTTP, subpackage = "middleware")
    }

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        writer.write("op.install(#T())", middlewareSymbol)
    }
}
