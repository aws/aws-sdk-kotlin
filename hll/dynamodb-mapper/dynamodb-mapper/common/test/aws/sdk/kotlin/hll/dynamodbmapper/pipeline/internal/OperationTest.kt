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
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LReqContext
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import dev.mokkery.answering.calls
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.spy
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

private const val TABLE_NAME = "foo-table"

class OperationTest {
    private val ddbMapper = mock<DynamoDbMapper>()

    private val fooTable = object : PersistenceSpec<Foo> {
        override val mapper = ddbMapper
        override val schema = fooSchema
    }

    private fun interceptor() = spy<FooInterceptor>(object : FooInterceptor { })

    private val interceptorA = interceptor()
    private val interceptorB = interceptor()
    private val interceptors = listOf(interceptorA, interceptorB)

    private fun initialize(hReq: HFooRequest) = HReqContextImpl(hReq, fooSchema, MapperContextImpl(fooTable, "FooOp"))

    private val op = Operation(
        ::initialize,
        { ctx -> LFooRequest(TABLE_NAME, ctx.serializeSchema.converter.convertRight(ctx.highLevelRequest.foo)) },
        { ctx -> LFooResponse(ctx.lowLevelRequest.foo) },
        { ctx -> HFooResponse(ctx.deserializeSchema.converter.convertLeft(ctx.lowLevelResponse.foo)) },
        interceptors,
    )

    @Test
    fun testFullInvocationOrder() = runTest {
        val res = op.execute(HFooRequest(Foo("the foo")))
        assertEquals("the foo", res.foo.value) // Sanity check

        verify {
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
        every { interceptorA.modifyBeforeSerialization(any()) } calls {
            val ctx = assertIs<HReqContext<Foo, ItemSchema<Foo>, HFooRequest>>(it.args[0])
            SerializeInputImpl(HFooRequest(Foo(ctx.highLevelRequest.foo.value.reversed())), ctx.serializeSchema)
        }

        val res = op.execute(HFooRequest(Foo("the foo")))
        assertEquals("oof eht", res.foo.value) // Should be reversed
    }

    @Test
    fun testReadOnlyHookErrorIsThrown() = runTest {
        every { interceptorA.readAfterSerialization(any()) } throws RuntimeException("Cannot foo!")

        every { interceptorB.readAfterSerialization(any()) } calls {
            val ctx = assertIs<LReqContext<Foo, ItemSchema<Foo>, HFooRequest, LFooRequest?>>(it.args[0])
            val ex = assertIs<RuntimeException>(ctx.error)
            assertEquals("Cannot foo!", ex.message)
        }

        assertFailsWith<RuntimeException>("Cannot foo!") {
            op.execute(HFooRequest(Foo("the foo")))
        }

        verify {
            interceptorA.readAfterSerialization(any())
            interceptorB.readAfterSerialization(any())
        }

        // Should not continue to later interceptors
        verify(VerifyMode.not) {
            interceptorA.modifyBeforeInvocation(any())
            interceptorB.modifyBeforeInvocation(any())
        }
    }

    @Test
    fun testModifyHookErrorIsThrown() = runTest {
        every { interceptorA.modifyBeforeSerialization(any()) } throws RuntimeException("Cannot foo!")

        interceptors.forEach { interceptor ->
            every { interceptor.readBeforeSerialization(any()) } calls {
                val ctx = assertIs<HReqContext<Foo, ItemSchema<Foo>, HFooRequest>>(it.args[0])
                val ex = assertIs<RuntimeException>(ctx.error)
                assertEquals("Cannot foo!", ex.message)
            }
        }

        assertFailsWith<RuntimeException>("Cannot foo!") {
            op.execute(HFooRequest(Foo("the foo")))
        }

        verify {
            interceptorA.modifyBeforeSerialization(any())
            interceptorA.readBeforeSerialization(any())
            interceptorB.readBeforeSerialization(any())
        }

        // Should not continue to later interceptors
        verify(VerifyMode.not) {
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

private typealias FooInterceptor = Interceptor<Foo, ItemSchema<Foo>, HFooRequest, LFooRequest, LFooResponse, HFooResponse>
