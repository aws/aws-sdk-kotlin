/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.imds

import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.operation.ModifyRequestMiddleware
import aws.smithy.kotlin.runtime.http.operation.SdkHttpOperation
import aws.smithy.kotlin.runtime.http.operation.SdkHttpRequest
import aws.smithy.kotlin.runtime.http.operation.setResolvedEndpoint
import aws.smithy.kotlin.runtime.http.request.HttpRequestBuilder
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.CachedValue
import aws.smithy.kotlin.runtime.util.ExpiringValue
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Tokens are cached to remove the need to reload the token between subsequent requests. To ensure
 * a request never fails with a 401 (expired token), a buffer window exists during which the token
 * is not expired but refreshed anyway to ensure the token doesn't expire during an in-flight operation.
 */
internal const val TOKEN_REFRESH_BUFFER_SECONDS = 120

internal const val X_AWS_EC2_METADATA_TOKEN_TTL_SECONDS = "x-aws-ec2-metadata-token-ttl-seconds"
internal const val X_AWS_EC2_METADATA_TOKEN = "x-aws-ec2-metadata-token"

internal class TokenMiddleware(
    private val httpClient: SdkHttpClient,
    private val endpointProvider: ImdsEndpointProvider,
    private val ttl: Duration = DEFAULT_TOKEN_TTL_SECONDS.seconds,
    private val clock: Clock = Clock.System,
) : ModifyRequestMiddleware {
    private var cachedToken = CachedValue<Token>(null, bufferTime = TOKEN_REFRESH_BUFFER_SECONDS.seconds, clock = clock)

    override fun install(op: SdkHttpOperation<*, *>) {
        op.execution.onEachAttempt.register(this)
    }

    override suspend fun modifyRequest(req: SdkHttpRequest): SdkHttpRequest {
        val token = cachedToken.getOrLoad { getToken(clock, req).let { ExpiringValue(it, it.expires) } }
        req.subject.headers.append(X_AWS_EC2_METADATA_TOKEN, token.value.decodeToString())
        return req
    }

    private suspend fun getToken(clock: Clock, req: SdkHttpRequest): Token {
        val logger = coroutineContext.logger<TokenMiddleware>()
        logger.trace { "refreshing IMDS token" }

        val tokenReq = HttpRequestBuilder().apply {
            method = HttpMethod.PUT
            headers.append(X_AWS_EC2_METADATA_TOKEN_TTL_SECONDS, ttl.inWholeSeconds.toString())
            req.subject.headers["User-Agent"]?.let { headers.append("User-Agent", it) }
            url.path.encoded = "/latest/api/token"
        }

        // endpoint resolution for the request happens right before signing, need to resolve the endpoint ourselves
        val endpoint = endpointProvider.resolveImdsEndpoint()
        setResolvedEndpoint(SdkHttpRequest(tokenReq), endpoint)

        val call = httpClient.call(tokenReq)
        logger.trace { call.toTraceString() }
        return try {
            when (call.response.status) {
                HttpStatusCode.OK -> {
                    val ttl = call.response.headers[X_AWS_EC2_METADATA_TOKEN_TTL_SECONDS]?.toLong() ?: throw EC2MetadataError(200, "No TTL provided in IMDS response")
                    val token = call.response.body.readAll() ?: throw EC2MetadataError(200, "No token provided in IMDS response")
                    val expires = clock.now() + ttl.seconds
                    Token(token, expires)
                }
                else -> {
                    val message = when (call.response.status) {
                        HttpStatusCode.Forbidden -> "Request forbidden: IMDS is disabled or the caller has insufficient permissions."
                        else -> "Failed to retrieve IMDS token"
                    }
                    throw EC2MetadataError(call.response.status.value, message)
                }
            }
        } finally {
            call.complete()
        }
    }
}
