/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.KeyFilterImpl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.SortKeyFilterDslImpl
import aws.sdk.kotlin.hll.dynamodbmapper.items.Key
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType

/**
 * Represents a filter which limits a Query operation to a specific partition key and optional sort key criteria (if
 * applicable)
 */
public interface KeyFilter {
    /**
     * The required value of the partition key
     */
    public val partitionKey: KeyType

    /**
     * The sort key expressions, in key order
     */
    public val sortKeyExpressions: List<SortKeyExpr>
}

/**
 * Creates a new [KeyFilter] implementation for the given partition key value and optional sort key expressions
 * @param partitionKey The partition key value to assert
 * @param sortKeyExpressions Up to 4 sort key expressions in sort key attribute order
 */
public fun KeyFilter(partitionKey: ByteArray, vararg sortKeyExpressions: SortKeyFilterDsl.() -> SortKeyExpr): KeyFilter = KeyFilter(Key(partitionKey), *sortKeyExpressions)

/**
 * Creates a new [KeyFilter] implementation for the given partition key value and optional sort key expressions
 * @param partitionKey The partition key value to assert
 * @param sortKeyExpressions Up to 4 sort key expressions in sort key attribute order
 */
public fun KeyFilter(partitionKey: Byte, vararg sortKeyExpressions: SortKeyFilterDsl.() -> SortKeyExpr): KeyFilter = KeyFilter(Key(partitionKey), *sortKeyExpressions)

/**
 * Creates a new [KeyFilter] implementation for the given partition key value and optional sort key expressions
 * @param partitionKey The partition key value to assert
 * @param sortKeyExpressions Up to 4 sort key expressions in sort key attribute order
 */
public fun KeyFilter(partitionKey: Int, vararg sortKeyExpressions: SortKeyFilterDsl.() -> SortKeyExpr): KeyFilter = KeyFilter(Key(partitionKey), *sortKeyExpressions)

/**
 * Creates a new [KeyFilter] implementation for the given partition key value and optional sort key expressions
 * @param partitionKey The partition key value to assert
 * @param sortKeyExpressions Up to 4 sort key expressions in sort key attribute order
 */
public fun KeyFilter(partitionKey: Long, vararg sortKeyExpressions: SortKeyFilterDsl.() -> SortKeyExpr): KeyFilter = KeyFilter(Key(partitionKey), *sortKeyExpressions)

/**
 * Creates a new [KeyFilter] implementation for the given partition key value and optional sort key expressions
 * @param partitionKey The partition key value to assert
 * @param sortKeyExpressions Up to 4 sort key expressions in sort key attribute order
 */
public fun KeyFilter(partitionKey: Short, vararg sortKeyExpressions: SortKeyFilterDsl.() -> SortKeyExpr): KeyFilter = KeyFilter(Key(partitionKey), *sortKeyExpressions)

/**
 * Creates a new [KeyFilter] implementation for the given partition key value and optional sort key expressions
 * @param partitionKey The partition key value to assert
 * @param sortKeyExpressions Up to 4 sort key expressions in sort key attribute order
 */
public fun KeyFilter(partitionKey: String, vararg sortKeyExpressions: SortKeyFilterDsl.() -> SortKeyExpr): KeyFilter = KeyFilter(Key(partitionKey), *sortKeyExpressions)

/**
 * Creates a new [KeyFilter] implementation for the given partition key value and optional sort key expressions
 * @param partitionKey The partition key value to assert
 * @param sortKeyExpressions Up to 4 sort key expressions in sort key attribute order
 */
public fun KeyFilter(partitionKey: KeyType, vararg sortKeyExpressions: SortKeyFilterDsl.() -> SortKeyExpr): KeyFilter =
    KeyFilterImpl(partitionKey, SortKeyFilterDslImpl.run { sortKeyExpressions.map { it() } })
