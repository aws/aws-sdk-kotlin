/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.AttrImpl
import kotlin.test.Test
import kotlin.test.assertEquals

class AttributePathTest {
    @Test
    fun testToString() {
        val testCases = mapOf(
            AttrImpl["foo"] to "foo",
            AttrImpl["foo"]["bar"] to "foo.bar",
            AttrImpl["foo"][1] to "foo[1]",
            AttrImpl["foo"][1]["bar"] to "foo[1].bar",
        )
        testCases.entries.forEach { (attr, expected) -> assertEquals(expected, attr.toString()) }
    }
}
