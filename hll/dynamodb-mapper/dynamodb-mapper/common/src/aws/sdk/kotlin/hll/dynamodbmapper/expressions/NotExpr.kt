/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.NotExprImpl

/**
 * Represents a `NOT` expression as described in
 * [DynamoDB's **logical evaluations** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.LogicalEvaluations).
 * This expression will be true if `!operand` (i.e., `operand` evaluates to `false`).
 */
public interface NotExpr : BooleanExpr {
    /**
     * The condition to negate
     */
    public val operand: BooleanExpr

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new `NOT` expression
 * @param operand The condition to negate
 */
public fun NotExpr(operand: BooleanExpr): NotExpr = NotExprImpl(operand)
