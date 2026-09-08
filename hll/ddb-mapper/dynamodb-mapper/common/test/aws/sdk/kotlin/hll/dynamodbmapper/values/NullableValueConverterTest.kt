/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterChain
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterImpl
import kotlin.test.Test

class NullableValueConverterTest : ValueConvertersTest() {
    @Test
    fun testNullConverter() = given(NullableValueConverter(stringReverseConverter)) {
        "foo" inDdbIs "oof"
        "bar" inDdbIs "rab"
        null inDdbIs theSame
        "null" inDdbIs "llun"
    }
}

private val stringReverseConverter = ConverterChain(
    ConverterImpl(String::reversed, String::reversed),
    StringValueConverter,
)
