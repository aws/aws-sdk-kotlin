/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.Expression
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.InExpr

internal data class InExprImpl(override val value: Expression, override val set: Collection<Expression>) : InExpr
