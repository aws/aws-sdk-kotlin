/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

/**
 * Represents a
 * [DynamoDB attribute data type](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes)
 * @param abbreviation The DynamoDB type name
 */
public enum class AttributeType(public val abbreviation: kotlin.String) {
    /**
     * Binary data type, denoted in DynamoDB as `B`
     */
    Binary("B"),

    /**
     * Binary set data type, denoted in DynamoDB as `BS`
     */
    BinarySet("BS"),

    /**
     * Boolean data type, denoted in DynamoDB as `BOOL`
     */
    Boolean("BOOL"),

    /**
     * List data type, denoted in DynamoDB as `L`
     */
    List("L"),

    /**
     * Map data type, denoted in DynamoDB as `M`
     */
    Map("M"),

    /**
     * Null data type, denoted in DynamoDB as `NULL`
     */
    Null("NULL"),

    /**
     * Number data type, denoted in DynamoDB as `N`
     */
    Number("N"),

    /**
     * Number set data type, denoted in DynamoDB as `NS`
     */
    NumberSet("NS"),

    /**
     * String data type, denoted in DynamoDB as `S`
     */
    String("S"),

    /**
     * String set data type, denoted in DynamoDB as `SS`
     */
    StringSet("SS"),
}
