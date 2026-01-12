/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model.internal

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.interceptors.TtlInterceptor
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.model.Index
import aws.sdk.kotlin.hll.dynamodbmapper.model.SchemaAttributes
import aws.sdk.kotlin.hll.dynamodbmapper.model.Table
import aws.sdk.kotlin.hll.dynamodbmapper.model.TableSpec
import aws.sdk.kotlin.hll.dynamodbmapper.operations.*
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.Interceptor
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LReqContext
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.GetItemRequest as LowLevelGetItemRequest
import aws.sdk.kotlin.services.dynamodb.model.GetItemResponse as LowLevelGetItemResponse
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest as LowLevelPutItemRequest
import aws.sdk.kotlin.services.dynamodb.model.PutItemResponse as LowLevelPutItemResponse

internal fun <T, PK : KeyType> tableImpl(
    mapper: DynamoDbMapper,
    name: String,
    schema: ItemSchema.PartitionKey<T, PK>,
): Table.PartitionKey<T, PK> {
    val specImpl = TableSpecPartitionKeyImpl(mapper, name, schema)
    val opsImpl = TableOperationsImpl(specImpl)
    return object :
        Table.PartitionKey<T, PK>,
        TableSpec.PartitionKey<T, PK> by specImpl,
        TableOperations<T> by opsImpl {

        override fun <T, PK : KeyType> getIndex(
            name: String,
            schema: ItemSchema.PartitionKey<T, PK>,
        ): Index.PartitionKey<T, PK> = indexImpl(mapper, tableName, name, schema)

        override fun <T, PK : KeyType, SK : KeyType> getIndex(
            name: String,
            schema: ItemSchema.CompositeKey<T, PK, SK>,
        ): Index.CompositeKey<T, PK, SK> = indexImpl(mapper, tableName, name, schema)

        override suspend fun getItem(partitionKey: PK): T? {
            val keyItem = schema.partitionKey.toFields(partitionKey)
            val interceptor = KeyInsertionInterceptor<T>(keyItem)
            val op = getItemOperation(specImpl).let {
                it.copy(
                    interceptors = it.interceptors.prepend(interceptor),
                )
            }
            val hRes = op.execute(GetItemRequest { })
            return hRes.item
        }

        override suspend fun putItem(request: PutItemRequest<T>): PutItemResponse<T> {
            val ttlInterceptor = schema.ttlInterceptor

            val op = putItemOperation(specImpl).let {
                if (ttlInterceptor != null) {
                    @Suppress("UNCHECKED_CAST")
                    val typedInterceptor = ttlInterceptor as Interceptor<T, PutItemRequest<T>, LowLevelPutItemRequest, LowLevelPutItemResponse, PutItemResponse<T>>
                    it.copy(interceptors = it.interceptors.prepend(typedInterceptor))
                } else {
                    it
                }
            }
            return op.execute(request)
        }
    }
}

internal fun <T, PK : KeyType, SK : KeyType> tableImpl(
    mapper: DynamoDbMapper,
    name: String,
    schema: ItemSchema.CompositeKey<T, PK, SK>,
): Table.CompositeKey<T, PK, SK> {
    val specImpl = TableSpecCompositeKeyImpl(mapper, name, schema)
    val opsImpl = TableOperationsImpl(specImpl)
    return object :
        Table.CompositeKey<T, PK, SK>,
        TableSpec.CompositeKey<T, PK, SK> by specImpl,
        TableOperations<T> by opsImpl {

        override fun <T, PK : KeyType> getIndex(
            name: String,
            schema: ItemSchema.PartitionKey<T, PK>,
        ): Index.PartitionKey<T, PK> = indexImpl(mapper, tableName, name, schema)

        override fun <T, PK : KeyType, SK : KeyType> getIndex(
            name: String,
            schema: ItemSchema.CompositeKey<T, PK, SK>,
        ): Index.CompositeKey<T, PK, SK> = indexImpl(mapper, tableName, name, schema)

        override suspend fun getItem(partitionKey: PK, sortKey: SK): T? {
            val keyItem = schema.partitionKey.toFields(partitionKey) + schema.sortKey.toFields(sortKey)
            val interceptor = KeyInsertionInterceptor<T>(keyItem)
            val op = getItemOperation(specImpl).let {
                it.copy(
                    interceptors = it.interceptors.prepend(interceptor),
                )
            }
            val hRes = op.execute(GetItemRequest { })
            return hRes.item
        }

        override suspend fun putItem(request: PutItemRequest<T>): PutItemResponse<T> {
            val ttlInterceptor = schema.ttlInterceptor

            val op = putItemOperation(specImpl).let {
                if (ttlInterceptor != null) {
                    @Suppress("UNCHECKED_CAST")
                    val typedInterceptor = ttlInterceptor as Interceptor<T, PutItemRequest<T>, aws.sdk.kotlin.services.dynamodb.model.PutItemRequest, aws.sdk.kotlin.services.dynamodb.model.PutItemResponse, PutItemResponse<T>>
                    it.copy(interceptors = it.interceptors.prepend(typedInterceptor))
                } else {
                    it
                }
            }

            return op.execute(request)
        }
    }
}

private typealias GetItemInterceptor<T> =
    Interceptor<T, GetItemRequest<T>, LowLevelGetItemRequest, LowLevelGetItemResponse, GetItemResponse<T>>

private class KeyInsertionInterceptor<T>(private val newKey: Map<String, AttributeValue>) : GetItemInterceptor<T> {
    override fun modifyBeforeInvocation(ctx: LReqContext<T, GetItemRequest<T>, LowLevelGetItemRequest>) =
        ctx.lowLevelRequest.copy {
            if (key == null) {
                key = newKey
            }
        }
}

private fun <T> List<T>.prepend(element: T): List<T> = buildList(size + 1) {
    add(element)
    addAll(this)
}

private val <T> ItemSchema<T>.ttlInterceptor: TtlInterceptor<T>?
    get() = attributes
        .getOrNull(SchemaAttributes.TtlFields)
        ?.takeIf { it.isNotEmpty() }
        ?.let { TtlInterceptor() }
