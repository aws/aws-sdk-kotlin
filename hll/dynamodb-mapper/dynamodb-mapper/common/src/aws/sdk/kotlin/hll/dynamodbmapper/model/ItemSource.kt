/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model

import aws.sdk.kotlin.hll.dynamodbmapper.operations.ItemSourceOperations

/**
 * Represents a source of DynamoDB items (such as a table or secondary index)
 */
public interface ItemSource<T> :
    PersistenceSpec<T>,
    ItemSourceOperations<T> {

    /**
     * Represents a source of DynamoDB items (such as a table or secondary index) whose primary key is a single
     * partition key
     * @param T The type of objects which will be read from and/or written to this item source
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     */
    public interface PartitionKey<T, PK> :
        ItemSource<T>,
        PersistenceSpec.PartitionKey<T, PK>

    /**
     * Represents a source of DynamoDB items (such as a table or secondary index) whose primary key is a composite of a
     * partition key and a sort key
     * @param T The type of objects which will be read from and/or written to this item source
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     * @param SK The type of the sort key property, either [String], [Number], or [ByteArray]
     */
    public interface CompositeKey<T, PK, SK> :
        ItemSource<T>,
        PersistenceSpec.CompositeKey<T, PK, SK>
}
