/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.util

import kotlin.test.Test
import kotlin.test.assertEquals

class StringsTest {
    @Test
    fun testEscape() {
        assertEquals("Control char \\t escaped?", "Control char \t escaped?".escape())
        assertEquals("Control char \\\\ escaped?", "Control char \\ escaped?".escape())

        assertEquals("Control char \\u0000 escaped?", "Control char ${Character.toString(0)} escaped?".escape())
        assertEquals("Control char \\u0080 escaped?", "Control char ${Character.toString(128)} escaped?".escape())

        assertEquals("Unescaped ñ ™ € ⭐ 🏆", "Unescaped ñ ™ € ⭐ 🏆".escape())
    }

    @Test
    fun testQuote() {
        assertEquals("\"This \\t is a \\\"test\\\"!\"", "This \t is a \"test\"!".quote())
    }
}
