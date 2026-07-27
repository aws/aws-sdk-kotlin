/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.operations.BatchGetItemRequestTable

internal data class BatchGetItemRequestTablePartitionKeyImpl<T, PK : KeyType>(
    override val consistentRead: Boolean?,
    override val keys: List<PK>,
    override val schema: ItemSchema.PartitionKey<T, PK>,
    override val tableName: String,
) : BatchGetItemRequestTable.PartitionKey<T, PK>

internal data class BatchGetItemRequestTableCompositeKeyImpl<T, PK : KeyType, SK : KeyType>(
    override val consistentRead: Boolean?,
    override val keys: List<Pair<PK, SK>>,
    override val schema: ItemSchema.CompositeKey<T, PK, SK>,
    override val tableName: String,
) : BatchGetItemRequestTable.CompositeKey<T, PK, SK>
