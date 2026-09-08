/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.AttributePath
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.Expression
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.UpdateAction
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.UpdateClauseExpr

internal data class UpdateClauseExprImpl(
    override val action: UpdateAction,
    override val target: AttributePath,
    override val value: Expression,
) : UpdateClauseExpr
