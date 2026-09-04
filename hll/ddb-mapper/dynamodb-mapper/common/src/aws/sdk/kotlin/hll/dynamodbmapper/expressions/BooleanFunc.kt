/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

/**
 * Identifies a
 * [DynamoDB expression function](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Functions)
 * which returns a boolean value
 * @param exprString The literal name of the function to use in expression strings
 */
public enum class BooleanFunc(public val exprString: String) {
    /**
     * The `attribute_exist` function
     */
    ATTRIBUTE_EXISTS("attribute_exists"),

    /**
     * The `attribute_not_exists` function
     */
    ATTRIBUTE_NOT_EXISTS("attribute_not_exists"),

    /**
     * The `attribute_type` function
     */
    ATTRIBUTE_TYPE("attribute_type"),

    /**
     * The `begins_with` function
     */
    BEGINS_WITH("begins_with"),

    /**
     * The `contains` function
     */
    CONTAINS("contains"),
}
