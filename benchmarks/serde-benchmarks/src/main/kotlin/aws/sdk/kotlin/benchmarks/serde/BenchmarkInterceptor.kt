/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.serde

import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.client.ProtocolResponseInterceptorContext
import aws.smithy.kotlin.runtime.client.RequestInterceptorContext
import aws.smithy.kotlin.runtime.client.ResponseInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse

/**
 * Captures precise timestamps at the SDK's boundaries per the benchmark spec:
 *
 * Serialization:
 * - Start: [readBeforeExecution] — immediately before the SDK does anything (matches spec's
 *   "immediately before the invocation that would transmit the request to the server")
 * - End: [readBeforeTransmit] — right before the HTTP request is sent to the transport
 *   (matches spec's "immediately after the request object is no longer mutated")
 *
 * Deserialization:
 * - Start: [readBeforeDeserialization] — immediately before the HTTP response is handed
 *   to SDK deserialization code
 * - End: [readAfterDeserialization] — immediately after the final output object is produced
 */
class BenchmarkInterceptor : HttpInterceptor {
    var serializationStartNanos: Long = 0L
        private set
    var serializationEndNanos: Long = 0L
        private set
    var deserializationStartNanos: Long = 0L
        private set
    var deserializationEndNanos: Long = 0L
        private set

    fun reset() {
        serializationStartNanos = 0L
        serializationEndNanos = 0L
        deserializationStartNanos = 0L
        deserializationEndNanos = 0L
    }

    fun serializationNanos(): Long = serializationEndNanos - serializationStartNanos

    fun deserializationNanos(): Long = deserializationEndNanos - deserializationStartNanos

    override fun readBeforeExecution(context: RequestInterceptorContext<Any>) {
        serializationStartNanos = System.nanoTime()
    }

    override fun readBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>) {
        serializationEndNanos = System.nanoTime()
    }

    override fun readBeforeDeserialization(context: ProtocolResponseInterceptorContext<Any, HttpRequest, HttpResponse>) {
        deserializationStartNanos = System.nanoTime()
    }

    override fun readAfterDeserialization(context: ResponseInterceptorContext<Any, Any, HttpRequest, HttpResponse>) {
        deserializationEndNanos = System.nanoTime()
    }
}
