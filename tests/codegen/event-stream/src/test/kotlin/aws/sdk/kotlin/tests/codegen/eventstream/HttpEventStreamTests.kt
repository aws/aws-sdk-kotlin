/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.tests.codegen.eventstream

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.test.restjson1.model.*
import aws.sdk.kotlin.test.restjson1.serde.deserializeTestStreamOpOperationBody
import aws.sdk.kotlin.test.restjson1.serde.serializeTestStreamOpOperationBody
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
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Integration test suite that checks the generated event stream serialization and deserialization codegen
 * works as expected for an HTTP-bound service.
 *
 * Most event shapes are covered by the Smithy `eventStreamTests` protocol tests run in smithy-kotlin. This suite only
 * covers cases those tests don't model (e.g. enum-typed event headers).
 */
@OptIn(InternalApi::class)
class HttpEventStreamTests {
    private suspend fun serializedMessage(event: TestStream): Message {
        val req = TestStreamOpRequest {
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

        val body = serializeTestStreamOpOperationBody(testContext, req)
        assertIs<HttpBody.ChannelContent>(body)

        // should be the actual message + the empty end frame
        val frames = decodeFrames(body.readFrom()).toList()
        assertEquals(2, frames.size)
        val signedMessage = frames[0]
        val buffer = SdkBuffer().apply { write(signedMessage.payload) }
        return Message.decode(buffer)
    }

    private suspend fun deserializedEvent(message: Message): TestStream {
        val buffer = SdkBuffer()
        message.encode(buffer)

        val response = HttpResponse(
            HttpStatusCode.OK,
            Headers.Empty,
            HttpBody.fromBytes(buffer.readByteArray()),
        )
        val call = HttpCall(HttpRequestBuilder().build(), response)

        val builder = TestStreamOpResponse.Builder()

        deserializeTestStreamOpOperationBody(builder, call)

        val resp = builder.build()
        checkNotNull(resp.value)

        return resp.value.single()
    }

    @Test
    fun testSerializeMessageWithEnumHeaders() = runTest {
        val event = TestStream.MessageWithEnumHeaders(
            MessageWithEnumHeaders {
                enum = aws.sdk.kotlin.test.restjson1.model.Enum.fromValue("DIAMOND")
                intEnum = IntEnum.fromValue(1)
            },
        )

        val message = serializedMessage(event)

        val headers = message.headers.associate { it.name to it.value }
        assertEquals("event", headers[":message-type"]?.expectString())
        assertEquals("MessageWithEnumHeaders", headers[":event-type"]?.expectString())
        assertEquals(aws.sdk.kotlin.test.restjson1.model.Enum.fromValue("DIAMOND").value, headers["enum"]?.expectString())
        assertEquals(IntEnum.fromValue(1).value, headers["intEnum"]?.expectInt32())

        val deserialized = deserializedEvent(message)
        assertIs<TestStream.MessageWithEnumHeaders>(deserialized)
        assertEquals(event, deserialized)
    }
}
