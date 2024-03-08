/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.internal

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.Interceptor
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient

internal data class DynamoDbMapperImpl(
    override val client: DynamoDbClient,
    private val config: DynamoDbMapper.Config,
) : DynamoDbMapper {
    override fun <T, PK> getTable(name: String, schema: ItemSchema.PartitionKey<T, PK>) =
        TableImpl.PartitionKeyImpl(this, name, schema)

    override fun <T, PK, SK> getTable(name: String, schema: ItemSchema.CompositeKey<T, PK, SK>) =
        TableImpl.CompositeKeyImpl(this, name, schema)
}

internal data class MapperConfigImpl(
    override val interceptors: List<Interceptor<*, *, *, *, *>>,
) : DynamoDbMapper.Config {
    override fun toBuilder() = DynamoDbMapper
        .Config
        .Builder()
        .also { it.interceptors = interceptors.toMutableList() }
}

internal class MapperConfigBuilderImpl : DynamoDbMapper.Config.Builder {
    override var interceptors = mutableListOf<Interceptor<*, *, *, *, *>>()

    override fun build() = MapperConfigImpl(interceptors.toList())
}
