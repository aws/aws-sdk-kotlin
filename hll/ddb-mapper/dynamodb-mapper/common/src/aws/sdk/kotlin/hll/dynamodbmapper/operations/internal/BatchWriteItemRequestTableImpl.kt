/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.operations.BatchWriteItemRequestTable

internal data class BatchWriteItemRequestTablePartitionKeyImpl<T, PK : KeyType>(
    override val putItems: List<T>,
    override val tableName: String,
    override val deleteKeys: List<PK>,
    override val schema: ItemSchema.PartitionKey<T, PK>,
) : BatchWriteItemRequestTable.PartitionKey<T, PK>

internal data class BatchWriteItemRequestTableCompositeKeyImpl<T, PK : KeyType, SK : KeyType>(
    override val putItems: List<T>,
    override val tableName: String,
    override val deleteKeys: List<Pair<PK, SK>>,
    override val schema: ItemSchema.CompositeKey<T, PK, SK>,
) : BatchWriteItemRequestTable.CompositeKey<T, PK, SK>
