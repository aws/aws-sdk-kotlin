/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TemplateEngineTest {
    private val reverser = TemplateProcessor.typed<String>('R') { it.reversed() }
    private val numbersToWords = TemplateProcessor.typed<Int>('N') {
        it
            .toString()
            .replace("-", "Negative")
            .replace("0", "Zero")
            .replace("1", "One")
            .replace("2", "Two")
            .replace("3", "Three")
            .replace("4", "Four")
            .replace("5", "Five")
            .replace("6", "Six")
            .replace("7", "Seven")
            .replace("8", "Eight")
            .replace("9", "Nine")
    }

    private val engine = TemplateEngine(listOf(TemplateProcessor.Literal, reverser, numbersToWords))

    private fun test(expected: String, template: String, vararg args: Any) {
        val actual = engine.process(template, args.toList())
        assertEquals(expected, actual)
    }

    @Test
    fun testSimple() {
        test("Parameter-less test", "Parameter-less test")
        test("Reversed 'foo' is 'oof'", "Reversed 'foo' is '#R'", "foo")
        test("Favorite number = OneTwoThree", "Favorite number = #N", 123)
        test("Mixed: yemilb, FourTwo", "Mixed: #R, #N", "blimey", 42)
    }

    @Test
    fun testIndexed() {
        test("Repeated: abc, abc, abc, def, abc, def", "Repeated: #1L, #1L, #1L, #2L, #1L, #2L", "abc", "def")

        assertFailsWith<IllegalArgumentException> {
            test("n/a", "This should fail: #1L #2L #L #3L", "a", "b", "c")
        }

        assertFailsWith<IllegalArgumentException> {
            test("n/a", "This should fail: #L #L #1L #L", "a", "b", "c")
        }
    }
}
