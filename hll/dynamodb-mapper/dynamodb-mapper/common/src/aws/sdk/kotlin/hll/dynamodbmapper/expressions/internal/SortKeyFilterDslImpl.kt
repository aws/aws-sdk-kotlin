/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.*

private data object SortKeyImpl : SortKey

internal data object SortKeyFilterDslImpl : SortKeyFilterDsl {
    override val sortKey: SortKey
        get() = SortKeyImpl

    override infix fun SortKey.eq(expr: LiteralExpr) =
        ComparisonExpr(Comparator.EQUALS, SkAttrPathImpl, expr)

    override infix fun SortKey.neq(expr: LiteralExpr) =
        ComparisonExpr(Comparator.NOT_EQUALS, SkAttrPathImpl, expr)

    override infix fun SortKey.lt(expr: LiteralExpr) =
        ComparisonExpr(Comparator.LESS_THAN, SkAttrPathImpl, expr)

    override infix fun SortKey.lte(expr: LiteralExpr) =
        ComparisonExpr(Comparator.LESS_THAN_OR_EQUAL, SkAttrPathImpl, expr)

    override infix fun SortKey.gt(expr: LiteralExpr) =
        ComparisonExpr(Comparator.GREATER_THAN, SkAttrPathImpl, expr)

    override infix fun SortKey.gte(expr: LiteralExpr) =
        ComparisonExpr(Comparator.GREATER_THAN_OR_EQUAL, SkAttrPathImpl, expr)

    override fun SortKey.isBetween(min: LiteralExpr, max: LiteralExpr) =
        BetweenExpr(SkAttrPathImpl, min, max)

    override infix fun SortKey.startsWith(expr: LiteralExpr) =
        BooleanFuncExpr(BooleanFunc.BEGINS_WITH, SkAttrPathImpl, expr)
}
