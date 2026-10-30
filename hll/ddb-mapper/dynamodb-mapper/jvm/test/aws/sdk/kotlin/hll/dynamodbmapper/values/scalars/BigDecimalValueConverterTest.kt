/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConvertersTest
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import java.math.BigDecimal
import kotlin.test.Test

class BigDecimalValueConverterTest : ValueConvertersTest() {
    @Test
    fun testBigDecimalConverter() = given(BigDecimalValueConverter) {
        BigDecimal("0") inDdbIs AttributeValue.N("0")
        BigDecimal("-1.41421") inDdbIs AttributeValue.N("-1.41421")
        BigDecimal("3.141592653589793238462643383279502884") inDdbIs
            AttributeValue.N("3.141592653589793238462643383279502884")

        // The converter preserves the exact decimal string, including trailing zeroes and plain (non-exponent) form.
        // DynamoDB itself trims trailing zeroes on the `N` type, but that happens server-side, not in the converter.
        // Note: java.math.BigDecimal equality is scale-sensitive (19.90 != 19.9), unlike smithy content.BigDecimal
        // whose equality normalizes scale (19.90 == 19.9). The value stored in DynamoDB is identical for both types.
        BigDecimal("19.90") inDdbIs AttributeValue.N("19.90") whenGoing Direction.TO_ATTRIBUTE_VALUE
        BigDecimal("1E+10") inDdbIs AttributeValue.N("10000000000") whenGoing Direction.TO_ATTRIBUTE_VALUE
    }
}
