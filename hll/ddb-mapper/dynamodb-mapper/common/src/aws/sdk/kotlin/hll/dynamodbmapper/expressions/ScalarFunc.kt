/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

/**
 * Identifies a
 * [DynamoDB expression function](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Functions)
 * which returns a non-boolean value
 * @param exprString The literal name of the function to use in expression strings
 */
public enum class ScalarFunc(public val exprString: String) {
    /**
     * The [`if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites)
     * function
     */
    IF_NOT_EXISTS("if_not_exists"),

    /**
     * The [`list_append`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.UpdatingListElements)
     * function
     */
    LIST_APPEND("list_append"),

    /**
     * The [`size`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Functions)
     * function
     */
    SIZE("size"),
}
