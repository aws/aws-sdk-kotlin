/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

/**
 * A [visitor](https://en.wikipedia.org/wiki/Visitor_pattern) that can traverse an [Expression]
 * @param T The type of value used for state tracking by this visitor
 */
public interface ExpressionVisitor<T> {
    /**
     * Visit an [AndExpr]
     * @param expr The expression to visit
     */
    public fun visit(expr: AndExpr): T

    /**
     * Visit an [AttributePath]
     * @param expr The expression to visit
     */
    public fun visit(expr: AttributePath): T

    /**
     * Visit a [BetweenExpr]
     * @param expr The expression to visit
     */
    public fun visit(expr: BetweenExpr): T

    /**
     * Visit a [BooleanFuncExpr]
     * @param expr The expression to visit
     */
    public fun visit(expr: BooleanFuncExpr): T

    /**
     * Visit a [ComparisonExpr]
     * @param expr The expression to visit
     */
    public fun visit(expr: ComparisonExpr): T

    /**
     * Visit a [LiteralExpr]
     * @param expr The expression to visit
     */
    public fun visit(expr: LiteralExpr): T

    /**
     * Visit an [InExpr]
     * @param expr The expression to visit
     */
    public fun visit(expr: InExpr): T

    /**
     * Visit a [NotExpr]
     * @param expr The expression to visit
     */
    public fun visit(expr: NotExpr): T

    /**
     * Visit an [OrExpr]
     * @param expr The expression to visit
     */
    public fun visit(expr: OrExpr): T

    /**
     * Visit a [ScalarFuncExpr]
     * @param expr The expression to visit
     */
    public fun visit(expr: ScalarFuncExpr): T
}
