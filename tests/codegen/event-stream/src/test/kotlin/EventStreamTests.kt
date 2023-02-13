/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.test.eventstream.restjson1.model.*
import aws.sdk.kotlin.test.eventstream.restjson1.transform.deserializeTestStreamOpOperationBody
import aws.sdk.kotlin.test.eventstream.restjson1.transform.serializeTestStreamOpOperationBody
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningAttributes
import aws.smithy.kotlin.runtime.auth.awssigning.DefaultAwsSigner
import aws.smithy.kotlin.runtime.auth.awssigning.HashSpecification
import aws.smithy.kotlin.runtime.awsprotocol.eventstream.*
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.io.SdkBuffer
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.smithy.test.assertJsonStringsEqual
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Integration test suite that checks the generated event stream serialization and deserialization codegen
 * works as expected.
 */
@OptIn(ExperimentalCoroutinesApi::class, InternalApi::class)
class EventStreamTests {
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
            attributes[AwsSigningAttributes.RequestSignature] = HashSpecification.EmptyBody.hash.encodeToByteArray()
            attributes[AwsSigningAttributes.Signer] = DefaultAwsSigner
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
        val body = ByteArrayContent(buffer.readByteArray())
        val builder = TestStreamOpResponse.Builder()

        deserializeTestStreamOpOperationBody(builder, body)

        val resp = builder.build()
        checkNotNull(resp.value)

