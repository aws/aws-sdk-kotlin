/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.ParameterizingExpressionVisitor
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.UpdateDslImpl
import aws.sdk.kotlin.hll.dynamodbmapper.util.av
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateDslTest {
    @Test
    fun testBooleans() {
        listOf(false, true, null).forEach { value ->
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
            )
        }
    }

    @Test
    fun testByteArrays() {
        val b1 = byteArrayOf(1, 2, 3)
        val b2 = byteArrayOf(4, 5, 6)
        val b3 = byteArrayOf(7, 8, 9)

        listOf(b1, b2, b3).forEach { value ->
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
            )
        }
    }

    @Test
    fun testExpressions() {
        testUpdates(
            "SET foo = oof, bar[0] = rab[0], baz.qux = zab.xuq, nox = xon + onx, pix = xip - ipx, moo = if_not_exists(oom, omo) ADD bur rub DELETE zip piz" to {
                set {
                    attr["foo"] = attr["oof"]
                    attr["bar"][0] = attr["rab"][0]
                    attr["baz"]["qux"] = attr["zab"]["xuq"]
                    attr["nox"] = attr["xon"] + attr["onx"]
                    attr["pix"] = attr["xip"] - attr["ipx"]
                    attr["moo"] = attr["oom"] orElse attr["omo"]
                }
                add {
                    attr["bur"] += attr["rub"]
                }
                delete {
                    attr["zip"] -= attr["piz"]
                }
            },
        )
    }

    @Test
    fun testLists() {
        listOf(
            listOf("apple", false, 1, null),
            listOf("banana", true, 2),
            listOf("cherry", 3),
            null,
        ).forEach { value ->
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
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
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
            )
        }
    }

    @Test
    fun testNull() {
        testUpdates(
            av(null),
            "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                set {
                    attr["foo"] = null
                    attr["bar"][2] = null
                    attr["baz"]["qux"] = null
                    attr["nox"] = attr["nox"] orElse null
                }
            },
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
            null,
        ).forEach { value ->
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
            )

            if (value != null) {
                testUpdates(
                    av(value),
                    "SET foo = bar + :v0, baz = qux - :v0, nox[0] = nox[0] + :v0, nox[1] = nox[1] - :v0 ADD xon :v0" to {
                        set {
                            attr["foo"] = attr["bar"] + value
                            attr["baz"] = attr["qux"] - value
                            attr["nox"][0] += value
                            attr["nox"][1] -= value
                        }
                        add {
                            attr["xon"] += value
                        }
                    },
                )
            }
        }
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
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
            )

            if (value != null) {
                testUpdates(
                    av(value),
                    "ADD foo :v0 DELETE bar :v0" to {
                        add {
                            attr["foo"] += value
                        }
                        delete {
                            attr["bar"] -= value
                        }
                    },
                )
            }
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
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
            )

            if (value != null) {
                testUpdates(
                    av(value),
                    "ADD foo :v0 DELETE bar :v0" to {
                        add {
                            attr["foo"] += value
                        }
                        delete {
                            attr["bar"] -= value
                        }
                    },
                )
            }
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
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
            )

            if (value != null) {
                testUpdates(
                    av(value),
                    "ADD foo :v0 DELETE bar :v0" to {
                        add {
                            attr["foo"] += value
                        }
                        delete {
                            attr["bar"] -= value
                        }
                    },
                )
            }
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
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
            )

            if (value != null) {
                testUpdates(
                    av(value),
                    "ADD foo :v0 DELETE bar :v0" to {
                        add {
                            attr["foo"] += value
                        }
                        delete {
                            attr["bar"] -= value
                        }
                    },
                )
            }
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
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
            )

            if (value != null) {
                testUpdates(
                    av(value),
                    "ADD foo :v0 DELETE bar :v0" to {
                        add {
                            attr["foo"] += value
                        }
                        delete {
                            attr["bar"] -= value
                        }
                    },
                )
            }
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
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
            )

            if (value != null) {
                testUpdates(
                    av(value),
                    "ADD foo :v0 DELETE bar :v0" to {
                        add {
                            attr["foo"] += value
                        }
                        delete {
                            attr["bar"] -= value
                        }
                    },
                )
            }
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
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
            )

            if (value != null) {
                testUpdates(
                    av(value),
                    "ADD foo :v0 DELETE bar :v0" to {
                        add {
                            attr["foo"] += value
                        }
                        delete {
                            attr["bar"] -= value
                        }
                    },
                )
            }
        }
    }

    @Test
    fun testStrings() {
        listOf(
            "apple",
            "banana",
            "cherry",
        ).forEach { value ->
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
            )
        }
    }

    @Test
    fun testUBytes() {
        listOf(
            UByte.MIN_VALUE,
            42.toUByte(),
            UByte.MAX_VALUE,
            null,
        ).forEach { value ->
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
            )

            if (value != null) {
                testUpdates(
                    av(value),
                    "SET foo = bar + :v0, baz = qux - :v0, nox[0] = nox[0] + :v0, nox[1] = nox[1] - :v0 ADD xon :v0" to {
                        set {
                            attr["foo"] = attr["bar"] + value
                            attr["baz"] = attr["qux"] - value
                            attr["nox"][0] += value
                            attr["nox"][1] -= value
                        }
                        add {
                            attr["xon"] += value
                        }
                    },
                )
            }
        }
    }

    @Test
    fun testUInts() {
        listOf(
            UInt.MIN_VALUE,
            42.toUInt(),
            UInt.MAX_VALUE,
            null,
        ).forEach { value ->
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
            )

            if (value != null) {
                testUpdates(
                    av(value),
                    "SET foo = bar + :v0, baz = qux - :v0, nox[0] = nox[0] + :v0, nox[1] = nox[1] - :v0 ADD xon :v0" to {
                        set {
                            attr["foo"] = attr["bar"] + value
                            attr["baz"] = attr["qux"] - value
                            attr["nox"][0] += value
                            attr["nox"][1] -= value
                        }
                        add {
                            attr["xon"] += value
                        }
                    },
                )
            }
        }
    }

    @Test
    fun testULongs() {
        listOf(
            ULong.MIN_VALUE,
            42.toULong(),
            ULong.MAX_VALUE,
            null,
        ).forEach { value ->
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
            )

            if (value != null) {
                testUpdates(
                    av(value),
                    "SET foo = bar + :v0, baz = qux - :v0, nox[0] = nox[0] + :v0, nox[1] = nox[1] - :v0 ADD xon :v0" to {
                        set {
                            attr["foo"] = attr["bar"] + value
                            attr["baz"] = attr["qux"] - value
                            attr["nox"][0] += value
                            attr["nox"][1] -= value
                        }
                        add {
                            attr["xon"] += value
                        }
                    },
                )
            }
        }
    }

    @Test
    fun testUShorts() {
        listOf(
            UShort.MIN_VALUE,
            42.toUShort(),
            UShort.MAX_VALUE,
            null,
        ).forEach { value ->
            testUpdates(
                av(value),
                "SET foo = :v0, bar[2] = :v0, baz.qux = :v0, nox = if_not_exists(nox, :v0)" to {
                    set {
                        attr["foo"] = value
                        attr["bar"][2] = value
                        attr["baz"]["qux"] = value
                        attr["nox"] = attr["nox"] orElse value
                    }
                },
            )

            if (value != null) {
                testUpdates(
                    av(value),
                    "SET foo = bar + :v0, baz = qux - :v0, nox[0] = nox[0] + :v0, nox[1] = nox[1] - :v0 ADD xon :v0" to {
                        set {
                            attr["foo"] = attr["bar"] + value
                            attr["baz"] = attr["qux"] - value
                            attr["nox"][0] += value
                            attr["nox"][1] -= value
                        }
                        add {
                            attr["xon"] += value
                        }
                    },
                )
            }
        }
    }

    private fun testUpdates(vararg tests: Pair<String, UpdateDsl.() -> Unit>) {
        testUpdates(null, *tests)
    }

    private fun testUpdates(expectedAV: AttributeValue, vararg tests: Pair<String, UpdateDsl.() -> Unit>) = testUpdates(mapOf(":v0" to expectedAV), *tests)

    private fun testUpdates(
        expectedAVs: Map<String, AttributeValue>?,
        vararg tests: Pair<String, UpdateDsl.() -> Unit>,
        expectedANs: Map<String, String>? = null,
    ) = tests.forEach { (expectedExprString, block) ->
        val parameterizer = ParameterizingExpressionVisitor()
        val expr = UpdateDslImpl().apply(block).toExpression()
        val actualExprString = expr.accept(parameterizer)

        assertEquals(expectedExprString, actualExprString)

        val actualAVs = parameterizer.expressionAttributeValues()
        assertEquals(expectedAVs, actualAVs)

        val actualANs = parameterizer.expressionAttributeNames()
        assertEquals(expectedANs, actualANs)
    }
}
