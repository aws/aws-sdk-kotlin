/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.withKeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.PersistenceSpec
import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.HReqContext
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.Interceptor
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.InterceptorAny
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LReqContext
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

private const val TABLE_NAME = "foo-table"

// FIXME Should be in commonTest but mockk is JVM-only and finding a good KMP mocking library is hard

class OperationTest {
    private val ddbMapper = mockk<DynamoDbMapper>()

    private val fooTable = object : PersistenceSpec<Foo> {
        override val mapper = ddbMapper
        override val schema = fooSchema
    }

    private fun interceptor(name: String) =
        spyk(object : Interceptor<Foo, HFooRequest, LFooRequest, LFooResponse, HFooResponse> {}, name)

    private val interceptorA = interceptor("A")
    private val interceptorB = interceptor("B")
    private val interceptors = listOf(interceptorA, interceptorB)

    private fun initialize(hReq: HFooRequest) = HReqContextImpl(hReq, fooSchema, MapperContextImpl(fooTable, "FooOp"))

    private fun op(
        initialize: (HFooRequest) -> HReqContextImpl<Foo, HFooRequest> = ::initialize,
        serialize: (HFooRequest, ItemSchema<Foo>) -> LFooRequest = { hReq, schema -> hReq.convert(TABLE_NAME, schema) },
        lowLevelInvoke: suspend (LFooRequest) -> LFooResponse = ::dummyInvoke,
        deserialize: (LFooResponse, ItemSchema<Foo>) -> HFooResponse = { lRes, schema -> lRes.convert(schema) },
        interceptors: Collection<InterceptorAny> = this.interceptors,
    ) = Operation(initialize, serialize, lowLevelInvoke, deserialize, interceptors)

    @Test
    fun testFullInvocationOrder() = runTest {
        val res = op().execute(HFooRequest(Foo("the foo")))
        assertEquals("the foo", res.foo.value) // Sanity check

        verifyOrder {
            interceptorA.readAfterInitialization(any())
            interceptorB.readAfterInitialization(any())

            interceptorA.modifyBeforeSerialization(any())
            interceptorB.modifyBeforeSerialization(any())

            interceptorA.readBeforeSerialization(any())
            interceptorB.readBeforeSerialization(any())

            interceptorA.readAfterSerialization(any())
            interceptorB.readAfterSerialization(any())

            interceptorA.modifyBeforeInvocation(any())
            interceptorB.modifyBeforeInvocation(any())

            interceptorA.readBeforeInvocation(any())
            interceptorB.readBeforeInvocation(any())

            // Interceptor invocation order flips here

            interceptorB.readAfterInvocation(any())
            interceptorA.readAfterInvocation(any())

            interceptorB.modifyBeforeDeserialization(any())
            interceptorA.modifyBeforeDeserialization(any())

            interceptorB.readBeforeDeserialization(any())
            interceptorA.readBeforeDeserialization(any())

            interceptorB.readAfterDeserialization(any())
            interceptorA.readAfterDeserialization(any())

            interceptorB.modifyBeforeCompletion(any())
            interceptorA.modifyBeforeCompletion(any())

            interceptorB.readBeforeCompletion(any())
            interceptorA.readBeforeCompletion(any())
        }
    }

    @Test
    fun testModifyHook() = runTest {
        every { interceptorA.modifyBeforeSerialization(any()) } answers {
            val ctx = assertIs<HReqContext<Foo, HFooRequest>>(it.invocation.args[0])
            SerializeInputImpl(HFooRequest(Foo(ctx.highLevelRequest.foo.value.reversed())), ctx.serializeSchema)
        }

        val res = op().execute(HFooRequest(Foo("the foo")))
        assertEquals("oof eht", res.foo.value) // Should be reversed
    }

    @Test
    fun testReadOnlyHookErrorIsThrown() = runTest {
        every { interceptorA.readAfterSerialization(any()) } throws RuntimeException("Cannot foo!")

        every { interceptorB.readAfterSerialization(any()) } answers {
            val ctx = assertIs<LReqContext<Foo, HFooRequest, LFooRequest?>>(it.invocation.args[0])
            val ex = assertIs<RuntimeException>(ctx.error)
            assertEquals("Cannot foo!", ex.message)
        }

        assertFailsWith<RuntimeException>("Cannot foo!") {
            op().execute(HFooRequest(Foo("the foo")))
        }

        verifyOrder {
            interceptorA.readAfterSerialization(any())
            interceptorB.readAfterSerialization(any())
        }

        // Should not continue to later interceptors
        verify(inverse = true) {
            interceptorA.modifyBeforeInvocation(any())
            interceptorB.modifyBeforeInvocation(any())
        }
    }

    @Test
    fun testModifyHookErrorIsThrown() = runTest {
        every { interceptorA.modifyBeforeSerialization(any()) } throws RuntimeException("Cannot foo!")

        interceptors.forEach { interceptor ->
            every { interceptor.readBeforeSerialization(any()) } answers {
                val ctx = assertIs<HReqContext<Foo, HFooRequest>>(it.invocation.args[0])
                val ex = assertIs<RuntimeException>(ctx.error)
                assertEquals("Cannot foo!", ex.message)
            }
        }

        assertFailsWith<RuntimeException>("Cannot foo!") {
            op().execute(HFooRequest(Foo("the foo")))
        }

        verifyOrder {
            interceptorA.modifyBeforeSerialization(any())
            interceptorA.readBeforeSerialization(any())
            interceptorB.readBeforeSerialization(any())
        }

        // Should not continue to later interceptors
        verify(inverse = true) {
            interceptorB.modifyBeforeSerialization(any())
            interceptorA.readAfterSerialization(any())
            interceptorB.readAfterSerialization(any())
        }
    }
}

private data class Foo(val value: String)

private val fooConverter = object : ItemConverter<Foo> {
    override val left: MonoConverter<Item, Foo> = MonoConverter { item ->
        Foo(item["foo"]!!.asS())
    }

    override val right: MonoConverter<Foo, Item> = MonoConverter { obj ->
        itemOf("foo" to AttributeValue.S(obj.value))
    }
}
private val fooSchema = fooConverter.withKeySpec(KeySpec.string("foo"))

private data class HFooRequest(val foo: Foo)
private data class LFooRequest(val table: String, val foo: Item)
private data class LFooResponse(val foo: Item)
private data class HFooResponse(val foo: Foo)

private fun HFooRequest.convert(table: String, schema: ItemSchema<Foo>) =
    LFooRequest(table, schema.converter.convertRight(foo))

private fun LFooResponse.convert(schema: ItemSchema<Foo>) =
    HFooResponse(schema.converter.convertLeft(foo))

private suspend fun dummyInvoke(req: LFooRequest) = LFooResponse(req.foo)
