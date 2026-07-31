/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AttributePath
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.BooleanFunc
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.BooleanFuncExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.Expression

internal data class BooleanFuncExprImpl(
    override val func: BooleanFunc,
    override val path: AttributePath,
    override val additionalOperands: List<Expression> = listOf(),
) : BooleanFuncExpr
