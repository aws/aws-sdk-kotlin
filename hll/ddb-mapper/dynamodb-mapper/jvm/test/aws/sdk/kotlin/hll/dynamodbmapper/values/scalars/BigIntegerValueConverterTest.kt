/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConvertersTest
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import java.math.BigInteger
import kotlin.test.Test

class BigIntegerValueConverterTest : ValueConvertersTest() {
    @Test
    fun testBigIntegerConverter() = given(BigIntegerValueConverter) {
        BigInteger("0") inDdbIs AttributeValue.N("0")
        BigInteger("-42") inDdbIs AttributeValue.N("-42")
        BigInteger("99999999999999999999999999999999999999") inDdbIs
            AttributeValue.N("99999999999999999999999999999999999999")
    }
}
