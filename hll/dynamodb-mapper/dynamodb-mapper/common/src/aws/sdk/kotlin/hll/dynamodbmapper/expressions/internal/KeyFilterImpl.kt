/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.*
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.attrs
import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.values
import aws.sdk.kotlin.hll.dynamodbmapper.util.dynamicAv

internal data class KeyFilterImpl(
    override val partitionKey: KeyType,
    override val sortKeyExpressions: List<SortKeyExpr>,
) : KeyFilter

internal fun KeyFilter.toExpression(schema: ItemSchema<*>): Expression {
    val conditions = when (schema) {
        is ItemSchema.PartitionKey<*, *> -> {
            require(sortKeyExpressions.isEmpty()) { "A sort key condition is not allowed on schema without a sort key" }
            pkConditions(schema.partitionKey, partitionKey)
        }

        is ItemSchema.CompositeKey<*, *, *> ->
            pkConditions(schema.partitionKey, partitionKey) + skConditions(schema.sortKey, sortKeyExpressions)
    }

    return if (conditions.size == 1) conditions.single() else FilterDslImpl.and(conditions)
}

private fun pkConditions(spec: KeySpec<*>, value: KeyType): List<BooleanExpr> = FilterDslImpl.run {
    val attrs = spec.attrs
    val values = value.values
    require(attrs.size == values.size) {
        "Provided number of partition keys (${values.size}) does not match the number of keys defined in the schema (${attrs.size})"
    }

    attrs.zip(values).map { (attr, value) ->
        attr(attr.name) eq LiteralExpr(dynamicAv(value))
    }
}

private fun skConditions(spec: KeySpec<*>, sortKeyExpressions: List<SortKeyExpr>): List<BooleanExpr> = FilterDslImpl.run {
    val attrs = spec.attrs
    require(attrs.size >= sortKeyExpressions.size) {
        "Provided number of sort key expressions (${sortKeyExpressions.size}) is greater than the number of keys defined in the schema (${attrs.size})"
    }

    attrs
        .zip(sortKeyExpressions)
        .map { (attr, expression) ->
            val skAttr = attr(attr.name)
            when (expression) {
                is BetweenExpr -> BetweenExpr(skAttr, expression.min, expression.max)
                is ComparisonExpr -> ComparisonExpr(expression.comparator, skAttr, expression.right)
                is BooleanFuncExpr -> BooleanFuncExpr(expression.func, skAttr, expression.additionalOperands)
            }
        }
}
