/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.operations.BatchWriteItemResponseTable

internal data class BatchWriteItemResponseTablePartitionKeyImpl<T, PK : KeyType>(
    override val tableName: String,
    override val unprocessedItems: List<T>,
    override val schema: ItemSchema.PartitionKey<T, PK>,
    override val unprocessedKeys: List<PK>,
) : BatchWriteItemResponseTable.PartitionKey<T, PK>

internal data class BatchWriteItemResponseTableCompositeKeyImpl<T, PK : KeyType, SK : KeyType>(
    override val tableName: String,
    override val unprocessedItems: List<T>,
    override val schema: ItemSchema.CompositeKey<T, PK, SK>,
    override val unprocessedKeys: List<Pair<PK, SK>>,
) : BatchWriteItemResponseTable.CompositeKey<T, PK, SK>
