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
 * Captures precise timestamps around protocol serialization and deserialization only,
 * excluding signing, interceptors, retry handling, and other client-side machinery.
 *
 * Serialization:
 * - Start: [readBeforeSerialization] — immediately before the input is marshalled
 * - End: [readAfterSerialization] — immediately after the protocol request is produced
 *
 * Deserialization:
 * - Start: [readBeforeDeserialization] — immediately before the HTTP response is unmarshalled
 * - End: [readAfterDeserialization] — immediately after the output object is produced
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

    override fun readBeforeSerialization(context: RequestInterceptorContext<Any>) {
        serializationStartNanos = System.nanoTime()
    }

    override fun readAfterSerialization(context: ProtocolRequestInterceptorContext<Any, HttpRequest>) {
        serializationEndNanos = System.nanoTime()
    }

    override fun readBeforeDeserialization(context: ProtocolResponseInterceptorContext<Any, HttpRequest, HttpResponse>) {
        deserializationStartNanos = System.nanoTime()
    }

    override fun readAfterDeserialization(context: ResponseInterceptorContext<Any, Any, HttpRequest, HttpResponse>) {
        deserializationEndNanos = System.nanoTime()
    }
}
