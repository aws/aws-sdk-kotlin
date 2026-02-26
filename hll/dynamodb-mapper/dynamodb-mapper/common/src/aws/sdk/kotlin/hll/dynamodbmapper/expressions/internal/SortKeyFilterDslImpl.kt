/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.*

private data object SortKeyImpl : SortKey

internal val placeholderExpr = AttributePath("~~PLACEHOLDER~~")

internal data object SortKeyFilterDslImpl : SortKeyFilterDsl {
    override val sortKey: SortKey
        get() = SortKeyImpl

    override infix fun SortKey.eq(expr: LiteralExpr) = ComparisonExpr(Comparator.EQUALS, placeholderExpr, expr)

    override infix fun SortKey.neq(expr: LiteralExpr) = ComparisonExpr(Comparator.NOT_EQUALS, placeholderExpr, expr)

    override infix fun SortKey.lt(expr: LiteralExpr) = ComparisonExpr(Comparator.LESS_THAN, placeholderExpr, expr)

    override infix fun SortKey.lte(expr: LiteralExpr) = ComparisonExpr(Comparator.LESS_THAN_OR_EQUAL, placeholderExpr, expr)

    override infix fun SortKey.gt(expr: LiteralExpr) = ComparisonExpr(Comparator.GREATER_THAN, placeholderExpr, expr)

    override infix fun SortKey.gte(expr: LiteralExpr) = ComparisonExpr(Comparator.GREATER_THAN_OR_EQUAL, placeholderExpr, expr)

    override fun SortKey.isBetween(min: LiteralExpr, max: LiteralExpr) = BetweenExpr(placeholderExpr, min, max)

    override infix fun SortKey.startsWith(expr: LiteralExpr) = BooleanFuncExpr(BooleanFunc.BEGINS_WITH, placeholderExpr, expr)
}
