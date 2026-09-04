/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model.internal

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.model.Index
import aws.sdk.kotlin.hll.dynamodbmapper.model.Table
import aws.sdk.kotlin.hll.dynamodbmapper.model.TableSpec
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TableOperations
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TableOperationsCompositeKeyImpl
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TableOperationsPartitionKeyImpl

internal fun <T, PK : KeyType> tableImpl(
    mapper: DynamoDbMapper,
    name: String,
    schema: ItemSchema.PartitionKey<T, PK>,
): Table.PartitionKey<T, PK> {
    val specImpl = TableSpecPartitionKeyImpl(mapper, name, schema)
    val opsImpl = TableOperationsPartitionKeyImpl(specImpl)
    return object :
        Table.PartitionKey<T, PK>,
        TableSpec.PartitionKey<T, PK> by specImpl,
        TableOperations.PartitionKey<T, PK> by opsImpl {

        override fun <T, PK : KeyType> getIndex(
            name: String,
            schema: ItemSchema.PartitionKey<T, PK>,
        ): Index.PartitionKey<T, PK> = indexImpl(mapper, tableName, name, schema)

        override fun <T, PK : KeyType, SK : KeyType> getIndex(
            name: String,
            schema: ItemSchema.CompositeKey<T, PK, SK>,
        ): Index.CompositeKey<T, PK, SK> = indexImpl(mapper, tableName, name, schema)
    }
}

internal fun <T, PK : KeyType, SK : KeyType> tableImpl(
    mapper: DynamoDbMapper,
    name: String,
    schema: ItemSchema.CompositeKey<T, PK, SK>,
): Table.CompositeKey<T, PK, SK> {
    val specImpl = TableSpecCompositeKeyImpl(mapper, name, schema)
    val opsImpl = TableOperationsCompositeKeyImpl(specImpl)
    return object :
        Table.CompositeKey<T, PK, SK>,
        TableSpec.CompositeKey<T, PK, SK> by specImpl,
        TableOperations.CompositeKey<T, PK, SK> by opsImpl {

        override fun <T, PK : KeyType> getIndex(
            name: String,
            schema: ItemSchema.PartitionKey<T, PK>,
        ): Index.PartitionKey<T, PK> = indexImpl(mapper, tableName, name, schema)

        override fun <T, PK : KeyType, SK : KeyType> getIndex(
            name: String,
            schema: ItemSchema.CompositeKey<T, PK, SK>,
        ): Index.CompositeKey<T, PK, SK> = indexImpl(mapper, tableName, name, schema)
    }
}
