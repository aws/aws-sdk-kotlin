/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.UpdateAddImpl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.UpdateExprClauseImpl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.UpdateExprImpl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.UpdateSetImpl

/**
 * Represents an
 * [update expression](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html)
 * used in an `UpdateItem` operation, which consists of four clauses: `SET`, `REMOVE`, `ADD`, and `DELETE`
 */
public interface UpdateExpr : Expression {
    public companion object { }

    /**
     * The `SET` clause of this expression
     */
    public val set: Clause

    /**
     * The `REMOVE` clause of this expression
     */
    public val remove: Clause

    /**
     * The `ADD` clause of this expression
     */
    public val add: Clause

    /**
     * The `DELETE` clause of this expression
     */
    public val delete: Clause

    /**
     * Represents a clause inside an update expression
     */
    public interface Clause {
        /**
         * A list of update clause expressions which are associated with this clause
         */
        public val updates: List<UpdateClauseExpr>
    }

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new update expression
 * @param set The `SET` clause of this expression
 * @param remove The `REMOVE` clause of this expression
 * @param add The `ADD` clause of this expression
 * @param delete The `DELETE` clause of this expression
 */
public fun UpdateExpr(
    set: UpdateExpr.Clause = UpdateExprClauseImpl(),
    remove: UpdateExpr.Clause = UpdateExprClauseImpl(),
    add: UpdateExpr.Clause = UpdateExprClauseImpl(),
    delete: UpdateExpr.Clause = UpdateExprClauseImpl(),
): UpdateExpr = UpdateExprImpl(set, remove, add, delete)

/**
 * Creates an update expression clause
 * @param updates A list of update clause expressions which are associated with this clause
 */
public fun UpdateExpr.Companion.Clause(
    updates: List<UpdateClauseExpr>,
): UpdateExpr.Clause = UpdateExprClauseImpl(updates)
