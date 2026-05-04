/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.AndExprImpl

/**
 * Represents an `AND` expression as described in
 * [DynamoDB's **logical evaluations** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.LogicalEvaluations).
 * This expression will be true if `(operand[0] && operand[1] && ... && operand[n - 1])`.
 */
public interface AndExpr : BooleanExpr {
    /**
     * A list of 2 or more [BooleanExpr] conditions which are ANDed together
     */
    public val operands: List<BooleanExpr>

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new [AndExpr] with the given [operands]
 * @param operands A list of 2 or more [BooleanExpr] conditions which are ANDed together
 */
public fun AndExpr(operands: List<BooleanExpr>): AndExpr = AndExprImpl(operands)
