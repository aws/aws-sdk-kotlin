/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.*

internal data class ScalarFuncExprImpl(
    override val func: ScalarFunc,
    override val initialOperand: Expression,
    override val additionalOperands: List<Expression> = listOf(),
) : ScalarFuncExpr
