/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AndExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.BooleanExpr

internal data class AndExprImpl(override val operands: List<BooleanExpr>) : AndExpr {
    init {
        require(operands.size > 1) { "AND operations require two or more operands" }
    }
}
