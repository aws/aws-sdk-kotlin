/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model

import aws.sdk.kotlin.hll.dynamodbmapper.operations.IndexOperations

/**
 * Represents a secondary index on a table in DynamoDB and an associated item schema. Operations on this index will
 * invoke low-level operations and map items to objects.
 * @param T The type of objects which will be read from this index
 */
public interface Index<T> :
    IndexSpec<T>,
    IndexOperations<T>,
    ItemSource<T> {

    /**
     * Represents a secondary index whose primary key is a single partition key
     * @param T The type of objects which will be read from this index
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     */
    public interface PartitionKey<T, PK> :
        Index<T>,
        PersistenceSpec.PartitionKey<T, PK>

    /**
     * Represents a secondary index whose primary key is a composite of a partition key and a sort key
     * @param T The type of objects which will be read from this index
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     * @param SK The type of the sort key property, either [String], [Number], or [ByteArray]
     */
    public interface CompositeKey<T, PK, SK> :
        Index<T>,
        PersistenceSpec.CompositeKey<T, PK, SK>
}
