/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TransactGetItemsRequestTable

internal data class TransactGetItemsRequestTablePartitionKeyImpl<T, PK : KeyType>(
    override val tableName: String,
    override val keys: List<PK>,
    override val schema: ItemSchema.PartitionKey<T, PK>,
) : TransactGetItemsRequestTable.PartitionKey<T, PK>

internal data class TransactGetItemsRequestTableCompositeKeyImpl<T, PK : KeyType, SK : KeyType>(
    override val tableName: String,
    override val keys: List<Pair<PK, SK>>,
    override val schema: ItemSchema.CompositeKey<T, PK, SK>,
) : TransactGetItemsRequestTable.CompositeKey<T, PK, SK>
