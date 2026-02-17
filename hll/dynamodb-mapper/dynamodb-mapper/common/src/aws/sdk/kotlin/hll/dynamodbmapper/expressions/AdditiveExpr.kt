/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.AdditiveExprImpl

/**
 * Describes a type of additive operation
 * @param exprString The literal value of the operator to use in an expression string
 */
public enum class AdditiveOperation(public val exprString: String) {
    /**
     * An addition operation, equivalent to `+` in Kotlin and DynamoDB
     */
    ADD("+"),

    /**
     * A subtraction operation, equivalent to `-` in Kotlin and DynamoDB
     */
    SUBTRACT("-"),
}

/**
 * Represents an arithmetic addition or subtraction expression
 */
public interface AdditiveExpr : Expression {
    /**
     * The type of additive operation (i.e., the operator)
     */
    public val operation: AdditiveOperation

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
 * Creates a new additive expression
 * @param operation The type of additive operation (i.e., the operator)
 * @param left The left value being compared
 * @param right The right value being compared
 */
public fun AdditiveExpr(
    operation: AdditiveOperation,
    left: Expression,
    right: Expression,
): AdditiveExpr = AdditiveExprImpl(operation, left, right)
