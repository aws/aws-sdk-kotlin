/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.ComparisonExprImpl

/**
 * Represents a comparison expression as described in
 * [DynamoDB's **making comparisons** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Comparators).
 * The specific type of comparison is identified by the [comparator] field.
 */
public interface ComparisonExpr :
    BooleanExpr,
    SortKeyExpr {
    /**
     * The [Comparator] to use in the expression
     */
    public val comparator: Comparator

    /**
     * The left value being compared
     */
    public val left: Expression

    /**
     * The right value being compared
     */
    public val right: Expression

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new comparison expression
 * @param comparator The [Comparator] to use in the expression
 * @param left The left value being compared
 * @param right The right value being compared
 */
public fun ComparisonExpr(comparator: Comparator, left: Expression, right: Expression): ComparisonExpr =
    ComparisonExprImpl(comparator, left, right)
