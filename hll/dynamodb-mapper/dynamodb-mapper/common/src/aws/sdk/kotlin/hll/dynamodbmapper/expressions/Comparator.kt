/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

/**
 * Identifies a comparison operator to use in an expression
 * @param exprString The literal value of the operator to use in an expression string
 */
public enum class Comparator(public val exprString: String) {
    /**
     * An equality comparison, equivalent to `==` in Kotlin and to `=` in DynamoDB
     */
    EQUALS("="),

    /**
     * An inequality comparison, equivalent to `!=` in Kotlin and to `<>` in DynamoDB
     */
    NOT_EQUALS("<>"),

    /**
     * A less-than comparison, equivalent to `<` in Kotlin and DynamoDB
     */
    LESS_THAN("<"),

    /**
     * A less-than-or-equal-to comparison, equivalent to `<=` in Kotlin and DynamoDB
     */
    LESS_THAN_OR_EQUAL("<="),

    /**
     * A greater-than comparison, equivalent to `>` in Kotlin and DynamoDB
     */
    GREATER_THAN(">"),

    /**
     * A greater-than-or-equal-to comparison, equivalent to `>=` in Kotlin and DynamoDB
     */
    GREATER_THAN_OR_EQUAL(">="),
}
