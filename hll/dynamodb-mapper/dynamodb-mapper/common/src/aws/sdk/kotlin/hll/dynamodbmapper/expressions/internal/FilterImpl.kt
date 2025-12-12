/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.*

internal data object FilterImpl : Filter {
    // ATTRIBUTES
    override fun attr(name: String) = AttributePath(name)
    override fun AttributePath.get(index: Int) = AttributePath(index, parent = this)
    override fun AttributePath.get(key: String) = AttributePath(key, parent = this)

    // BINARY OPERATORS
    override fun Expression.eq(expr: Expression) = ComparisonExpr(Comparator.EQUALS, this, expr)
    override fun Expression.neq(expr: Expression) = ComparisonExpr(Comparator.NOT_EQUALS, this, expr)
    override fun Expression.lt(expr: Expression) = ComparisonExpr(Comparator.LESS_THAN, this, expr)
    override fun Expression.lte(expr: Expression) = ComparisonExpr(Comparator.LESS_THAN_OR_EQUAL, this, expr)
    override fun Expression.gt(expr: Expression) = ComparisonExpr(Comparator.GREATER_THAN, this, expr)
    override fun Expression.gte(expr: Expression) = ComparisonExpr(Comparator.GREATER_THAN_OR_EQUAL, this, expr)

    // RANGES & SETS
    override fun AttributePath.isBetween(min: Expression, max: Expression) = BetweenExpr(this, min, max)
    override fun AttributePath.isIn(set: Collection<Expression>) = InExpr(this, set)

    // FUNCTIONS
    override fun AttributePath.contains(expr: Expression) = BooleanFuncExpr(BooleanFunc.CONTAINS, this, expr)
    override fun AttributePath.exists() = BooleanFuncExpr(BooleanFunc.ATTRIBUTE_EXISTS, this)
    override fun AttributePath.notExists() = BooleanFuncExpr(BooleanFunc.ATTRIBUTE_NOT_EXISTS, this)
    override fun AttributePath.isOfType(type: AttributeType) = BooleanFuncExpr(BooleanFunc.ATTRIBUTE_TYPE, this, LiteralExpr(type.abbreviation))
    override val AttributePath.size get() = ScalarFuncExpr(ScalarFunc.SIZE, this)
    override fun AttributePath.startsWith(expr: Expression) = BooleanFuncExpr(BooleanFunc.BEGINS_WITH, this, expr)

    // BOOLEAN LOGIC
    override fun and(conditions: List<BooleanExpr>) = AndExpr(conditions)
    override fun not(condition: BooleanExpr) = NotExpr(condition)
    override fun or(conditions: List<BooleanExpr>) = OrExpr(conditions)
}
