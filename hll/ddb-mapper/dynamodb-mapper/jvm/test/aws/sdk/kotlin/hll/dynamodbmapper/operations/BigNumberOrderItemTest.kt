/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.AttributeDescriptor
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.SimpleItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.DdbLocalTest
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.getItem
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.BigDecimalValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.BigIntegerValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.testing.BeforeAll
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private data class Order(
    val id: String,
    val itemsCount: BigInteger,
    val totalPrice: BigDecimal,
)

private class OrderBuilder {
    var id: String? = null
    var itemsCount: BigInteger? = null
    var totalPrice: BigDecimal? = null

    fun build() = Order(id!!, itemsCount!!, totalPrice!!)
}

private val schema = ItemSchema(
    SimpleItemConverter(
        ::OrderBuilder,
        OrderBuilder::build,
        AttributeDescriptor("id", Order::id, OrderBuilder::id::set, StringValueConverter),
        AttributeDescriptor("itemsCount", Order::itemsCount, OrderBuilder::itemsCount::set, BigIntegerValueConverter),
        AttributeDescriptor("totalPrice", Order::totalPrice, OrderBuilder::totalPrice::set, BigDecimalValueConverter),
    ),
    KeySpec.string("id"),
)

class BigNumberOrderItemTest : DdbLocalTest() {
    companion object {
        private const val TABLE_NAME = "big-number-order-item-test"
    }

    @BeforeAll
    fun setUp() = runTest {
        createTable(TABLE_NAME, schema)
    }

    @Test
    fun testPutAndGetOrder() = runTest {
        val table = mapper().getTable(TABLE_NAME, schema)

        val order = Order(
            id = "order-001",
            itemsCount = BigInteger("500000"),
            totalPrice = BigDecimal("19.90"),
        )
        table.putItem { item = order }

        // DynamoDB trims trailing zeroes on the `N` type, so 19.90 is stored and read back as 19.9; see
        // https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number
        // java.math.BigDecimal equality is scale-sensitive (19.90 != 19.9), so the trimming is observable here; the
        // smithy content.BigDecimal type normalizes scale in equals (19.90 == 19.9), so the same round trip would
        // compare equal to the original. The stored value is identical for both: DynamoDB stores 19.9 either way.
        val roundTripped = assertNotNull(table.getItem("order-001").item)
        assertEquals(BigInteger("500000"), roundTripped.itemsCount)
        assertEquals(BigDecimal("19.9"), roundTripped.totalPrice)

        val stored = assertNotNull(lowLevelAccess { getItem(TABLE_NAME, "id" to "order-001") }.item)
        assertEquals(AttributeValue.N("500000"), stored["itemsCount"])
        assertEquals(AttributeValue.N("19.9"), stored["totalPrice"])
    }
}
