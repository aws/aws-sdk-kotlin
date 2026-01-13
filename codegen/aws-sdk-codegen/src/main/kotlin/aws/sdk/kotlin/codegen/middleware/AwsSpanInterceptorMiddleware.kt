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

private const val AWS_SPAN_INTERCEPTOR_NAME = "AwsSpanInterceptor"

class AwsSpanInterceptorMiddleware : ProtocolMiddleware {
    override val name: String = AWS_SPAN_INTERCEPTOR_NAME
    private val interceptorSymbol = buildSymbol {
        name = AWS_SPAN_INTERCEPTOR_NAME
        namespace(AwsKotlinDependency.AWS_HTTP, "interceptors")
    }
    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        writer.write("op.interceptors.add(#T)", interceptorSymbol)
    }
}
