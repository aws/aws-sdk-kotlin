/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType

public sealed interface BatchWriteItemRequestTableDsl<T> {
    public var putItems: List<T>

    public fun putItem(item: T)
    public fun putItems(items: Iterable<T>)

    public interface PartitionKey<T, PK : KeyType> : BatchWriteItemRequestTableDsl<T> {
        public var deleteKeys: List<PK>

        public fun deleteKey(key: PK)
        public fun deleteKeys(keys: Iterable<PK>)
    }

    public interface CompositeKey<T, PK : KeyType, SK : KeyType> : BatchWriteItemRequestTableDsl<T> {
        public var deleteKeys: List<Pair<PK, SK>>

        public fun deleteKey(partitionKey: PK, sortKey: SK)
        public fun deleteKeys(keys: Iterable<Pair<PK, SK>>)
    }
}
