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
import kotlin.time.Duration
import kotlin.time.TimeSource

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
    private val timeSource = TimeSource.Monotonic

    private var serializationStart: TimeSource.Monotonic.ValueTimeMark = timeSource.markNow()
    private var serializationDuration: Duration = Duration.ZERO
    private var deserializationStart: TimeSource.Monotonic.ValueTimeMark = timeSource.markNow()
    private var deserializationDuration: Duration = Duration.ZERO

    fun reset() {
        serializationDuration = Duration.ZERO
        deserializationDuration = Duration.ZERO
    }

    fun serializationNanos(): Long = serializationDuration.inWholeNanoseconds

    fun deserializationNanos(): Long = deserializationDuration.inWholeNanoseconds

    override fun readBeforeSerialization(context: RequestInterceptorContext<Any>) {
        serializationStart = timeSource.markNow()
    }

    override fun readAfterSerialization(context: ProtocolRequestInterceptorContext<Any, HttpRequest>) {
        serializationDuration = serializationStart.elapsedNow()
    }

    override fun readBeforeDeserialization(context: ProtocolResponseInterceptorContext<Any, HttpRequest, HttpResponse>) {
        deserializationStart = timeSource.markNow()
    }

    override fun readAfterDeserialization(context: ResponseInterceptorContext<Any, Any, HttpRequest, HttpResponse>) {
        deserializationDuration = deserializationStart.elapsedNow()
    }
}
