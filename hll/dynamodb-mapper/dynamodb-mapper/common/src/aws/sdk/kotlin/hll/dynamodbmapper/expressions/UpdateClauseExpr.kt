/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.UpdateClauseExprImpl

/**
 * Describes a type of update action. Each entry corresponds to a clause in an update expression.
 */
public enum class UpdateAction {
    /**
     * A [`SET`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET)
     * type of update
     */
    SET,

    /**
     * A [`REMOVE`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.REMOVE)
     * type of update
     */
    REMOVE,

    /**
     * An [`ADD`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.ADD)
     * type of update
     */
    ADD,

    /**
     * A [`DELETE`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.DELETE)
     * type of update
     */
    DELETE,
}

/**
 * Represents an update action inside an update expression clause
 */
public interface UpdateClauseExpr : Expression {
    /**
     * The type of update to perform
     */
    public val action: UpdateAction

    /**
     * The target attribute being affected by the update
     */
    public val target: AttributePath

    /**
     * The value used in the update action. The interpretation of this value varies depending on the update action.
     */
    public val value: Expression

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Create a new update clause expression
 * @param action The type of update to perform
 * @param target The target attribute being affected by the update
 * @param value The value used in the update action. The interpretation of this value varies depending on the update
 * action.
 */
public fun UpdateClauseExpr(
    action: UpdateAction,
    target: AttributePath,
    value: Expression,
): UpdateClauseExpr = UpdateClauseExprImpl(action, target, value)
