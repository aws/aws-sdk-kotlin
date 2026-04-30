/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType

/**
 * Specifies how items can be read from a secondary index
 * @param T The type of objects which will be read from this index
 */
public interface IndexSpec<T> : PersistenceSpec<T> {
    /**
     * The name of the table
     */
    public val tableName: String

    /**
     * The name of the secondary index
     */
    public val indexName: String?

    /**
     * Specifies how items can be read from a secondary index whose primary key consists of a single partition key
     */
    public interface PartitionKey<T, PK : KeyType> :
        IndexSpec<T>,
        PersistenceSpec.PartitionKey<T, PK> {
        override val schema: ItemSchema.PartitionKey<T, PK>
    }

    /**
     * Specifies how items can be read from a secondary index whose primary key consists of a composite of a partition
     * key and a sort key
     */
    public interface CompositeKey<T, PK : KeyType, SK : KeyType> :
        IndexSpec<T>,
        PersistenceSpec.CompositeKey<T, PK, SK> {
        override val schema: ItemSchema.CompositeKey<T, PK, SK>
    }
}
