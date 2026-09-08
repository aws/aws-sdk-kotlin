/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType

public sealed interface BatchGetItemRequestTableDsl {
    public var consistentRead: Boolean?

    public interface PartitionKey<T, PK : KeyType> : BatchGetItemRequestTableDsl {
        public var keys: List<PK>

        public fun key(key: PK)
        public fun keys(keys: Iterable<PK>)
    }

    public interface CompositeKey<T, PK : KeyType, SK : KeyType> : BatchGetItemRequestTableDsl {
        public var keys: List<Pair<PK, SK>>

        public fun key(partitionKey: PK, sortKey: SK)
        public fun keys(keys: Iterable<Pair<PK, SK>>)
    }
}
