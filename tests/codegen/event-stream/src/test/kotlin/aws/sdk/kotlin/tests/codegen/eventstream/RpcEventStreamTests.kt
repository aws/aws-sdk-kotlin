/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.tests.codegen.eventstream

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.test.awsjson11.model.MessageWithString
import aws.sdk.kotlin.test.awsjson11.model.TestStream
import aws.sdk.kotlin.test.awsjson11.model.TestStreamOperationWithInitialRequestResponseRequest
import aws.sdk.kotlin.test.awsjson11.model.TestStreamOperationWithInitialRequestResponseResponse
import aws.sdk.kotlin.test.awsjson11.serde.deserializeTestStreamOperationWithInitialRequestResponseOperationBody
import aws.sdk.kotlin.test.awsjson11.serde.serializeTestStreamOperationWithInitialRequestResponseOperationBody
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningAttributes
import aws.smithy.kotlin.runtime.auth.awssigning.DefaultAwsSigner
import aws.smithy.kotlin.runtime.auth.awssigning.HashSpecification
import aws.smithy.kotlin.runtime.awsprotocol.eventstream.*
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpCall
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.request.HttpRequestBuilder
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.io.SdkBuffer
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Integration test suite that checks the codegen for `initial-request` message serialization and
 * `initial-response` message deserialization works as expected for an RPC-bound service.
 */
@OptIn(InternalApi::class)
class RpcEventStreamTests {
    @Test
    fun testInitialRequest() = runTest {
        val eventStreamData = "Hello, this is the event stream"
        val event = TestStream.MessageWithString(
            MessageWithString { data = eventStreamData },
        )
        val initialRequestData = "This is the user's initial request!"

        val messages = serializedMessages(event, initialRequestData)

        // validate initial-request message
        val buffer = SdkBuffer().also { it.write(messages[0].payload) }
        val initialMessage = Message.decode(buffer)
        val initialMessageHeaders = initialMessage.headers.associate { it.name to it.value }
        assertEquals("event", initialMessageHeaders[":message-type"]?.expectString())
        assertEquals("initial-request", initialMessageHeaders[":event-type"]?.expectString())
        assertEquals("""{"initial":"$initialRequestData"}""", initialMessage.payload.decodeToString())

        // validate the event stream
        buffer.write(messages[1].payload)
        val eventStreamMessage = Message.decode(buffer)
        val eventStreamMessageHeaders = eventStreamMessage.headers.associate { it.name to it.value }
        assertEquals("event", eventStreamMessageHeaders[":message-type"]?.expectString())
        assertEquals("MessageWithString", eventStreamMessageHeaders[":event-type"]?.expectString())
        assertEquals("text/plain", eventStreamMessageHeaders[":content-type"]?.expectString())
        assertEquals(eventStreamData, eventStreamMessage.payload.decodeToString())
    }

    /**
     * Test handling a service response containing an initial-response event
     */
    @Test
    fun testInitialResponse() = runTest {
        val initialResponseData = "This is the service's initial response!"

        val initialResponseMessage = buildMessage {
            payload = """{"initial": "$initialResponseData"}""".encodeToByteArray()
            addHeader(":message-type", HeaderValue.String("event"))
            addHeader(":event-type", HeaderValue.String("initial-response"))
            addHeader(":content-type", HeaderValue.String("application/json"))
        }

        val eventStreamData = "Hello, this is the event stream"
        val eventStreamResponse = buildMessage {
            payload = eventStreamData.encodeToByteArray()
            addHeader(":message-type", HeaderValue.String("event"))
            addHeader(":event-type", HeaderValue.String("MessageWithString"))
            addHeader(":content-type", HeaderValue.String("application/json"))
        }

        val responseBody = flowOf(initialResponseMessage, eventStreamResponse)
            .encode()
            .asEventStreamHttpBody()
        val builder = TestStreamOperationWithInitialRequestResponseResponse.Builder()

        deserializeTestStreamOperationWithInitialRequestResponseOperationBody(builder, responseBody.asHttpCall())

        assertEquals(builder.initial, initialResponseData)
        val event = builder.value?.single() // this throws an exception if there's not exactly 1 event
        assertEquals(eventStreamData, event?.asMessageWithString()?.data)
    }

    /**
     * Test handling a service response where the initial-response is not present
     * (because it's optional, it may not get returned every time)
     */
    @Test
    fun testInitialResponseNotPresent() = runTest {
        val eventStreamData = "Hello, this is the event stream"
        val eventStreamResponse = buildMessage {
            payload = eventStreamData.encodeToByteArray()
            addHeader(":message-type", HeaderValue.String("event"))
            addHeader(":event-type", HeaderValue.String("MessageWithString"))
            addHeader(":content-type", HeaderValue.String("application/json"))
        }

        val responseBody = flowOf(eventStreamResponse)
            .encode()
            .asEventStreamHttpBody()

        val builder = TestStreamOperationWithInitialRequestResponseResponse.Builder()
        deserializeTestStreamOperationWithInitialRequestResponseOperationBody(builder, responseBody.asHttpCall())

        assertNull(builder.initial)
        val event = builder.value?.single()
        assertEquals(eventStreamData, event?.asMessageWithString()?.data)
    }

    private suspend fun serializedMessages(event: TestStream, initialRequestData: String? = null): List<Message> {
        val req = TestStreamOperationWithInitialRequestResponseRequest {
            initial = initialRequestData
            value = flowOf(event)
        }

        val testContext = ExecutionContext.build {
            attributes[AwsSigningAttributes.SigningRegion] = "us-east-2"
            attributes[AwsSigningAttributes.SigningService] = "test"
            attributes[AwsSigningAttributes.CredentialsProvider] = StaticCredentialsProvider(
                Credentials("fake-access-key", "fake-secret-key"),
            )
            attributes[AwsSigningAttributes.Signer] = DefaultAwsSigner
        }

        testContext.launch {
            // complete the request signature (giving enough time to setup the deferred)
            delay(200.milliseconds)
            testContext[AwsSigningAttributes.RequestSignature].complete(HashSpecification.EmptyBody.hash.encodeToByteArray())
        }

        val body = serializeTestStreamOperationWithInitialRequestResponseOperationBody(testContext, req)
        // the frames are now signed and serialized

        assertIs<HttpBody.ChannelContent>(body)

        // should be an optional initial-request, the event stream, and the empty end frame
        val frames = decodeFrames(body.readFrom()).toList()
        val expectedFramesSize = initialRequestData?.let { 3 } ?: 2

        assertEquals(expectedFramesSize, frames.size)

        return frames
    }

    private fun HttpBody.asHttpCall(): HttpCall {
        val response = HttpResponse(
            HttpStatusCode.OK,
            Headers.Empty,
            this,
        )
        return HttpCall(HttpRequestBuilder().build(), response)
    }
}
