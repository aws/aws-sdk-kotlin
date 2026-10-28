/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.KeyFilter
import aws.sdk.kotlin.hll.dynamodbmapper.items.AttributeDescriptor
import aws.sdk.kotlin.hll.dynamodbmapper.items.HeterogeneousItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.Key
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.SimpleItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.model.SchemaAttributes
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.DdbLocalTest
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.getItem
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.putItem
import aws.smithy.kotlin.runtime.collections.attributesOf
import aws.smithy.kotlin.runtime.testing.BeforeAll
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Exercises a heterogeneous table schema through the full mapper pipeline (including the TTL and counter interceptors)
 * against DynamoDB Local. The schema mirrors what codegen emits for a `@DynamoDbHeterogeneousType` table family whose
 * members all declare the same TTL and counter attributes.
 */
class HeterogeneousItemTest : DdbLocalTest() {
    companion object {
        private const val TABLE_NAME = "heterogeneous-item-test"
        private const val TYPE_ATTRIBUTE = "entityType"
        private const val LIFETIME = 3600L

        private sealed interface Entity {
            val pk: String
            val sk: String
        }

        private data class User(
            override var pk: String = "",
            override var sk: String = "",
            var name: String = "",
            var version: Long = 0,
            var expiresAt: Long = 0,
        ) : Entity

        private data class Order(
            override var pk: String = "",
            override var sk: String = "",
            var total: Long = 0,
            var version: Long = 0,
            var expiresAt: Long = 0,
        ) : Entity

        private val userConverter = SimpleItemConverter(
            ::User,
            { this },
            AttributeDescriptor("pk", User::pk, User::pk::set, StringValueConverter),
            AttributeDescriptor("sk", User::sk, User::sk::set, StringValueConverter),
            AttributeDescriptor("name", User::name, User::name::set, StringValueConverter),
            AttributeDescriptor("version", User::version, User::version::set, NumberValueConverters.Long),
            AttributeDescriptor("expiresAt", User::expiresAt, User::expiresAt::set, NumberValueConverters.Long),
        )

        private val orderConverter = SimpleItemConverter(
            ::Order,
            { this },
            AttributeDescriptor("pk", Order::pk, Order::pk::set, StringValueConverter),
            AttributeDescriptor("sk", Order::sk, Order::sk::set, StringValueConverter),
            AttributeDescriptor("total", Order::total, Order::total::set, NumberValueConverters.Long),
            AttributeDescriptor("version", Order::version, Order::version::set, NumberValueConverters.Long),
            AttributeDescriptor("expiresAt", Order::expiresAt, Order::expiresAt::set, NumberValueConverters.Long),
        )

        private val converter = HeterogeneousItemConverter<Entity>(typeAttribute = TYPE_ATTRIBUTE) {
            exactType("order", orderConverter)
            exactType("user", userConverter)
        }

        private val ttlFields = mapOf("expiresAt" to LIFETIME)

        private val schema = ItemSchema(
            converter,
            KeySpec.string("pk"),
            KeySpec.string("sk"),
            attributesOf {
                SchemaAttributes.TtlFields to ttlFields
                SchemaAttributes.CounterFields to setOf("version")
            },
        )
    }

    @BeforeAll
    fun setUp() = runTest {
        createTable(TABLE_NAME, schema)
    }

    @Test
    fun testPutAndGetEachSubtype() = runTest {
        val table = mapper().getTable(TABLE_NAME, schema)
        val before = Instant.now().epochSeconds

        table.putItem { item = User("USER#put", "PROFILE", "Alice") }
        table.putItem { item = Order("USER#put", "ORDER#1", 1999) }

        val after = Instant.now().epochSeconds

        listOf("PROFILE" to "user", "ORDER#1" to "order").forEach { (sk, typeName) ->
            val raw = assertNotNull(lowLevelAccess { getItem(TABLE_NAME, "pk" to "USER#put", "sk" to sk) }.item)
            assertEquals(AttributeValue.S(typeName), raw[TYPE_ATTRIBUTE])
            assertEquals(1L, raw["version"]?.asN()?.toLong())
            assertExpiresBetween(before, after, raw)
        }

        val user = assertIs<User>(table.getItem("USER#put", "PROFILE").item)
        assertEquals("Alice", user.name)
        assertEquals(1L, user.version)

        val order = assertIs<Order>(table.getItem("USER#put", "ORDER#1").item)
        assertEquals(1999L, order.total)
        assertEquals(1L, order.version)
    }

