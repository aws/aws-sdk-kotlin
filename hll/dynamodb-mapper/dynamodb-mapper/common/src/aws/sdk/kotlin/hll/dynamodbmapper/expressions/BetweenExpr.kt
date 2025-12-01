/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.BetweenExprImpl

/**
 * Represents a `BETWEEN` expression as described in
 * [DynamoDB's **making comparisons** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Comparators).
 * This expression will be true if `value >= min && value <= max`.
 */
public interface BetweenExpr :
    BooleanExpr,
    SortKeyExpr {
    /**
     * The value being compared to the [min] and [max]
     */
    public val value: Expression

    /**
     * The minimum bound for the comparison
     */
    public val min: Expression

    /**
     * The maximum bound for the comparison
     */
    public val max: Expression

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new [BetweenExpr] for the given [value] and range bounded by [min] and [max]
 * @param value The value being compared to the [min] and [max]
 * @param min The minimum bound for the comparison
 * @param max The maximum bound for the comparison
 */
public fun BetweenExpr(value: Expression, min: Expression, max: Expression): BetweenExpr =
    BetweenExprImpl(value, min, max)
