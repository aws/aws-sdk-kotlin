/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.ServiceErrorMetadata
import aws.smithy.kotlin.runtime.ServiceException
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.client.ResponseInterceptorContext
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.http.operation.HttpOperationContext
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.identity.Identity
import aws.smithy.kotlin.runtime.identity.IdentityProvider
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CredentialsInvalidationInterceptorTest {
    private val credentials = Credentials("AKID", "secret", "token")

    private class RecordingProvider(private val failOnInvalidate: Boolean = false) : IdentityProvider {
        var rejected: Identity? = null

        override suspend fun resolve(attributes: Attributes): Identity = error("not needed for test")

        override suspend fun invalidate(rejectedIdentity: Identity) {
            rejected = rejectedIdentity
            if (failOnInvalidate) throw IllegalStateException("invalidation blew up")
        }
    }

    @Test
    fun testInvalidatesOnRejectedCredentials() = runTest {
        val provider = RecordingProvider()
        val ex = serviceException("ExpiredTokenException")
        val context = context(Result.failure(ex), credentials, provider)

        val result = CredentialsInvalidationInterceptor().modifyBeforeAttemptCompletion(context)

        assertEquals(credentials, provider.rejected)
        // the interceptor reports the invalidation, it does not change what the caller sees
        assertEquals(ex, result.exceptionOrNull())
    }

    @Test
    fun testInvalidatesOnEveryRejectionCode() = runTest {
        listOf("ExpiredToken", "ExpiredTokenException", "InvalidToken").forEach { code ->
            val provider = RecordingProvider()
            val context = context(Result.failure(serviceException(code)), credentials, provider)

            CredentialsInvalidationInterceptor().modifyBeforeAttemptCompletion(context)

            assertEquals(credentials, provider.rejected, "expected `$code` to invalidate")
        }
    }

    @Test
    fun testIgnoresAccessDenied() = runTest {
        val provider = RecordingProvider()
        val context = context(Result.failure(serviceException("AccessDenied")), credentials, provider)

        CredentialsInvalidationInterceptor().modifyBeforeAttemptCompletion(context)

        // AccessDenied usually means the credentials are fine and the principal lacks permission; refreshing them
        // would send the same request again with the same outcome.
        assertNull(provider.rejected)
    }

    @Test
    fun testIgnoresUnrelatedFailure() = runTest {
        val provider = RecordingProvider()
        val context = context(Result.failure(serviceException("ThrottlingException")), credentials, provider)

        CredentialsInvalidationInterceptor().modifyBeforeAttemptCompletion(context)

        assertNull(provider.rejected)
    }

    @Test
    fun testIgnoresSuccess() = runTest {
        val provider = RecordingProvider()
        val context = context(Result.success("payload"), credentials, provider)

        val result = CredentialsInvalidationInterceptor().modifyBeforeAttemptCompletion(context)

        assertNull(provider.rejected)
        assertEquals("payload", result.getOrNull())
    }

    @Test
    fun testIgnoresMissingResolvedIdentity() = runTest {
        // an operation that resolved no identity - an unsigned or anonymous one - has nothing to invalidate
        val ex = serviceException("ExpiredToken")
        val context = context(Result.failure(ex), identity = null, provider = null)

        val result = CredentialsInvalidationInterceptor().modifyBeforeAttemptCompletion(context)

        assertEquals(ex, result.exceptionOrNull())
    }

    @Test
    fun testFailedInvalidationDoesNotReplaceServiceError() = runTest {
        val provider = RecordingProvider(failOnInvalidate = true)
        val ex = serviceException("ExpiredToken")
        val context = context(Result.failure(ex), credentials, provider)

        val result = CredentialsInvalidationInterceptor().modifyBeforeAttemptCompletion(context)

        assertTrue(result.isFailure)
        assertEquals(ex, result.exceptionOrNull())
    }

    private fun serviceException(errorCode: String) = ServiceException().apply {
        sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorCode] = errorCode
    }

    private fun context(
        response: Result<Any>,
        identity: Identity?,
        provider: IdentityProvider?,
    ) = object : ResponseInterceptorContext<Any, Any, HttpRequest, HttpResponse?> {
        override val executionContext = ExecutionContext.build {
            identity?.let { attributes[HttpOperationContext.ResolvedIdentity] = it }
            provider?.let { attributes[HttpOperationContext.ResolvedIdentityProvider] = it }
        }
        override val request = Unit
        override val response = response
        override val protocolRequest = HttpRequest { }
        override val protocolResponse = null
    }
}
