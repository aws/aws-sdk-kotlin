/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.*
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.util.dynamicAttr
import aws.sdk.kotlin.hll.dynamodbmapper.util.requireNull

internal data class KeyFilterImpl(override val partitionKey: Any, override val sortKey: SortKeyExpr?) : KeyFilter {
    init {
        require(
            partitionKey is ByteArray ||
                partitionKey is Number ||
                partitionKey is String ||
                partitionKey is UByte ||
                partitionKey is UInt ||
                partitionKey is ULong ||
                partitionKey is UShort,
        ) { "Partition key values must be either a ByteArray, Number, String, or an unsigned number type" }
    }
}

internal fun KeyFilter.toExpression(schema: ItemSchema<*>) = when (schema) {
    is ItemSchema.CompositeKey<*, *, *> -> {
        val pkCondition = pkCondition(schema, partitionKey)

        sortKey?.let { sortKey ->
            FilterImpl.run {
                val skAttr = attr(schema.sortKey.name)
                val skCondition = when (sortKey) {
                    is BetweenExpr -> BetweenExpr(skAttr, sortKey.min, sortKey.max)
                    is ComparisonExpr -> ComparisonExpr(sortKey.comparator, skAttr, sortKey.right)
                    is BooleanFuncExpr -> BooleanFuncExpr(sortKey.func, skAttr, sortKey.additionalOperands)
                }

                and(pkCondition, skCondition)
            }
        } ?: pkCondition
    }

    is ItemSchema.PartitionKey<*, *> -> {
        requireNull(sortKey) { "Sort key condition not allowed on schema without a sort key" }
        pkCondition(schema, partitionKey)
    }

    else -> error("Unknown schema type ${schema::class} (expected ItemSchema.CompositeKey or ItemSchema.PartitionKey)")
}

private fun pkCondition(schema: ItemSchema.PartitionKey<*, *>, partitionKey: Any) = FilterImpl.run { attr(schema.partitionKey.name) eq LiteralExpr(dynamicAttr(partitionKey)) }
