/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.ParameterizingExpressionVisitor
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.SkAttrPathImpl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.SortKeyFilterDslImpl
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.UByteRange
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.UShortRange
import aws.sdk.kotlin.hll.dynamodbmapper.util.av
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import kotlin.test.Test
import kotlin.test.assertEquals

class SortKeyFilterDslTest {
    @Test
    fun testByteArrays() {
        val b1 = byteArrayOf(1, 2, 3)
        val b2 = byteArrayOf(4, 5, 6)
        val b3 = byteArrayOf(7, 8, 9)

        listOf(b1, b2, b3).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { sortKey eq value },
                "foo <> :v0" to { sortKey neq value },
                "foo < :v0" to { sortKey lt value },
                "foo <= :v0" to { sortKey lte value },
                "foo > :v0" to { sortKey gt value },
                "foo >= :v0" to { sortKey gte value },
                "begins_with(foo, :v0)" to { sortKey startsWith value },
            )
        }

        testFilters(
            mapOf(
                ":v0" to av(b1),
                ":v1" to av(b2),
            ),
            "foo BETWEEN :v0 AND :v1" to { sortKey.isBetween(b1, b2) },
        )
    }

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
                "foo = :v0" to { sortKey eq value },
                "foo <> :v0" to { sortKey neq value },
                "foo < :v0" to { sortKey lt value },
                "foo <= :v0" to { sortKey lte value },
                "foo > :v0" to { sortKey gt value },
                "foo >= :v0" to { sortKey gte value },
            )
        }

        testFilters(
            mapOf(
                ":v0" to av(100),
                ":v1" to av(200),
            ),
            "foo BETWEEN :v0 AND :v1" to { sortKey isIn 100..200 },
        )
    }

    @Test
    fun testStrings() {
        listOf(
            "apple",
            "banana",
            "cherry",
        ).forEach { value ->
            testFilters(
                av(value),
                "foo = :v0" to { sortKey eq value },
                "foo <> :v0" to { sortKey neq value },
                "foo < :v0" to { sortKey lt value },
                "foo <= :v0" to { sortKey lte value },
                "foo > :v0" to { sortKey gt value },
                "foo >= :v0" to { sortKey gte value },
                "begins_with(foo, :v0)" to { sortKey startsWith value },
            )
        }

        testFilters(
            mapOf(
                ":v0" to av("apple"),
                ":v1" to av("banana"),
            ),
            "foo BETWEEN :v0 AND :v1" to { sortKey isIn "apple".."banana" },
        )
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
                "foo = :v0" to { sortKey eq value },
                "foo <> :v0" to { sortKey neq value },
                "foo < :v0" to { sortKey lt value },
                "foo <= :v0" to { sortKey lte value },
                "foo > :v0" to { sortKey gt value },
                "foo >= :v0" to { sortKey gte value },
            )
        }

        testFilters(
            mapOf(
                ":v0" to av(100.toUByte()),
                ":v1" to av(200.toUByte()),
            ),
            "foo BETWEEN :v0 AND :v1" to { sortKey isIn UByteRange(100.toUByte(), 200.toUByte()) },
        )
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
                "foo = :v0" to { sortKey eq value },
                "foo <> :v0" to { sortKey neq value },
                "foo < :v0" to { sortKey lt value },
                "foo <= :v0" to { sortKey lte value },
                "foo > :v0" to { sortKey gt value },
                "foo >= :v0" to { sortKey gte value },
            )
        }

        testFilters(
            mapOf(
                ":v0" to av(100.toUInt()),
                ":v1" to av(200.toUInt()),
            ),
            "foo BETWEEN :v0 AND :v1" to { sortKey isIn 100.toUInt().rangeTo(200.toUInt()) },
        )
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
                "foo = :v0" to { sortKey eq value },
                "foo <> :v0" to { sortKey neq value },
                "foo < :v0" to { sortKey lt value },
                "foo <= :v0" to { sortKey lte value },
                "foo > :v0" to { sortKey gt value },
                "foo >= :v0" to { sortKey gte value },
            )
        }

        testFilters(
            mapOf(
                ":v0" to av(100.toULong()),
                ":v1" to av(200.toULong()),
            ),
            "foo BETWEEN :v0 AND :v1" to { sortKey isIn 100.toULong().rangeTo(200.toULong()) },
        )
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
                "foo = :v0" to { sortKey eq value },
                "foo <> :v0" to { sortKey neq value },
                "foo < :v0" to { sortKey lt value },
                "foo <= :v0" to { sortKey lte value },
                "foo > :v0" to { sortKey gt value },
                "foo >= :v0" to { sortKey gte value },
            )
        }

        testFilters(
            mapOf(
                ":v0" to av(100.toUShort()),
                ":v1" to av(200.toUShort()),
            ),
            "foo BETWEEN :v0 AND :v1" to { sortKey isIn UShortRange(100.toUShort(), 200.toUShort()) },
        )
    }

    private fun testFilters(expectedAV: AttributeValue, vararg tests: Pair<String, SortKeyFilterDsl.() -> SortKeyExpr>) = testFilters(mapOf(":v0" to expectedAV), *tests)

    private fun testFilters(
        expectedAVs: Map<String, AttributeValue>?,
        vararg tests: Pair<String, SortKeyFilterDsl.() -> SortKeyExpr>,
        expectedANs: Map<String, String>? = null,
    ) = tests.forEach { (expectedExprString, block) ->
        val parameterizer = SortKeyExpressionVisitor()
        val expr = SortKeyFilterDslImpl.block()
        val actualExprString = expr.accept(parameterizer)

        assertEquals(expectedExprString, actualExprString)

        val actualAVs = parameterizer.expressionAttributeValues()
        assertEquals(expectedAVs, actualAVs)

        val actualANs = parameterizer.expressionAttributeNames()
        assertEquals(expectedANs, actualANs)
    }
}

private class SortKeyExpressionVisitor : ParameterizingExpressionVisitor() {
    override fun visit(expr: AttributePath) = when (expr) {
        SkAttrPathImpl -> "foo" // Swap out dummy attr path for "foo" (normally KeyFilter.toExpression would do this)
        else -> super.visit(expr)
    }
}
