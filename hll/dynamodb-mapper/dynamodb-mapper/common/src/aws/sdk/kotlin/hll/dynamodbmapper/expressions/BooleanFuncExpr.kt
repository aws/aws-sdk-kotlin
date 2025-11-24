/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.BooleanFuncExprImpl

/**
 * Represents a function expression that yields a boolean result as described in
 * [DynamoDB's **function** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Functions)
 */
public interface BooleanFuncExpr :
    BooleanExpr,
    SortKeyExpr {
    /**
     * The specific boolean function to use
     */
    public val func: BooleanFunc

    /**
     * The attribute path to pass as the function's first argument
     */
    public val path: AttributePath

    /**
     * Any additional arguments used by the function
     */
    public val additionalOperands: List<Expression>

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new boolean function expression
 * @param func The specific boolean function to use
 * @param path The attribute path to pass as the function's first argument
 * @param additionalOperands Any additional arguments used by the function
 */
public fun BooleanFuncExpr(
    func: BooleanFunc,
    path: AttributePath,
    additionalOperands: List<Expression> = listOf(),
): BooleanFuncExpr = BooleanFuncExprImpl(func, path, additionalOperands)

/**
 * Creates a new boolean function expression
 * @param func The specific boolean function to use
 * @param path The attribute path to pass as the function's first argument
 * @param additionalOperands Any additional arguments used by the function
 */
public fun BooleanFuncExpr(
    func: BooleanFunc,
    path: AttributePath,
    vararg additionalOperands: Expression,
): BooleanFuncExpr = BooleanFuncExprImpl(func, path, additionalOperands.toList())
