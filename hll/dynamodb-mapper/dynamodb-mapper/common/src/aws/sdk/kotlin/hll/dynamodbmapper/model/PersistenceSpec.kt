/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema

/**
 * Specifies how items can be read from and written to a specific DynamoDB location (such as a table or a secondary
 * index)
 * @param T The type of objects which will be read from and/or written to this item source
 */
public interface PersistenceSpec<T> {
    /**
     * The [DynamoDbMapper] which holds the underlying DynamoDB service client used to invoke operations
     */
    public val mapper: DynamoDbMapper

    /**
     * The [ItemSchema] which describes how to map objects to items and vice versa
     */
    public val schema: ItemSchema<T>

    /**
     * Specifies how items can be read from and written to a specific DynamoDB location (such as a table or a secondary
     * index) whose primary key consists of a single partition key
     */
    public interface PartitionKey<T, PK> : PersistenceSpec<T> {
        override val schema: ItemSchema.PartitionKey<T, PK>
    }

    /**
     * Specifies how items can be read from and written to a specific DynamoDB location (such as a table or a secondary
     * index) whose primary key consists of a composite of a partition key and a sort key
     */
    public interface CompositeKey<T, PK, SK> : PersistenceSpec<T> {
        override val schema: ItemSchema.CompositeKey<T, PK, SK>
    }
}
