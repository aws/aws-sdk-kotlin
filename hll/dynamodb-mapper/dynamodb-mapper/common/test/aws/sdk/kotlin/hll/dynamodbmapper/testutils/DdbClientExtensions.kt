/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.testutils

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyAttrSpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.SimpleItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.attrs
import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.batchWriteItem
import aws.sdk.kotlin.services.dynamodb.createTable
import aws.sdk.kotlin.services.dynamodb.getItem
import aws.sdk.kotlin.services.dynamodb.model.*
import aws.sdk.kotlin.services.dynamodb.waiters.waitUntilTableExists

/**
 * Creates a table with the given name/keys. This method waits for the asynchronous completion of the operation before
 * returning.
 * @param name The name for the new table
 * @param schema The schema for the table
 * @param gsis A map of GSI names to schemas
 * @param lsis A map of LSI names to schemas
 */
suspend fun DynamoDbClient.createTable(
    name: String,
    schema: ItemSchema<*>,
    gsis: Map<String, ItemSchema<*>>,
    lsis: Map<String, ItemSchema<*>>,
) {
    val partitionKeyAttrs = schema.partitionKeyAttrs()
    val sortKeyAttrs = schema.sortKeyAttrs()

    val throughput = ProvisionedThroughput {
        // provisioned throughput is required but ignored by DDB Local so just use dummy values
        readCapacityUnits = 1
        writeCapacityUnits = 1
    }

    createTable {
        tableName = name

        attributeDefinitions = deriveAttributes(schema, gsis, lsis)

        keySchema = partitionKeyAttrs.map { attr ->
            KeySchemaElement {
                attributeName = attr.name
                keyType = KeyType.Hash
            }
        } + sortKeyAttrs.map { attr ->
            KeySchemaElement {
                attributeName = attr.name
                keyType = KeyType.Range
            }
        }

        provisionedThroughput = throughput

        globalSecondaryIndexes = gsis.takeUnless { it.isEmpty() }?.map { (name, schema) ->
            GlobalSecondaryIndex {
                indexName = name
                keySchema = schema.toKeySchema()
                projection = schema.toProjection()
                provisionedThroughput = throughput
            }
        }

        localSecondaryIndexes = lsis.takeUnless { it.isEmpty() }?.map { (name, schema) ->
            LocalSecondaryIndex {
                indexName = name
                keySchema = schema.toKeySchema()
                projection = schema.toProjection()
            }
        }
    }

    waitUntilTableExists { tableName = name }
}

/**
 * Gets an item from the given table with the given keys
 * @param tableName The name of the table to fetch from
 * @param keys One or more tuples of string key to value
 */
suspend fun DynamoDbClient.getItem(tableName: String, vararg keys: Pair<String, Any?>) = getItem {
    this.tableName = tableName
    key = itemOf(*keys)
}

/**
 * Puts the given items into the given table
 * @param tableName The name of the table to insert to
 * @param items A collection of maps of strings to values to be mapped and persisted to the table
 */
suspend fun DynamoDbClient.putItems(tableName: String, items: List<Item>) {
    val batches = items
        .map { item ->
            WriteRequest {
                putRequest { this.item = item }
            }
        }
        .chunked(25) // Max batchWriteItem page size

    batches.forEach { batch ->
        batchWriteItem {
            requestItems = mapOf(tableName to batch)
        }
    }
}

/**
 * Derives the [AttributeDefinition] instances for a table, taking into account its schema and any secondary indices
 * @param schema The schema of the table
 * @param gsis A map of GSI names to schemas
 * @param lsis A map of LSI names to schemas
 */
private fun deriveAttributes(
    schema: ItemSchema<*>,
    gsis: Map<String, ItemSchema<*>>,
    lsis: Map<String, ItemSchema<*>>,
): List<AttributeDefinition> {
    val keyAttrs = schema.allKeyAttrs() +
        gsis.values.flatMap { it.allKeyAttrs() } +
        lsis.values.flatMap { it.allKeyAttrs() }

    return keyAttrs
        .distinctBy { it.name }
        .map { it.toAttributeDefinition() }
}

private fun KeyAttrSpec<*>.toAttributeDefinition() = AttributeDefinition {
    attributeName = name
    attributeType = type
}

/**
 * Gets the names of all attributes comprehended by this schema. **Note**: Only works for [SimpleItemConverter] since
 * that's all our tests require so far.
 */
private val ItemSchema<*>.allAttributeNames: Set<String>
    get() = when (val c = converter) {
        is SimpleItemConverter<*, *> -> c.descriptors.keys
        else -> error("Unsupported converter type ${c::class}")
    }

/**
 * Extracts the partition and sort key attributes from this [ItemSchema] as a list
 */
private fun ItemSchema<*>.allKeyAttrs() = partitionKeyAttrs() + sortKeyAttrs()

/**
 * Extracts the partition key attributes from this [ItemSchema] as a list
 */
private fun ItemSchema<*>.partitionKeyAttrs() = when (this) {
    is ItemSchema.PartitionKey<*, *> -> partitionKey.attrs
    is ItemSchema.CompositeKey<*, *, *> -> partitionKey.attrs
}

/**
 * Extracts the sort key attributes from this [ItemSchema] as a list. The returned list will be empty if the schema has
 * no sort key.
 */
private fun ItemSchema<*>.sortKeyAttrs() = when (this) {
    is ItemSchema.PartitionKey<*, *> -> listOf()
    is ItemSchema.CompositeKey<*, *, *> -> sortKey.attrs
}

/**
 * Derives [KeySchemaElement] instances for this schema. The first [KeySpec] in the schema is used as the partition/hash
 * key. The second (if present) is used as the sort/range key.
 */
private fun ItemSchema<*>.toKeySchema() = keyAttributeNames.mapIndexed { index, key ->
    KeySchemaElement {
        attributeName = key
        keyType = if (index == 0) KeyType.Hash else KeyType.Range
    }
}

/**
 * Derives a [Projection] for this schema
 */
private fun ItemSchema<*>.toProjection() = Projection {
    val projectionAttributes = allAttributeNames - keyAttributeNames

    if (projectionAttributes.isEmpty()) {
        projectionType = ProjectionType.KeysOnly
    } else {
        projectionType = ProjectionType.Include
        nonKeyAttributes = (allAttributeNames - keyAttributeNames).toList()
    }
}
