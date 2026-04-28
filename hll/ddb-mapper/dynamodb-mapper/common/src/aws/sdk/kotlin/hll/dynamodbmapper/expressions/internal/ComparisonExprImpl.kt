/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.Comparator
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.ComparisonExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.Expression

internal data class ComparisonExprImpl(
    override val comparator: Comparator,
    override val left: Expression,
    override val right: Expression,
) : ComparisonExpr
