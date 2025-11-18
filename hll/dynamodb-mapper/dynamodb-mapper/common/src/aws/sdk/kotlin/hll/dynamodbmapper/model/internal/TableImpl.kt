/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model.internal

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.model.Index
import aws.sdk.kotlin.hll.dynamodbmapper.model.Table
import aws.sdk.kotlin.hll.dynamodbmapper.model.TableSpec
import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
import aws.sdk.kotlin.hll.dynamodbmapper.operations.*
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.Interceptor
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LReqContext
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.GetItemRequest as LowLevelGetItemRequest
import aws.sdk.kotlin.services.dynamodb.model.GetItemResponse as LowLevelGetItemResponse

internal fun <T, PK> tableImpl(
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

        override fun <T, PK> getIndex(
            name: String,
            schema: ItemSchema.PartitionKey<T, PK>,
        ): Index.PartitionKey<T, PK> = indexImpl(mapper, tableName, name, schema)

        override fun <T, PK, SK> getIndex(
            name: String,
            schema: ItemSchema.CompositeKey<T, PK, SK>,
        ): Index.CompositeKey<T, PK, SK> = indexImpl(mapper, tableName, name, schema)

        override suspend fun getItem(partitionKey: PK): T? {
            val keyItem = itemOf(schema.partitionKey.toField(partitionKey))
            val interceptor = KeyInsertionInterceptor<T>(keyItem)
            val op = getItemOperation(specImpl).let {
                it.copy(
                    interceptors = it.interceptors.prepend(interceptor),
                )
            }
            val hRes = op.execute(GetItemRequest { })
            return hRes.item
        }
    }
}

internal fun <T, PK, SK> tableImpl(
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

        override fun <T, PK> getIndex(
            name: String,
            schema: ItemSchema.PartitionKey<T, PK>,
        ): Index.PartitionKey<T, PK> = indexImpl(mapper, tableName, name, schema)

        override fun <T, PK, SK> getIndex(
            name: String,
            schema: ItemSchema.CompositeKey<T, PK, SK>,
        ): Index.CompositeKey<T, PK, SK> = indexImpl(mapper, tableName, name, schema)

        override suspend fun getItem(partitionKey: PK, sortKey: SK): T? {
            val keyItem = itemOf(
                schema.partitionKey.toField(partitionKey),
                schema.sortKey.toField(sortKey),
            )
            val interceptor = KeyInsertionInterceptor<T>(keyItem)
            val op = getItemOperation(specImpl).let {
                it.copy(
                    interceptors = it.interceptors.prepend(interceptor),
                )
            }
            val hRes = op.execute(GetItemRequest { })
            return hRes.item
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
