/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AdditiveExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AdditiveOperation
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.Expression

internal data class AdditiveExprImpl(
    override val operation: AdditiveOperation,
    override val left: Expression,
    override val right: Expression,
) : AdditiveExpr
