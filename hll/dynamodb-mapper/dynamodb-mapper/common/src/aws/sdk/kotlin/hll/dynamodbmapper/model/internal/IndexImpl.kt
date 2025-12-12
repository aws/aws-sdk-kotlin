/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model.internal

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.model.Index
import aws.sdk.kotlin.hll.dynamodbmapper.model.IndexSpec
import aws.sdk.kotlin.hll.dynamodbmapper.operations.IndexOperations
import aws.sdk.kotlin.hll.dynamodbmapper.operations.IndexOperationsImpl

internal fun <T, PK : KeyType> indexImpl(
    mapper: DynamoDbMapper,
    tableName: String,
    indexName: String,
    schema: ItemSchema.PartitionKey<T, PK>,
): Index.PartitionKey<T, PK> {
    val specImpl = IndexSpecPartitionKeyImpl(mapper, tableName, indexName, schema)
    val opsImpl = IndexOperationsImpl(specImpl)
    return object :
        Index.PartitionKey<T, PK>,
        IndexSpec.PartitionKey<T, PK> by specImpl,
        IndexOperations<T> by opsImpl { }
}

internal fun <T, PK : KeyType, SK : KeyType> indexImpl(
    mapper: DynamoDbMapper,
    tableName: String,
    indexName: String,
    schema: ItemSchema.CompositeKey<T, PK, SK>,
): Index.CompositeKey<T, PK, SK> {
    val specImpl = IndexSpecCompositeKeyImpl(mapper, tableName, indexName, schema)
    val opsImpl = IndexOperationsImpl(specImpl)
    return object :
        Index.CompositeKey<T, PK, SK>,
        IndexSpec.CompositeKey<T, PK, SK> by specImpl,
        IndexOperations<T> by opsImpl { }
}
