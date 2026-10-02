/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConvertersTest
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.content.BigInteger
import kotlin.test.Test

class BigIntegerSetValueConverterTest : ValueConvertersTest() {
    @Test
    fun testBigIntegerSetConverter() = given(BigIntegerSetValueConverter) {
        setOf(BigInteger("0"), BigInteger("99999999999999999999999999999999999999")) inDdbIs
            AttributeValue.Ns(listOf("0", "99999999999999999999999999999999999999"))
    }
}
