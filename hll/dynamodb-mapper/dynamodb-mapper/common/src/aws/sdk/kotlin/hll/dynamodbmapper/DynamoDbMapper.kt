/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper.Companion.Config
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper.Config.Companion.Builder
import aws.sdk.kotlin.hll.dynamodbmapper.internal.DynamoDbMapperImpl
import aws.sdk.kotlin.hll.dynamodbmapper.internal.MapperConfigBuilderImpl
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.model.Table
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.Interceptor
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.InterceptorAny
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient

/**
 * A high-level client for DynamoDB which maps custom data types into DynamoDB attributes and vice versa.
 */
public interface DynamoDbMapper {
    public companion object {
        /**
         * Instantiate a new [Config] object
         * @param config A DSL block for setting properties of the config
         */
        public fun Config(config: Config.Builder.() -> Unit = { }): Config =
            Config.Builder().apply(config).build()
    }

    /**
     * The low-level DynamoDB client used for underlying calls to the service
     */
    public val client: DynamoDbClient

    /**
     * The active configuration for this mapper
     */
    public val config: Config

    /**
     * Get a [Table] reference for performing table operations
     * @param T The type of objects which will be read from and/or written to this table
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     * @param name The name of the table
     * @param schema The [ItemSchema] which describes the table, its keys, and how items are converted
     */
    public fun <T, PK> getTable(
        name: String,
        schema: ItemSchema.PartitionKey<T, PK>,
    ): Table.PartitionKey<T, PK>

    /**
     * Get a [Table] reference for performing table operations
     * @param T The type of objects which will be read from and/or written to this table
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     * @param SK The type of the sort key property, either [String], [Number], or [ByteArray]
     * @param name The name of the table
     * @param schema The [ItemSchema] which describes the table, its keys, and how items are converted
     */
    public fun <T, PK, SK> getTable(
        name: String,
        schema: ItemSchema.CompositeKey<T, PK, SK>,
    ): Table.CompositeKey<T, PK, SK>

    // TODO add multi-table operations like batchGetItem, transactWriteItems, etc.

    /**
     * The immutable configuration for a [DynamoDbMapper] instance
     */
    public interface Config {
        public companion object {
            /**
             * Instantiate a new [Builder] object
             */
            public fun Builder(): Builder = MapperConfigBuilderImpl()
        }

        /**
         * A list of [Interceptor] instances which will be applied to operations as they move through the request
         * pipeline.
         */
        public val interceptors: List<InterceptorAny>

        /**
         * Convert this immutable configuration into a mutable [Builder] object. Updates made to the mutable builder
         * properties will not affect this instance.
         */
        public fun toBuilder(): Builder

        /**
         * A mutable configuration builder for a [DynamoDbMapper] instance
         */
        public interface Builder {
            /**
             * A list of [Interceptor] instances which will be applied to operations as they move through the request
             * pipeline.
             */
            public var interceptors: MutableList<InterceptorAny>

            /**
             * Builds this mutable [Builder] object into an immutable [Config] object. Changes made to this instance do
             * not affect the built instance.
             */
            public fun build(): Config
        }
    }
}

/**
 * Instantiate a new [DynamoDbMapper]
 * @param client The low-level DynamoDB client to use for underlying calls to the service
 * @param config A DSL configuration block
 */
public fun DynamoDbMapper(
    client: DynamoDbClient,
    config: DynamoDbMapper.Config.Builder.() -> Unit = { },
): DynamoDbMapper = DynamoDbMapperImpl(client, DynamoDbMapper.Config(config))
