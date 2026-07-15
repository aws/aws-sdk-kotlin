/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.express

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningAttributes
import aws.smithy.kotlin.runtime.collections.toMutableAttributes
import aws.smithy.kotlin.runtime.http.auth.HttpSigner
import aws.smithy.kotlin.runtime.http.auth.SignHttpRequest
import aws.smithy.kotlin.runtime.http.request.header

internal const val S3_EXPRESS_SESSION_TOKEN_HEADER = "X-Amz-S3session-Token"
private const val SESSION_TOKEN_HEADER = "X-Amz-Security-Token"

/**
 * An [HttpSigner] used for S3 Express requests. It has identical behavior with the given [httpSigner] except for two differences:
 *    1. Adds an `X-Amz-S3Session-Token` header, with a value of the credentials' sessionToken
 *    2. Removes the `X-Amz-Security-Token` header, which must not be sent for S3 Express requests.
 * @param httpSigner An instance of [HttpSigner]
 */
internal class S3ExpressHttpSigner(
    private val httpSigner: HttpSigner,
) : HttpSigner {
    /**
     * Sign the request, adding `X-Amz-S3Session-Token` header and removing `X-Amz-Security-Token` header.
     */
    override suspend fun sign(signingRequest: SignHttpRequest) {
        val sessionToken = (signingRequest.identity as? Credentials)?.sessionToken
            ?: error("No session token found on identity, required for S3 Express")

        // 1. add the S3 Express Session Token header
        signingRequest.httpRequest.header(S3_EXPRESS_SESSION_TOKEN_HEADER, sessionToken)

        // 2. enable omitSessionToken for awsHttpSigner to disable signing session token header
        val mutAttrs = signingRequest.signingAttributes.toMutableAttributes()
        mutAttrs[AwsSigningAttributes.OmitSessionToken] = true

        // 3. call main signer
        httpSigner.sign(signingRequest.copy(signingAttributes = mutAttrs))

        // 4. remove session token header
        signingRequest.httpRequest.headers.remove(SESSION_TOKEN_HEADER)
    }
}
