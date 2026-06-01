/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.LiteralExpr
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

internal data class LiteralExprImpl(override val value: AttributeValue) : LiteralExpr
