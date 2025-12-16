/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model.internal

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.model.IndexSpec

internal data class IndexSpecPartitionKeyImpl<T, PK : KeyType>(
    override val mapper: DynamoDbMapper,
    override val tableName: String,
    override val indexName: String,
    override val schema: ItemSchema.PartitionKey<T, PK>,
) : IndexSpec.PartitionKey<T, PK>

internal data class IndexSpecCompositeKeyImpl<T, PK : KeyType, SK : KeyType>(
    override val mapper: DynamoDbMapper,
    override val tableName: String,
    override val indexName: String,
    override val schema: ItemSchema.CompositeKey<T, PK, SK>,
) : IndexSpec.CompositeKey<T, PK, SK>
