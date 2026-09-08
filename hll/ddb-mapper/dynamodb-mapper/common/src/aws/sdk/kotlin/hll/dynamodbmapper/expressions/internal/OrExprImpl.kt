/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.BooleanExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.OrExpr

internal data class OrExprImpl(override val operands: List<BooleanExpr>) : OrExpr {
    init {
        require(operands.size > 1) { "OR operations require two or more operands" }
    }
}
