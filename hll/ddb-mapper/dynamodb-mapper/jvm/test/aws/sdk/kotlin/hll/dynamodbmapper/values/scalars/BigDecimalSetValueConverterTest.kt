/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConvertersTest
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import java.math.BigDecimal
import kotlin.test.Test

class BigDecimalSetValueConverterTest : ValueConvertersTest() {
    @Test
    fun testBigDecimalSetConverter() = given(BigDecimalSetValueConverter) {
        setOf(BigDecimal("-1.5"), BigDecimal("3.141592653589793238462643383279502884")) inDdbIs
            AttributeValue.Ns(listOf("-1.5", "3.141592653589793238462643383279502884"))
    }
}
