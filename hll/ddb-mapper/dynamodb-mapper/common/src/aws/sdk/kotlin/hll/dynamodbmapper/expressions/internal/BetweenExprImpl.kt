/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.BetweenExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.Expression

internal data class BetweenExprImpl(
    override val value: Expression,
    override val min: Expression,
    override val max: Expression,
) : BetweenExpr
