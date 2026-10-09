/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.middleware

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.shapes.OperationShape

/**
 * Registers the interceptor that tells the identity provider when the target service rejected the credentials it
 * supplied, so the next resolution refreshes them instead of returning the rejected value from cache.
 */
class CredentialsInvalidationMiddleware : ProtocolMiddleware {
    override val name: String = "CredentialsInvalidation"

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        writer.write("op.interceptors.add(#T())", AwsRuntimeTypes.Config.Credentials.CredentialsInvalidationInterceptor)
    }
}
