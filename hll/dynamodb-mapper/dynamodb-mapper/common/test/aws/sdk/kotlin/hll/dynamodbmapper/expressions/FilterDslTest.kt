/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.FilterDslImpl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.ParameterizingExpressionVisitor
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.UByteRange
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.UShortRange
import aws.sdk.kotlin.hll.dynamodbmapper.util.av
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import kotlin.test.Test
import kotlin.test.assertEquals

class FilterDslTest {
    @Test
    fun testAnd() {
        testFilters(
            mapOf(
                ":v0" to av(5),
                ":v1" to av("banana"),
                ":v2" to av(null),
            ),
            "(foo = :v0) AND (bar < :v1) AND (baz <> :v2)" to {
                and(
                    attr("foo") eq 5,
                    attr("bar") lt "banana",
                    attr("baz") neq null,
                )
            },
        )
    }

    @Test
    fun testBooleans() {
        listOf(false, true, null).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
                "contains(foo, :v0)" to { attr("foo") contains value },
            )
        }
    }

    @Test
    fun testByteArrays() {
        val b1 = byteArrayOf(1, 2, 3)
        val b2 = byteArrayOf(4, 5, 6)
        val b3 = byteArrayOf(7, 8, 9)

        listOf(b1, b2, b3).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
                "foo < :v0" to { attr("foo") lt value },
                "foo <= :v0" to { attr("foo") lte value },
                "foo > :v0" to { attr("foo") gt value },
                "foo >= :v0" to { attr("foo") gte value },
                "contains(foo, :v0)" to { attr("foo") contains value },
            )
        }

        testFilters(
            mapOf(
                ":v0" to av(b1),
                ":v1" to av(b2),
            ),
            "foo BETWEEN :v0 AND :v1" to { attr("foo").isBetween(b1, b2) },
        )

        (null as ByteArray?).let { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
                "contains(foo, :v0)" to { attr("foo") contains value },
            )
        }
    }

    @Test
    fun testCollectionsOfByteArrays() {
        testFilters(
            mapOf(
                ":v0" to av(byteArrayOf(1, 2, 3)),
                ":v1" to av(byteArrayOf(4, 5, 6)),
                ":v2" to av(byteArrayOf(7, 8, 9)),
            ),
            "foo IN (:v0, :v1, :v2)" to {
                attr("foo") isIn setOf(
                    byteArrayOf(1, 2, 3),
                    byteArrayOf(4, 5, 6),
                    byteArrayOf(7, 8, 9),
                )
            },
        )
    }

    @Test
    fun testCollectionsOfLists() {
        testFilters(
            mapOf(
                ":v0" to av(listOf("apple", false, 1, null)),
                ":v1" to av(listOf("banana", true, 2)),
                ":v2" to av(listOf("cherry", 3)),
                ":v3" to av(null),
            ),
            "foo IN (:v0, :v1, :v2, :v3)" to {
                attr("foo") isIn setOf(
                    listOf("apple", false, 1, null),
                    listOf("banana", true, 2),
                    listOf("cherry", 3),
                    null,
                )
            },
        )
    }

    @Test
    fun testCollectionsOfMaps() {
        testFilters(
            mapOf(
                ":v0" to av(mapOf("a" to "apple", "b" to false, "c" to 1, "d" to null)),
                ":v1" to av(mapOf("e" to "banana", "f" to true, "g" to 2)),
                ":v2" to av(mapOf("h" to "cherry", "i" to 3)),
                ":v3" to av(null),
            ),
            "foo IN (:v0, :v1, :v2, :v3)" to {
                attr("foo") isIn setOf(
                    mapOf("a" to "apple", "b" to false, "c" to 1, "d" to null),
                    mapOf("e" to "banana", "f" to true, "g" to 2),
                    mapOf("h" to "cherry", "i" to 3),
                    null,
                )
            },
        )
    }

    @Test
    fun testCollectionsOfNumbers() {
        testFilters(
            mapOf(
                ":v0" to av(42),
                ":v1" to av(-1_000_000_000_000_000L),
                ":v2" to av(null),
            ),
            "foo IN (:v0, :v1, :v2)" to { attr("foo") isIn setOf(42, -1_000_000_000_000_000L, null) },
        )
    }

    @Test
    fun testCollectionsOfSetsOfByteArrays() {
        // Collections of sets of arrays 😵‍💫

        val sets = listOf(
            setOf(
                byteArrayOf(1, 2, 3),
                byteArrayOf(4, 5, 6),
                byteArrayOf(7, 8, 9),
            ),
            setOf(),
            null,
        )

        testFilters(
            mapOf(
                ":v0" to av(sets[0]),
                ":v1" to av(sets[1]),
                ":v2" to av(sets[2]),
            ),
            "foo IN (:v0, :v1, :v2)" to { attr("foo") isIn sets },
        )
    }

    @Test
    fun testCollectionsOfSetsOfNumbers() {
        val sets = listOf(
            setOf(
                13.toByte(),
                (-42).toShort(),
                -5,
                31_556_952_000L,
                2.71828f,
                3.14159,
            ),
            setOf(),
            null,
        )

        testFilters(
            mapOf(
                ":v0" to av(sets[0]),
                ":v1" to av(sets[1]),
                ":v2" to av(sets[2]),
            ),
            "foo IN (:v0, :v1, :v2)" to { attr("foo") isIn sets },
        )
    }

    @Test
    fun testCollectionsOfSetsOfStrings() {
        val sets = listOf(
            setOf(
                "apple",
                "banana",
                "cherry",
            ),
            setOf(),
            null,
        )

        testFilters(
            mapOf(
                ":v0" to av(sets[0]),
                ":v1" to av(sets[1]),
                ":v2" to av(sets[2]),
            ),
            "foo IN (:v0, :v1, :v2)" to { attr("foo") isIn sets },
        )
    }

    @Test
    fun testCollectionsOfSetsOfUBytes() {
        val sets = listOf(
            setOf(
                UByte.MIN_VALUE,
                42.toUByte(),
                UByte.MAX_VALUE,
            ),
            setOf(),
            null,
        )

        testFilters(
            mapOf(
                ":v0" to av(sets[0]),
                ":v1" to av(sets[1]),
                ":v2" to av(sets[2]),
            ),
            "foo IN (:v0, :v1, :v2)" to { attr("foo") isIn sets },
        )
    }

    @Test
    fun testCollectionsOfSetsOfUInt() {
        val sets = listOf(
            setOf(
                UInt.MIN_VALUE,
                42.toUInt(),
                UInt.MAX_VALUE,
            ),
            setOf(),
            null,
        )

        testFilters(
            mapOf(
                ":v0" to av(sets[0]),
                ":v1" to av(sets[1]),
                ":v2" to av(sets[2]),
            ),
            "foo IN (:v0, :v1, :v2)" to { attr("foo") isIn sets },
        )
    }

    @Test
    fun testCollectionsOfSetsOfULong() {
        val sets = listOf(
            setOf(
                ULong.MIN_VALUE,
                42.toULong(),
                ULong.MAX_VALUE,
            ),
            setOf(),
            null,
        )

        testFilters(
            mapOf(
                ":v0" to av(sets[0]),
                ":v1" to av(sets[1]),
                ":v2" to av(sets[2]),
            ),
            "foo IN (:v0, :v1, :v2)" to { attr("foo") isIn sets },
        )
    }

    @Test
    fun testCollectionsOfSetsOfUShorts() {
        val sets = listOf(
            setOf(
                UShort.MIN_VALUE,
                42.toUShort(),
                UShort.MAX_VALUE,
            ),
            setOf(),
            null,
        )

        testFilters(
            mapOf(
                ":v0" to av(sets[0]),
                ":v1" to av(sets[1]),
                ":v2" to av(sets[2]),
            ),
            "foo IN (:v0, :v1, :v2)" to { attr("foo") isIn sets },
        )
    }

    @Test
    fun testExists() = testFilters(
        "attribute_exists(foo)" to { attr("foo").exists() },
    )

    @Test
    fun testExpressions() {
        testFilters(
            "foo = bar" to { attr("foo") eq attr("bar") },
            "foo <> bar" to { attr("foo") neq attr("bar") },
            "foo < bar" to { attr("foo") lt attr("bar") },
            "foo <= bar" to { attr("foo") lte attr("bar") },
            "foo > bar" to { attr("foo") gt attr("bar") },
            "foo >= bar" to { attr("foo") gte attr("bar") },
            "contains(foo, bar)" to { attr("foo") contains attr("bar") },
            "begins_with(foo, bar)" to { attr("foo") startsWith attr("bar") },
            "foo BETWEEN bar AND baz" to { attr("foo").isBetween(attr("bar"), attr("baz")) },
            "foo IN (bar, baz, qux)" to { attr("foo") isIn listOf(attr("bar"), attr("baz"), attr("qux")) },
        )

        testFilters(
            expectedAVs = null,
            expectedANs = mapOf(
                "#k0" to "Key.With.Dots",
                "#k1" to "And",
                "#k2" to "Or",
            ),
            tests = arrayOf(
                "foo = Attr.#k0.#k1.#k2.Indices[5]" to {
                    attr("foo") eq attr("Attr")["Key.With.Dots"]["And"]["Or"]["Indices"][5]
                },
            ),
        )
    }

    @Test
    fun testIsOfType() =
        testFilters(
            av("S"),
            "attribute_type(foo, :v0)" to { attr("foo") isOfType AttributeType.String },
        )

    @Test
    fun testLists() {
        listOf(
            listOf("apple", false, 1, null),
            listOf("banana", true, 2),
            listOf("cherry", 3),
            null,
        ).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
            )
        }
    }

    @Test
    fun testMaps() {
        listOf(
            mapOf("a" to "apple", "b" to false, "c" to 1, "d" to null),
            mapOf("e" to "banana", "f" to true, "g" to 2),
            mapOf("h" to "cherry", "i" to 3),
            null,
        ).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
            )
        }
    }

    @Test
    fun testNot() = testFilters(
        "NOT (foo = bar)" to { not(attr("foo") eq attr("bar")) },
    )

    @Test
    fun testNotExists() = testFilters(
        "attribute_not_exists(foo)" to { attr("foo").notExists() },
    )

    @Test
    fun testNumbers() {
        listOf(
            13.toByte(),
            (-42).toShort(),
            -5,
            31_556_952_000L,
            2.71828f,
            3.14159,
        ).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
                "foo < :v0" to { attr("foo") lt value },
                "foo <= :v0" to { attr("foo") lte value },
                "foo > :v0" to { attr("foo") gt value },
                "foo >= :v0" to { attr("foo") gte value },
                "contains(foo, :v0)" to { attr("foo") contains value },
            )
        }

        testFilters(
            mapOf(
                ":v0" to av(100),
                ":v1" to av(200),
            ),
            "foo BETWEEN :v0 AND :v1" to { attr("foo") isIn 100..200 },
            "foo IN (:v0, :v1)" to { attr("foo") isIn setOf(100, 200) },
        )

        (null as Number?).let { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
                "contains(foo, :v0)" to { attr("foo") contains value },
            )
        }
    }

    @Test
    fun testOr() {
        testFilters(
            mapOf(
                ":v0" to av(5),
                ":v1" to av("banana"),
                ":v2" to av(null),
            ),
            "(foo > :v0) OR (bar >= :v1) OR (baz = :v2)" to {
                or(
                    attr("foo") gt 5,
                    attr("bar") gte "banana",
                    attr("baz") eq null,
                )
            },
        )
    }

    @Test
    fun testSetsOfByteArrays() {
        listOf(
            setOf(
                byteArrayOf(1, 2, 3),
                byteArrayOf(4, 5, 6),
                byteArrayOf(7, 8, 9),
            ),
            setOf(),
            null,
        ).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
            )
        }
    }

    @Test
    fun testSetsOfNumbers() {
        listOf(
            setOf(
                13.toByte(),
                (-42).toShort(),
                -5,
                31_556_952_000L,
                2.71828f,
                3.14159,
            ),
            setOf(),
            null,
        ).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
            )
        }
    }

    @Test
    fun testSetsOfStrings() {
        listOf(
            setOf(
                "apple",
                "banana",
                "cherry",
            ),
            setOf(),
            null,
        ).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
            )
        }
    }

    @Test
    fun testSetsOfUBytes() {
        listOf(
            setOf(
                UByte.MIN_VALUE,
                42.toUByte(),
                UByte.MAX_VALUE,
            ),
            setOf(),
            null,
        ).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
            )
        }
    }

    @Test
    fun testSetsOfUInts() {
        listOf(
            setOf(
                UInt.MIN_VALUE,
                42.toUInt(),
                UInt.MAX_VALUE,
            ),
            setOf(),
            null,
        ).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
            )
        }
    }

    @Test
    fun testSetsOfULongs() {
        listOf(
            setOf(
                ULong.MIN_VALUE,
                42.toULong(),
                ULong.MAX_VALUE,
            ),
            setOf(),
            null,
        ).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
            )
        }
    }

    @Test
    fun testSetsOfUShorts() {
        listOf(
            setOf(
                UShort.MIN_VALUE,
                42.toUShort(),
                UShort.MAX_VALUE,
            ),
            setOf(),
            null,
        ).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
            )
        }
    }

    @Test
    fun testSize() = testFilters(
        "size(foo)" to { attr("foo").size },
    )

    @Test
    fun testStrings() {
        listOf(
            "apple",
            "banana",
            "cherry",
        ).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
                "foo < :v0" to { attr("foo") lt value },
                "foo <= :v0" to { attr("foo") lte value },
                "foo > :v0" to { attr("foo") gt value },
                "foo >= :v0" to { attr("foo") gte value },
                "contains(foo, :v0)" to { attr("foo") contains value },
                "begins_with(foo, :v0)" to { attr("foo") startsWith value },
            )
        }

        testFilters(
            mapOf(
                ":v0" to av("apple"),
                ":v1" to av("banana"),
            ),
            "foo BETWEEN :v0 AND :v1" to { attr("foo") isIn "apple".."banana" },
            "foo IN (:v0, :v1)" to { attr("foo") isIn setOf("apple", "banana") },
        )

        (null as String?).let { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
                "contains(foo, :v0)" to { attr("foo") contains value },
            )
        }
    }

    @Test
    fun testUBytes() {
        listOf(
            UByte.MIN_VALUE,
            42.toUByte(),
            UByte.MAX_VALUE,
        ).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
                "foo < :v0" to { attr("foo") lt value },
                "foo <= :v0" to { attr("foo") lte value },
                "foo > :v0" to { attr("foo") gt value },
                "foo >= :v0" to { attr("foo") gte value },
            )
        }

        testFilters(
            mapOf(
                ":v0" to av(100.toUByte()),
                ":v1" to av(200.toUByte()),
            ),
            "foo BETWEEN :v0 AND :v1" to { attr("foo") isIn UByteRange(100.toUByte(), 200.toUByte()) },
            "foo IN (:v0, :v1)" to { attr("foo") isIn setOf(100.toUByte(), 200.toUByte()) },
        )

        (null as UByte?).let { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
            )
        }
    }

    @Test
    fun testUInts() {
        listOf(
            UInt.MIN_VALUE,
            42.toUInt(),
            UInt.MAX_VALUE,
        ).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
                "foo < :v0" to { attr("foo") lt value },
                "foo <= :v0" to { attr("foo") lte value },
                "foo > :v0" to { attr("foo") gt value },
                "foo >= :v0" to { attr("foo") gte value },
            )
        }

        testFilters(
            mapOf(
                ":v0" to av(100.toUInt()),
                ":v1" to av(200.toUInt()),
            ),
            "foo BETWEEN :v0 AND :v1" to { attr("foo") isIn 100.toUInt().rangeTo(200.toUInt()) },
            "foo IN (:v0, :v1)" to { attr("foo") isIn setOf(100.toUInt(), 200.toUInt()) },
        )

        (null as UInt?).let { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
            )
        }
    }

    @Test
    fun testULongs() {
        listOf(
            ULong.MIN_VALUE,
            42.toULong(),
            ULong.MAX_VALUE,
        ).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
                "foo < :v0" to { attr("foo") lt value },
                "foo <= :v0" to { attr("foo") lte value },
                "foo > :v0" to { attr("foo") gt value },
                "foo >= :v0" to { attr("foo") gte value },
            )
        }

        testFilters(
            mapOf(
                ":v0" to av(100.toULong()),
                ":v1" to av(200.toULong()),
            ),
            "foo BETWEEN :v0 AND :v1" to { attr("foo") isIn 100.toULong().rangeTo(200.toULong()) },
            "foo IN (:v0, :v1)" to { attr("foo") isIn setOf(100.toULong(), 200.toULong()) },
        )

        (null as ULong?).let { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
            )
        }
    }

    @Test
    fun testUShorts() {
        listOf(
            UShort.MIN_VALUE,
            42.toUShort(),
            UShort.MAX_VALUE,
        ).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
                "foo < :v0" to { attr("foo") lt value },
                "foo <= :v0" to { attr("foo") lte value },
                "foo > :v0" to { attr("foo") gt value },
                "foo >= :v0" to { attr("foo") gte value },
            )
        }

        testFilters(
            mapOf(
                ":v0" to av(100.toUShort()),
                ":v1" to av(200.toUShort()),
            ),
            "foo BETWEEN :v0 AND :v1" to { attr("foo") isIn UShortRange(100.toUShort(), 200.toUShort()) },
            "foo IN (:v0, :v1)" to { attr("foo") isIn setOf(100.toUShort(), 200.toUShort()) },
        )

        (null as UShort?).let { value ->
            testFilters(
                av(value),
                "foo = :v0" to { attr("foo") eq value },
                "foo <> :v0" to { attr("foo") neq value },
            )
        }
    }

    private fun testFilters(vararg tests: Pair<String, FilterDsl.() -> Expression>) {
        testFilters(null, *tests)
    }

    private fun testFilters(expectedAV: AttributeValue, vararg tests: Pair<String, FilterDsl.() -> Expression>) =
        testFilters(mapOf(":v0" to expectedAV), *tests)

    private fun testFilters(
        expectedAVs: Map<String, AttributeValue>?,
        vararg tests: Pair<String, FilterDsl.() -> Expression>,
        expectedANs: Map<String, String>? = null,
    ) = tests.forEach { (expectedExprString, block) ->
        val parameterizer = ParameterizingExpressionVisitor()
        val expr = FilterDslImpl.block()
        val actualExprString = expr.accept(parameterizer)

        assertEquals(expectedExprString, actualExprString)

        val actualAVs = parameterizer.expressionAttributeValues()
        assertEquals(expectedAVs, actualAVs)

        val actualANs = parameterizer.expressionAttributeNames()
        assertEquals(expectedANs, actualANs)
    }
}
