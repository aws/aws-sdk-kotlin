/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.services.sesv2.SesV2Client
import aws.sdk.kotlin.services.sesv2.sendEmail
import aws.smithy.kotlin.runtime.auth.awssigning.crt.CrtAwsSigner
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.http.HttpException
import aws.smithy.kotlin.runtime.http.auth.SigV4AsymmetricAuthScheme
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.io.use
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class Sigv4aTest {
    @Test
    fun testSigv4a() = runTest {
        val interceptor = RequestCapturingInterceptor()

        SesV2Client.fromEnvironment {
            retryStrategy {
                maxAttempts = 1 // The call is intended to fail, no sense trying more than once
            }

            interceptors += interceptor

            authSchemes = listOf(SigV4AsymmetricAuthScheme(CrtAwsSigner, "ses"))
        }.use { ses ->
            assertFailsWith<HttpException> {
                ses.sendEmail {
                    endpointId = "bdm3x3zl.n5x"
                }
            }
        }

        assertEquals(1, interceptor.requests.size)
        val request = interceptor.requests.single()

        assertContains("bdm3x3zl.n5x.endpoints.email.amazonaws.com", request.url.host.toString()) // Verify endpoint

        val authHeader = assertNotNull(
            request.headers["Authorization"],
            "Missing Authorization header, found: ${request.headers.entries().map { it.key }}",
        )
        assertContains(authHeader, "AWS4-ECDSA-P256-SHA256") // Verify that request was signed with Sigv4a
    }
}

private class RequestCapturingInterceptor : HttpInterceptor {
    val requests = mutableListOf<HttpRequest>()

    override fun readBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>) {
        requests += context.protocolRequest
    }
}
