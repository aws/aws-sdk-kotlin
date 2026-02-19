/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.example

import aws.sdk.kotlin.runtime.AwsServiceException
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.*
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.lang.IllegalStateException

/**
 * Partial implementation of: https://docs.amazonaws.cn/en_us/amazondynamodb/latest/developerguide/GettingStarted.Java.html
 */
fun main() = runBlocking {
    val client = DynamoDbClient { region = "us-east-2" }

    val tableName = "dynamo-movies-example"

    try {
        createMoviesTable(client, tableName)

        client.waitForTableReady(tableName)

        loadMoviesTable(client, tableName)

        val films2222 = client.moviesInYear(tableName, 2222)
        check(films2222.count == 0)

        val films2013 = client.moviesInYear(tableName, 2013)
        check(films2013.count == 2)

        val titles = films2013.items?.mapNotNull { (it["title"] as? AttributeValue.S)?.value }
        println("2013 film titles:")
        println(titles)
    } catch (ex: AwsServiceException) {
        println(ex)
    }

    client.close()
}

suspend fun createMoviesTable(client: DynamoDbClient, name: String) {
    val tableExists = client.listTables(ListTablesRequest {}).tableNames?.contains(name) ?: false
    if (tableExists) return

    val req = CreateTableRequest {
        tableName = name
        keySchema = listOf(
            KeySchemaElement {
                attributeName = "year"
                keyType = KeyType.Hash
            },
            KeySchemaElement {
                attributeName = "title"
                keyType = KeyType.Range
            },
        )

        attributeDefinitions = listOf(
            AttributeDefinition {
                attributeName = "year"
                attributeType = ScalarAttributeType.N
            },
            AttributeDefinition {
                attributeName = "title"
                attributeType = ScalarAttributeType.S
            },
        )
        provisionedThroughput {
            readCapacityUnits = 10
            writeCapacityUnits = 10
        }
    }

    val resp = client.createTable(req)
    println("created table: ${resp.tableDescription?.tableArn}")
}

// no waiters support (yet)
suspend fun DynamoDbClient.waitForTableReady(name: String) {
    while (true) {
        try {
            val req = DescribeTableRequest { tableName = name }
            if (describeTable(req).table?.tableStatus != TableStatus.Creating) {
                println("table ready")
                return
            }
        } catch (ex: AwsServiceException) {
            if (!ex.sdkErrorMetadata.isRetryable) throw ex
        }
        println("waiting for table to be ready...")
        delay(1000)
    }
}

suspend fun loadMoviesTable(client: DynamoDbClient, name: String) {
    // load items into table
    val data = getResourceAsText("data.json")
    val elements = JsonParser.parseString(data).asJsonArray
    elements.forEach {
        // map the json element -> AttributeValue
        val attrValue = jsonElementToAttributeValue(it) as? AttributeValue.M ?: throw IllegalStateException("expected a top level object value")
        val req = PutItemRequest {
            tableName = name
            item = attrValue.value
        }

        client.putItem(req)
    }
}

suspend fun DynamoDbClient.moviesInYear(name: String, year: Int): QueryResponse {
    val req = QueryRequest {
        tableName = name
        keyConditionExpression = "#yr = :yyyy"
        expressionAttributeNames = mapOf(
            "#yr" to "year",
        )
        expressionAttributeValues = mapOf(
            ":yyyy" to AttributeValue.N(year.toString()),
        )
    }
    return query(req)
}

// utility/support functions

fun getResourceAsText(path: String): String = object {}.javaClass.getResource(path)?.readText() ?: error("Unable to load $path")

// map json to attribute values
fun jsonElementToAttributeValue(element: JsonElement): AttributeValue = when {
    element.isJsonNull -> AttributeValue.Null(true)
    element.isJsonPrimitive -> {
        val primitive = element.asJsonPrimitive
        when {
            primitive.isBoolean -> AttributeValue.Bool(primitive.asBoolean)
            primitive.isString -> AttributeValue.S(primitive.asString)
            else -> {
                check(primitive.isNumber) { "expected number" }
                AttributeValue.N(primitive.asString)
            }
        }
    }
    element.isJsonArray -> AttributeValue.L(element.asJsonArray.map(::jsonElementToAttributeValue))
    element.isJsonObject -> {
        AttributeValue.M(
            element.asJsonObject.entrySet().associate {
                it.key to jsonElementToAttributeValue(it.value)
            },
        )
    }
    else -> throw IllegalStateException("unknown json element type: $element")
}