    @Test
    fun testQueryReturnsMixedSubtypes() = runTest {
        val table = mapper().getTable(TABLE_NAME, schema)
        table.putItem { item = User("USER#query", "PROFILE", "Bob") }
        table.putItem { item = Order("USER#query", "ORDER#1", 500) }
        table.putItem { item = Order("USER#query", "ORDER#2", 750) }

        val items = table.queryPaginated { keyCondition = KeyFilter("USER#query") }.items().toList()

        assertEquals(listOf("ORDER#1", "ORDER#2", "PROFILE"), items.map { it.sk })
        assertEquals(listOf(Order::class, Order::class, User::class), items.map { it::class })
        assertEquals(listOf(500L, 750L), items.filterIsInstance<Order>().map { it.total })
    }

    @Test
    fun testUpdateItemAppliesSharedTtlAndCounter() = runTest {
        val table = mapper().getTable(TABLE_NAME, schema)
        table.putItem { item = User("USER#update", "PROFILE", "Carol") }

        val before = Instant.now().epochSeconds
        table.updateItem {
            partitionKey = Key("USER#update")
            sortKey = Key("PROFILE")
            update {
                set { attr["name"] = "Carol B" }
            }
        }
        val after = Instant.now().epochSeconds

        val raw = assertNotNull(lowLevelAccess { getItem(TABLE_NAME, "pk" to "USER#update", "sk" to "PROFILE") }.item)
        assertEquals(AttributeValue.S("user"), raw[TYPE_ATTRIBUTE])
        assertEquals(2L, raw["version"]?.asN()?.toLong())
        assertExpiresBetween(before, after, raw)

        val user = assertIs<User>(table.getItem("USER#update", "PROFILE").item)
        assertEquals("Carol B", user.name)
    }

    @Test
    fun testBatchWriteMixedSubtypes() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, schema)
        val before = Instant.now().epochSeconds

        mapper.batchWriteItem {
            table(table) {
                putItem(User("USER#batch", "PROFILE", "Dave"))
                putItem(Order("USER#batch", "ORDER#1", 42))
            }
        }

        val after = Instant.now().epochSeconds

        val raws = tableItems(TABLE_NAME).filter { it["pk"] == AttributeValue.S("USER#batch") }
        assertEquals(setOf("user", "order"), raws.map { it[TYPE_ATTRIBUTE]?.asS() }.toSet())
        raws.forEach { raw ->
            assertEquals(1L, raw["version"]?.asN()?.toLong())
            assertExpiresBetween(before, after, raw)
        }
    }

    @Test
    fun testTransactWriteMixedSubtypes() = runTest {
        val mapper = mapper()
        val table = mapper.getTable(TABLE_NAME, schema)

        mapper.transactWriteItems {
            table(table) {
                put(User("USER#transact", "PROFILE", "Erin"))
                put(Order("USER#transact", "ORDER#1", 7))
            }
        }

        val items = table.queryPaginated { keyCondition = KeyFilter("USER#transact") }.items().toList()
        assertEquals(listOf(Order::class, User::class), items.map { it::class })
        val versions = items.map {
            when (it) {
                is Order -> it.version
                is User -> it.version
            }
        }
        assertEquals(listOf(1L, 1L), versions)
    }

    @Test
    fun testUnknownTypeFailsOnRead() = runTest {
        lowLevelAccess {
            putItem {
                tableName = TABLE_NAME
                item = mapOf(
                    "pk" to AttributeValue.S("USER#unknown"),
                    "sk" to AttributeValue.S("GHOST"),
                    TYPE_ATTRIBUTE to AttributeValue.S("ghost"),
                )
            }
        }

        val table = mapper().getTable(TABLE_NAME, schema)
        assertFailsWith<IllegalArgumentException> { table.getItem("USER#unknown", "GHOST") }
    }

    private fun assertExpiresBetween(before: Long, after: Long, raw: Map<String, AttributeValue>) {
        val expiresAt = assertNotNull(raw["expiresAt"]?.asN()?.toLong(), "Missing expiresAt in $raw")
        assertTrue(expiresAt in (before + LIFETIME)..(after + LIFETIME), "Unexpected expiresAt $expiresAt in $raw")
    }
}