        return resp.value.single()
    }

    @Test
    fun testSerializeMessageWithBlob() = runTest {
        val event = TestStream.MessageWithBlob(MessageWithBlob { data = "hello from Kotlin".encodeToByteArray() })

        val message = serializedMessage(event)

        val headers = message.headers.associate { it.name to it.value }
        assertEquals("event", headers[":message-type"]?.expectString())
        assertEquals("MessageWithBlob", headers[":event-type"]?.expectString())
        assertEquals("application/octet-stream", headers[":content-type"]?.expectString())
        assertEquals("hello from Kotlin", message.payload.decodeToString())

        val deserialized = deserializedEvent(message)
        assertIs<TestStream.MessageWithBlob>(deserialized)
        assertEquals(event, deserialized)
    }

    @Test
    fun testSerializeMessageWithString() = runTest {
        val event = TestStream.MessageWithString(MessageWithString { data = "hello from Kotlin" })

        val message = serializedMessage(event)

        val headers = message.headers.associate { it.name to it.value }
        assertEquals("event", headers[":message-type"]?.expectString())
        assertEquals("MessageWithString", headers[":event-type"]?.expectString())
        assertEquals("text/plain", headers[":content-type"]?.expectString())
        assertEquals("hello from Kotlin", message.payload.decodeToString())

        val deserialized = deserializedEvent(message)
        assertIs<TestStream.MessageWithString>(deserialized)
        assertEquals(event, deserialized)
    }

    @Test
    fun testSerializeMessageWithStruct() = runTest {
        val event = TestStream.MessageWithStruct(
            MessageWithStruct {
                someStruct {
                    someInt = 2
                    someString = "hello struct!"
                }
            },
        )

        val message = serializedMessage(event)

        val headers = message.headers.associate { it.name to it.value }
        assertEquals("event", headers[":message-type"]?.expectString())
        assertEquals("MessageWithStruct", headers[":event-type"]?.expectString())
        assertEquals("application/json", headers[":content-type"]?.expectString())

        val expectedBody = """{"someInt":2,"someString":"hello struct!"}"""
        assertJsonStringsEqual(expectedBody, message.payload.decodeToString())

        val deserialized = deserializedEvent(message)
        assertIs<TestStream.MessageWithStruct>(deserialized)
        assertEquals(event, deserialized)
    }

    @Test
    fun testSerializeMessageWithUnion() = runTest {
        val event = TestStream.MessageWithUnion(
            MessageWithUnion {
                someUnion = TestUnion.Foo("a lep is a ball")
            },
        )

        val message = serializedMessage(event)

        val headers = message.headers.associate { it.name to it.value }
        assertEquals("event", headers[":message-type"]?.expectString())
        assertEquals("MessageWithUnion", headers[":event-type"]?.expectString())
        assertEquals("application/json", headers[":content-type"]?.expectString())

        val expectedBody = """{"Foo":"a lep is a ball"}"""
        assertJsonStringsEqual(expectedBody, message.payload.decodeToString())

        val deserialized = deserializedEvent(message)
        assertIs<TestStream.MessageWithUnion>(deserialized)
        assertEquals(event, deserialized)
    }

    @Test
    fun testSerializeMessageWithHeaders() = runTest {
        val event = TestStream.MessageWithHeaders(
            MessageWithHeaders {
                blob = "blobby".encodeToByteArray()
                boolean = true
                byte = 55
                int = 100_000
                short = 16_000
                long = 9_000_000_000L
                string = "a tay is a hammer"
                timestamp = Instant.fromEpochSeconds(5)
            },
        )

        val message = serializedMessage(event)

        val headers = message.headers.associate { it.name to it.value }
        assertEquals("event", headers[":message-type"]?.expectString())
        assertEquals("MessageWithHeaders", headers[":event-type"]?.expectString())
        assertEquals("blobby", headers["blob"]?.expectByteArray()?.decodeToString())
        assertEquals(true, headers["boolean"]?.expectBool())
        assertEquals(55, headers["byte"]?.expectByte())
        assertEquals(16_000, headers["short"]?.expectInt16())
        assertEquals(100_000, headers["int"]?.expectInt32())
        assertEquals(9_000_000_000L, headers["long"]?.expectInt64())
        assertEquals("a tay is a hammer", headers["string"]?.expectString())
        assertEquals(Instant.fromEpochSeconds(5), headers["timestamp"]?.expectTimestamp())

        val deserialized = deserializedEvent(message)
        assertIs<TestStream.MessageWithHeaders>(deserialized)
        assertEquals(event, deserialized)
    }

    @Test
    fun testSerializeMessageWithHeaderAndPayload() = runTest {
        val event = TestStream.MessageWithHeaderAndPayload(
            MessageWithHeaderAndPayload {
                header = "a korf is a tiger"
                payload = "remember a korf is a tiger".encodeToByteArray()
            },
        )

        val message = serializedMessage(event)

        val headers = message.headers.associate { it.name to it.value }
        assertEquals("event", headers[":message-type"]?.expectString())
        assertEquals("MessageWithHeaderAndPayload", headers[":event-type"]?.expectString())
        assertEquals("a korf is a tiger", headers["header"]?.expectString())
        assertEquals("remember a korf is a tiger", message.payload.decodeToString())

        val deserialized = deserializedEvent(message)
        assertIs<TestStream.MessageWithHeaderAndPayload>(deserialized)
        assertEquals(event, deserialized)
    }

    @Test
    fun testSerializeMessageWithNoTraits() = runTest {
        val event = TestStream.MessageWithNoHeaderPayloadTraits(
            MessageWithNoHeaderPayloadTraits {
                someInt = 2
                someString = "a flix is comb"
            },
        )

        val message = serializedMessage(event)

        val headers = message.headers.associate { it.name to it.value }
        assertEquals("event", headers[":message-type"]?.expectString())
        assertEquals("MessageWithNoHeaderPayloadTraits", headers[":event-type"]?.expectString())
        assertEquals("application/json", headers[":content-type"]?.expectString())
        val expectedBody = """{"someInt":2,"someString":"a flix is comb"}"""
        assertJsonStringsEqual(expectedBody, message.payload.decodeToString())

        val deserialized = deserializedEvent(message)
        assertIs<TestStream.MessageWithNoHeaderPayloadTraits>(deserialized)
        assertEquals(event, deserialized)
    }

    @Test
    fun testSerializeMessageWithUnboundPayload() = runTest {
        val event = TestStream.MessageWithUnboundPayloadTraits(
            MessageWithUnboundPayloadTraits {
                header = "a korf is a tiger"
                unboundString = "a flix is comb"
            },
        )

        val message = serializedMessage(event)

        val headers = message.headers.associate { it.name to it.value }
        assertEquals("event", headers[":message-type"]?.expectString())
        assertEquals("MessageWithUnboundPayloadTraits", headers[":event-type"]?.expectString())
        assertEquals("application/json", headers[":content-type"]?.expectString())

        assertEquals("a korf is a tiger", headers["header"]?.expectString())

        val expectedBody = """{"unboundString":"a flix is comb"}"""
        assertJsonStringsEqual(expectedBody, message.payload.decodeToString())

        val deserialized = deserializedEvent(message)
        assertIs<TestStream.MessageWithUnboundPayloadTraits>(deserialized)
        assertEquals(event, deserialized)
    }
}
