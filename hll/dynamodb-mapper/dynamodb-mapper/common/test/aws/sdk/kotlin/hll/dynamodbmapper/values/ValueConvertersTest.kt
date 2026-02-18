/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.hll.dynamodbmapper.util.attr
import aws.sdk.kotlin.hll.dynamodbmapper.util.dynamicAttr
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A base class for unit testing [ValueConverter] instances, including a DSL for streamlining test setup. Tests are
 * configured using the [given] method and a DSL block that creates test steps using the
 * [infix function](https://kotlinlang.org/docs/functions.html#infix-notation) [TestBuilder.inDdbIs].
 *
 * Example usage:
 *
 * ```kotlin
 * fun testFooConverter() = given(fooConverterInstance) {
 *     // Test conversion of high-level value `aFoo` to low-level attribute value { "S": "a foo" } and back again
 *     aFoo inDdbIs attr("a foo")
 *
 *     // Test conversion of high-level value to an automatically-derived identical representation and back again
 *     bFoo inDdbIs theSame
 *
 *     // Test conversion resulting in an error
 *     cFoo inDdbIs anError
 *
 *     // Test conversion only going one way (useful when conversion back results in a different value)
 *     dFoo inDdbIs attr("d foo") whenGoing Direction.TO_DDB
 * }
 * ```
 * @see [given]
 * @see [TestBuilder.inDdbIs]
 * @see [TestBuilder.theSame]
 * @see [TestBuilder.anError]
 * @see [TestBuilder.whenGoing]
 * @see [attr]
 */
abstract class ValueConvertersTest {
    /**
     * Executes a series of individual tests on the given [converter]
     * @param converter The [ValueConverter] instance under test
     * @param block A DSL block for constructing individual tests via [TestBuilder.inDdbIs]
     */
    protected fun <T> given(converter: ValueConverter<T>, block: TestBuilder<T>.() -> Unit) {
        val tests = mutableListOf<TestCase<T>>()
        TestBuilder(tests).apply(block)

        tests.forEachIndexed { index, (steps) ->
            steps.forEach { (direction, highLevel, lowLevel) ->
                when (direction) {
                    Direction.TO_ATTRIBUTE_VALUE -> {
                        val result = runCatching { converter.convertRight(highLevel.requireInput()) }
                        lowLevel.assert(result, "Test $index failed converting to attribute value")
                    }

                    Direction.FROM_ATTRIBUTE_VALUE -> {
                        val result = runCatching { converter.convertLeft(lowLevel.requireInput()) }
                        highLevel.assert(result, "Test $index failed converting from attribute value")
                    }
                }
            }
        }
    }

    /**
     * Identifies a direction of conversion
     */
    enum class Direction {
        /**
         * Specifies conversion from a high-level value to a low-level DynamoDB attribute value
         */
        TO_ATTRIBUTE_VALUE,

        /**
         * Specifies conversion from a low-level DynamoDB attribute value to a high-level value
         */
        FROM_ATTRIBUTE_VALUE,
    }

    /**
     * A test case consisting of multiple test steps
     */
    data class TestCase<T>(val steps: List<TestStep<T>>) {
        companion object {
            fun <T> of(highLevel: TestExpectation<T>, lowLevel: TestExpectation<AttributeValue>): TestCase<T> {
                val toAv = highLevel.asSuccessOrNull()?.let { TestStep(Direction.TO_ATTRIBUTE_VALUE, it, lowLevel) }
                val fromAv = lowLevel.asSuccessOrNull()?.let { TestStep(Direction.FROM_ATTRIBUTE_VALUE, highLevel, it) }
                return TestCase(listOfNotNull(toAv, fromAv))
            }
        }
    }

    /**
     * A test step that describes the expectations for converting a value in a single direction
     */
    data class TestStep<T>(
        val direction: Direction,
        val highLevel: TestExpectation<T>,
        val lowLevel: TestExpectation<AttributeValue>,
    )

    /**
     * An expected value or outcome for an input or output to a conversion
     */
    sealed interface TestExpectation<out T> {
        fun assert(result: Result<*>, message: String)

        fun asSuccessOrNull(): Success<T>? = this as? Success

        fun requireInput(): T {
            require(this is Success) { "Cannot use $this as an input to a converter test" }
            return value
        }

        data class Success<out T>(val value: T) : TestExpectation<T> {
            override fun assert(result: Result<*>, message: String) {
                @Suppress("UNCHECKED_CAST")
                val output = result.getOrThrow() as T
                assertDeepEquals(value, output, message)
            }
        }

        data object Failure : TestExpectation<Nothing> {
            override fun assert(result: Result<*>, message: String) {
                assertTrue(result.isFailure, "$message (actual value: ${result.getOrNull()})")
            }
        }
    }

    class TestBuilder<T>(private val tests: MutableList<TestCase<T>>) {
        object AnError
        object TheSame

        /**
         * Indicates that an error is expected
         */
        val anError = AnError

        /**
         * Indicates that an automatically-derived identical representation is expected
         */
        val theSame = TheSame

        private fun T.addTest(lowLevel: AttributeValue): TestCase<T> = TestCase
            .of(TestExpectation.Success(this), TestExpectation.Success(lowLevel))
            .also(tests::add)

        private fun T.addTest(anError: AnError): TestCase<T> = TestCase
            .of(TestExpectation.Success(this), TestExpectation.Failure)
            .also(tests::add)

        infix fun T.inDdbIs(anError: AnError) = addTest(anError)
        infix fun T.inDdbIs(theSame: TheSame) = addTest(dynamicAttr(this))

        infix fun T.inDdbIs(attr: AttributeValue) = addTest(attr)
        infix fun T.inDdbIs(value: Boolean) = addTest(attr(value))
        infix fun T.inDdbIs(value: ByteArray) = addTest(attr(value))
        infix fun T.inDdbIs(value: List<Any?>) = addTest(attr(value))
        infix fun T.inDdbIs(value: Map<String, Any?>) = addTest(attr(value))
        infix fun T.inDdbIs(value: Nothing?) = addTest(attr(null))
        infix fun T.inDdbIs(value: Number) = addTest(attr(value))
        infix fun T.inDdbIs(value: String) = addTest(attr(value))

        @JvmName("inDdbIsSetByteArray")
        infix fun T.inDdbIs(value: Set<ByteArray>) = addTest(attr(value))

        @JvmName("inDdbIsSetNumber")
        infix fun T.inDdbIs(value: Set<Number>) = addTest(attr(value))

        @JvmName("inDdbIsSetString")
        infix fun T.inDdbIs(value: Set<String>) = addTest(attr(value))

        /**
         * Limits a test case to only a single direction (i.e., checking one of `fromAttributeValue`/`toAttributeValue`
         * but not both). Most test cases are bidirectional.
         */
        infix fun TestCase<T>.whenGoing(direction: Direction) {
            val removed = steps.filter { it.direction == direction }
            if (removed != steps) {
                tests.remove(this)
                tests.add(TestCase(removed))
            }
        }
    }
}

/**
 * A convenience function that allows asserting value equality for non-array types and content equality for array types
 * (including primitive arrays)
 */
@OptIn(ExperimentalUnsignedTypes::class)
@Suppress("UNCHECKED_CAST")
private fun <T> assertDeepEquals(expected: T, actual: T, message: String) = when {
    expected is Array<*> && actual is Array<*> ->
        assertContentEquals(expected as Array<Any>, actual as Array<Any>, message)

    expected is BooleanArray && actual is BooleanArray -> assertContentEquals(expected, actual, message)
    expected is ByteArray && actual is ByteArray -> assertContentEquals(expected, actual, message)
    expected is CharArray && actual is CharArray -> assertContentEquals(expected, actual, message)
    expected is DoubleArray && actual is DoubleArray -> assertContentEquals(expected, actual, message)
    expected is FloatArray && actual is FloatArray -> assertContentEquals(expected, actual, message)
    expected is IntArray && actual is IntArray -> assertContentEquals(expected, actual, message)
    expected is LongArray && actual is LongArray -> assertContentEquals(expected, actual, message)
    expected is ShortArray && actual is ShortArray -> assertContentEquals(expected, actual, message)
    expected is UByteArray && actual is UByteArray -> assertContentEquals(expected, actual, message)
    expected is UIntArray && actual is UIntArray -> assertContentEquals(expected, actual, message)
    expected is ULongArray && actual is ULongArray -> assertContentEquals(expected, actual, message)
    expected is UShortArray && actual is UShortArray -> assertContentEquals(expected, actual, message)
    else -> assertEquals(expected, actual, message)
}
