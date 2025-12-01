/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

/**
 * Represents any kind of expression. This is an abstract top-level interface and describes no details about an
 * expression on its own. Expressions may be of various specific types (e.g., [AttributePath], [ComparisonExpr],
 * [AndExpr], etc.) each of which have specific data detailing the expression.
 *
 * [Expression] and its derivatives support the [visitor design pattern](https://en.wikipedia.org/wiki/Visitor_pattern)
 * by way of an [accept] method.
 */
public sealed interface Expression {
    /**
     * Accepts a visitor that is traversing an expression tree by dispatching to a subtype implementation. Subtype
     * implementations MUST call the [ExpressionVisitor.visit] overload for their concrete type (effectively forming a
     * [double dispatch](https://en.wikipedia.org/wiki/Double_dispatch)) and MUST return the resulting value.
     * @param visitor The [ExpressionVisitor] that is traversing an expression
     */
    public fun <T> accept(visitor: ExpressionVisitor<T>): T
}

/**
 * A subtype of [Expression] that represents a condition with a boolean value, such as would be used for filtering
 * items. This is a [marker interface](https://en.wikipedia.org/wiki/Marker_interface_pattern) which adds no additional
 * declarations.
 */
public sealed interface BooleanExpr : Expression

/**
 * A subtype of [Expression] that represents a key condition on a sort key, such as would be used for specifying a Query
 * key. This is a [marker interface](https://en.wikipedia.org/wiki/Marker_interface_pattern) which adds no additional
 * declarations.
 */
public sealed interface SortKeyExpr : Expression
