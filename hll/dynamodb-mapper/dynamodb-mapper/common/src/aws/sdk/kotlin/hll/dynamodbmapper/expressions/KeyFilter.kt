/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.KeyFilterImpl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.SortKeyFilterImpl

/**
 * Represents a filter which limits a Query operation to a specific partition key and optional sort key criteria (if
 * applicable)
 */
public interface KeyFilter {
    /**
     * The required value of the partition key
     */
    public val partitionKey: Any

    /**
     * The sort key expression (if set)
     */
    public val sortKey: SortKeyExpr?
}

/**
 * Creates a new [KeyFilter] for a partition key
 * @param partitionKey The value required for the partition key. This must be set to a byte array, string, or number
 * (including unsigned numbers).
 */
public fun KeyFilter(partitionKey: Any): KeyFilter = KeyFilterImpl(partitionKey, null)

/**
 * Creates a new [KeyFilter] for a partition key and sort key. Note that using this overload requires a schema with a
 * composite key.
 * @param partitionKey The value required for the partition key. This must be set to a byte array, string, or number
 * (including unsigned numbers).
 * @param sortKey A DSL block that sets the condition for the sort key. See [SortKeyFilter] for more details.
 */
public fun KeyFilter(partitionKey: Any, sortKey: SortKeyFilter.() -> SortKeyExpr): KeyFilter =
    KeyFilterImpl(partitionKey, SortKeyFilterImpl.run(sortKey))
