/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.middleware

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.model.getTrait
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.aws.traits.protocols.AwsQueryErrorTrait
import software.amazon.smithy.model.knowledge.OperationIndex
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.StructureShape

/**
 * The error codes that mean a target service rejected the credentials that signed a request. Must match the codes the
 * runtime `CredentialsInvalidationInterceptor` checks.
 */
private val INVALIDATION_ERROR_CODES = setOf("ExpiredToken", "ExpiredTokenException")

/**
 * Registers the interceptor that tells the identity provider when the target service rejected the credentials it
 * supplied, so the next resolution refreshes them instead of returning the rejected value from cache.
 *
 * An operation that models one of the rejection error codes as its own error uses it for something other than the
 * signing credentials, for example an expired token in its input, so that code is passed to the interceptor to be
 * ignored for that operation.
 */
class CredentialsInvalidationMiddleware : ProtocolMiddleware {
    override val name: String = "CredentialsInvalidation"

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        // Includes the errors the service shape binds to every operation.
        val modeledErrorCodes = OperationIndex.of(ctx.model).getErrors(ctx.service, op)
            .map { it.wireErrorCode() }
            .filter { it in INVALIDATION_ERROR_CODES }
            .toSortedSet()

        if (modeledErrorCodes.isEmpty()) {
            writer.write("op.interceptors.add(#T())", AwsRuntimeTypes.Config.Credentials.CredentialsInvalidationInterceptor)
        } else {
            writer.write(
                "op.interceptors.add(#T(setOf(#L)))",
                AwsRuntimeTypes.Config.Credentials.CredentialsInvalidationInterceptor,
                modeledErrorCodes.joinToString { "\"$it\"" },
            )
        }
    }
}

/**
 * The error code the service sends for this error: the `@awsQueryError` code for awsQuery services that set one, and
 * the shape name otherwise. This is the code the runtime reads from the deserialized error.
 */
private fun StructureShape.wireErrorCode(): String = getTrait<AwsQueryErrorTrait>()?.code ?: id.name
