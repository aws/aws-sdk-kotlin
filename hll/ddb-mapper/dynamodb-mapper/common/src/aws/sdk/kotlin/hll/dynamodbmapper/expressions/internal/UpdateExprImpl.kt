/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.UpdateClauseExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.UpdateExpr

internal data class UpdateExprImpl(
    override val set: UpdateExpr.Clause,
    override val remove: UpdateExpr.Clause,
    override val add: UpdateExpr.Clause,
    override val delete: UpdateExpr.Clause,
) : UpdateExpr

internal data class UpdateExprClauseImpl(override val updates: List<UpdateClauseExpr>) : UpdateExpr.Clause
