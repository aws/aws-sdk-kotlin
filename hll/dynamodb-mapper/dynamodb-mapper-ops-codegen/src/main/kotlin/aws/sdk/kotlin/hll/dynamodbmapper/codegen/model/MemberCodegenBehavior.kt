/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.util.Pkg

private val attrMapTypes = setOf(Types.AttributeMap, Types.AttributeMap.nullable())

/**
 * Describes a behavior to apply for a given [Member] in a low-level structure when generating code for an equivalent
 * high-level structure. This interface implements no behaviors on its own; it merely gives strongly-typed names to
 * behaviors that will be implemented by calling code.
 */
sealed interface MemberCodegenBehavior {
    companion object {
        /**
         * Identifies a [MemberCodegenBehavior] for the given [Member] by way of various heuristics
         * @param member The [Member] for which to identify a codegen behavior
         */
        fun identifyFor(member: Member) = when {
            member in unsupportedMembers -> Drop
            member.type in attrMapTypes -> if (member.name == "key") MapKeys else MapAll
            member.isTableName -> Hoist
            else -> PassThrough
        }
    }

    /**
     * Indicates that a member should be copied as-is from a low-level structure to a high-level equivalent (i.e., no
     * changes to name, type, etc. are required)
     */
    data object PassThrough : MemberCodegenBehavior

    /**
     * Indicates that a member is an attribute map which may contain _all_ attributes for a data type (as opposed to
     * only _key_ attributes) and should be replaced with a generic type (i.e., a `Map<String, AttributeValue>` member
     * in a low-level structure should be replaced with a generic `T` member in a high-level structure)
     */
    data object MapAll : MemberCodegenBehavior

    /**
     * Indicates that a member is an attribute map which contains _key_ attributes for a data type (as opposed to _all_
     * attributes) and should be replaced with a generic type (i.e., a `Map<String, AttributeValue>` member
     * in a low-level structure should be replaced with a generic `T` member in a high-level structure)
     */
    data object MapKeys : MemberCodegenBehavior

    /**
     * Indicates that a member is unsupported and should not be replicated from a low-level structure to the high-level
     * equivalent (e.g., a deprecated member that has been replaced with new features need not be carried forward)
     */
    data object Drop : MemberCodegenBehavior

    /**
     * Indicates that a member from a low-level structure should be "hoisted" outside its high-level equivalent. This is
     * similar to [Drop] but indicates that other codegen may use the member in different ways (e.g., a table name
     * parameter in a low-level structure may be hoisted to a different API but not added to the equivalent high-level
     * structure).
     */
    data object Hoist : MemberCodegenBehavior
}

/**
 * Identifies a [MemberCodegenBehavior] for this [Member] by way of various heuristics
 */
val Member.codegenBehavior: MemberCodegenBehavior
    get() = when {
        this in unsupportedMembers -> MemberCodegenBehavior.Drop
        type in attrMapTypes -> if (name == "key") MemberCodegenBehavior.MapKeys else MemberCodegenBehavior.MapAll
        isTableName -> MemberCodegenBehavior.Hoist
        else -> MemberCodegenBehavior.PassThrough
    }

private val Member.isTableName: Boolean
    get() = name == "tableName" && type == Types.StringNullable

private fun llType(name: String) = TypeRef(Pkg.Ll.Model, name)

private val unsupportedMembers = listOf(
    // superseded by ConditionExpression
    Member("conditionalOperator", llType("ConditionalOperator")),
    Member("expected", Type.stringMap(llType("ExpectedAttributeValue"))),

    // superseded by FilterExpression
    Member("queryFilter", Type.stringMap(llType("Condition"))),
    Member("scanFilter", Type.stringMap(llType("Condition"))),

    // superseded by KeyConditionExpression
    Member("keyConditions", Type.stringMap(llType("Condition"))),

    // superseded by ProjectionExpression
    Member("attributesToGet", Type.list(Types.String)),

    // superseded by UpdateExpression
    Member("attributeUpdates", Type.stringMap(llType("AttributeValueUpdate"))),

    // TODO add support for expressions
    Member("expressionAttributeNames", Type.stringMap(Types.String)),
    Member("expressionAttributeValues", Types.AttributeMap),
    Member("conditionExpression", Types.String),
    Member("projectionExpression", Types.String),
    Member("updateExpression", Types.String),
).map { member ->
    if (member.type is TypeRef) {
        member.copy(type = member.type.nullable())
    } else {
        member
    }
}.toSet()
