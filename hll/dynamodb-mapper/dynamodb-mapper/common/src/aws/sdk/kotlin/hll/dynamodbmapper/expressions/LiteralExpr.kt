/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.LiteralExprImpl
import aws.sdk.kotlin.hll.dynamodbmapper.util.attr
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Represents an expression that consists of a single literal value
 */
public interface LiteralExpr : Expression {
    /**
     * The low-level DynamoDB representation of the literal value
     */
    public val value: AttributeValue

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new literal expression
 * @param value The low-level DynamoDB representation of the literal value
 */
public fun LiteralExpr(value: AttributeValue): LiteralExpr = LiteralExprImpl(value)

private val NULL_LITERAL = LiteralExpr(attr(null))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: Boolean?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: ByteArray?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: List<Any?>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: Map<String, Any?>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
@Suppress("UNUSED_PARAMETER")
public fun LiteralExpr(value: Nothing?): LiteralExpr = NULL_LITERAL

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: Number?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
@JvmName("LiteralExprSetByteArray")
public fun LiteralExpr(value: Set<ByteArray>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
@JvmName("LiteralExprSetNumber")
public fun LiteralExpr(value: Set<Number>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
@JvmName("LiteralExprSetString")
public fun LiteralExpr(value: Set<String>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
@JvmName("LiteralExprSetUByte")
public fun LiteralExpr(value: Set<UByte>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
@JvmName("LiteralExprSetUInt")
public fun LiteralExpr(value: Set<UInt>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
@JvmName("LiteralExprSetULong")
public fun LiteralExpr(value: Set<ULong>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
@JvmName("LiteralExprSetUShort")
public fun LiteralExpr(value: Set<UShort>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: String?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: UByte?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: UInt?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: ULong?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: UShort?): LiteralExpr = LiteralExpr(attr(value))
