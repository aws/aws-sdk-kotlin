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
    }
}
