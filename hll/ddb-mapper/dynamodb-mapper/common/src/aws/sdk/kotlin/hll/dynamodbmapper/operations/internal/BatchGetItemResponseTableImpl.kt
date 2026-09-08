/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.operations.BatchGetItemResponseTable

internal data class BatchGetItemResponseTablePartitionKeyImpl<T, PK : KeyType>(
    override val items: List<T>,
    override val tableName: String,
    override val schema: ItemSchema.PartitionKey<T, PK>,
    override val unprocessedKeys: List<PK>,
) : BatchGetItemResponseTable.PartitionKey<T, PK>

internal data class BatchGetItemResponseTableCompositeKeyImpl<T, PK : KeyType, SK : KeyType>(
    override val items: List<T>,
    override val tableName: String,
    override val schema: ItemSchema.CompositeKey<T, PK, SK>,
    override val unprocessedKeys: List<Pair<PK, SK>>,
) : BatchGetItemResponseTable.CompositeKey<T, PK, SK>
