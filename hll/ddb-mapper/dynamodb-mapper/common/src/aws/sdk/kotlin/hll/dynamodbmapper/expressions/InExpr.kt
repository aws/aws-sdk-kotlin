/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.InExprImpl

/**
 * Represents an `IN` expression as described in
 * [DynamoDB's **making comparisons** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Comparators).
 * This expression will be true if `value in set` (or, equivalently, if `set.contains(value)`).
 */
public interface InExpr : BooleanExpr {
    /**
     * The value to check for in [set]
     */
    public val value: Expression

    /**
     * The set of values to compare against [value]
     */
    public val set: Collection<Expression>

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new `IN` expression
 * @param value The value to check for in [set]
 * @param set The set of values to compare against [value]
 */
public fun InExpr(value: Expression, set: Collection<Expression>): InExpr = InExprImpl(value, set)
