/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http.interceptors

import aws.sdk.kotlin.runtime.http.operation.CustomUserAgentMetadata
import aws.smithy.kotlin.runtime.client.RequestInterceptorContext
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import kotlin.test.Test
import kotlin.test.assertEquals

class AddUserAgentMetadataInterceptorTest {
    @Test
    fun testAddNew() {
        val ctx = interceptorContext()

        val interceptor = AddUserAgentMetadataInterceptor(metadata)
        interceptor.readBeforeExecution(ctx)

        val actual = ctx.executionContext[CustomUserAgentMetadata.ContextKey].extras
        assertEquals("bar", actual["foo"])
    }

    @Test
    fun testMerge() {
        val existingMetadata = mapOf("foo" to "This value will be replaced", "baz" to "qux")
        val executionContext = ExecutionContext().apply {
            set(CustomUserAgentMetadata.ContextKey, CustomUserAgentMetadata(extras = existingMetadata))
        }
        val ctx = interceptorContext(executionContext)

        val interceptor = AddUserAgentMetadataInterceptor(metadata)
        interceptor.readBeforeExecution(ctx)

        val actual = ctx.executionContext[CustomUserAgentMetadata.ContextKey].extras
        assertEquals("bar", actual["foo"])
        assertEquals("qux", actual["baz"])
    }
}

private val metadata = mapOf("foo" to "bar")

private fun interceptorContext(executionContext: ExecutionContext = ExecutionContext()) = object : RequestInterceptorContext<Any> {
    override val request: Any = Unit
    override val executionContext: ExecutionContext = executionContext
}
