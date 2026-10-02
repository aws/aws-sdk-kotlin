/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.SdkBaseException
import aws.smithy.kotlin.runtime.ServiceErrorMetadata
import aws.smithy.kotlin.runtime.client.ResponseInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.operation.HttpOperationContext
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.telemetry.logging.debug
import aws.smithy.kotlin.runtime.telemetry.logging.warn
import kotlin.coroutines.coroutineContext

/**
 * Target service error codes that indicate the credentials themselves were rejected, as opposed to the request
 * being unauthorized. `AccessDenied` is deliberately absent: it usually means the credentials are valid but the
 * principal lacks permission, and refreshing them would not help.
 */
private val INVALIDATION_ERROR_CODES = setOf("ExpiredToken", "ExpiredTokenException", "InvalidToken")

/**
 * Signals credential invalidation back to the identity provider when a target service rejects the credentials that
 * signed the request.
 *
 * Registered per client. Does not modify the response and does not affect retry behavior — the failed request is
 * deliberately not retried on the strength of the invalidation; the refresh happens on the next resolution.
 */
@InternalSdkApi
public class CredentialsInvalidationInterceptor : HttpInterceptor {

    override suspend fun modifyBeforeAttemptCompletion(
        context: ResponseInterceptorContext<Any, Any, HttpRequest, HttpResponse?>,
    ): Result<Any> {
        val ex = context.response.exceptionOrNull() as? SdkBaseException ?: return context.response

        // Read from the deserialized exception rather than a response header: `x-amzn-ErrorType` is not populated for
        // all protocols, and query and XML services carry the code in the body.
        val errorCode = ex.sdkErrorMetadata.attributes.getOrNull(ServiceErrorMetadata.ErrorCode)
        if (errorCode !in INVALIDATION_ERROR_CODES) return context.response

        coroutineContext.debug<CredentialsInvalidationInterceptor> {
            "Credentials were rejected by the service with error code $errorCode"
        }

        val identity = context.executionContext.getOrNull(HttpOperationContext.ResolvedIdentity)
        val provider = context.executionContext.getOrNull(HttpOperationContext.ResolvedIdentityProvider)

        if (identity != null && provider != null) {
            try {
                provider.invalidate(identity)
            } catch (invalidationFailure: Exception) {
                // an implementation is not expected to throw here, so log loudly enough that the bug gets reported —
                // but never let invalidation bookkeeping replace the service error the caller needs to see
                coroutineContext.warn<CredentialsInvalidationInterceptor>(invalidationFailure) {
                    "failed to invalidate rejected credentials"
                }
            }
        }

        return context.response
    }
}
