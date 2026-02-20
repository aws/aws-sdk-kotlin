/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.*
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperPkg
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.ExpressionArgumentsType.AttributeNames
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.ExpressionArgumentsType.AttributeValues
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.ExpressionLiteralType.Filter
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.ExpressionLiteralType.KeyCondition
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.MemberCodegenBehavior.*

/**
 * Describes a behavior to apply for a given [Member] in a low-level structure when generating code for an equivalent
 * high-level structure. This interface implements no behaviors on its own; it merely gives strongly-typed names to
 * behaviors that will be implemented by calling code.
 */
internal sealed interface MemberCodegenBehavior {
    /**
     * Indicates that a member should be copied as-is from a low-level structure to a high-level equivalent (i.e., no
     * changes to name, type, etc. are required)
     */
    data object PassThrough : MemberCodegenBehavior

    /**
     * Indicates that a member is an attribute map which may contain _all_ attributes for a data type (as opposed to
     * only _key_ attributes) and should be mapped from a generic item type (i.e., a `Map<String, AttributeValue>`
     * member) in a low-level structure to a generic `T` member in a high-level structure
     */
    data object MapToObject : MemberCodegenBehavior

    /**
     * Indicates that a member is an attribute map which contains _key_ attributes for a data type (as opposed to _all_
     * attributes) and should be mapped from a generic item type (i.e., a `Map<String, AttributeValue>` member) in a
     * low-level structure to `KeyType` derivations in a high-level structure
     */
    data object MapToKeys : MemberCodegenBehavior

    /**
     * Indicates that a member is a list of attribute maps which may contain attributes for a data type and should be
     * mapped from a generic list of items (i.e., a `List<Map<String, AttributeValue>>` member) in a low-level structure
     * to a generic `List<T>` member in a high-level structure
     */
    data object ListMapToObject : MemberCodegenBehavior

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

    /**
     * Indicates that a member is a string expression parameter which should be replaced by an expression DSL
     * @param type The type of expression this member models
     */
    data class ExpressionLiteral(val type: ExpressionLiteralType) : MemberCodegenBehavior

    /**
     * Indicates that a member is a map of expression arguments which should be automatically handled by an expression
     * DSL
     * @param type The type of expression arguments this member models
     */
    data class ExpressionArguments(val type: ExpressionArgumentsType) : MemberCodegenBehavior
}

/**
 * Identifies a type of expression literal supported by DynamoDB APIs
 */
internal enum class ExpressionLiteralType {
    Condition,
    Filter,
    KeyCondition,
    Projection,
    Update,
}

/**
 * Identifies a type of expression arguments supported by DynamoDB APIs
 */
internal enum class ExpressionArgumentsType {
    AttributeNames,
    AttributeValues,
}

/**
 * Indicates whether this behavior represents one of the expression types (e.g., literal or arguments)
 */
internal val MemberCodegenBehavior.isExpression: Boolean
    get() = this is ExpressionLiteral || this is ExpressionArguments

/**
 * Identifies a [MemberCodegenBehavior] for this [Member] by way of various heuristics
 */
internal val Member.codegenBehavior: MemberCodegenBehavior
    get() = attributes.getOrNull(MapperAttributes.CodegenBehavior)
        ?: rules.firstNotNullOfOrNull { it.matchedBehaviorOrNull(this) }
        ?: PassThrough

private fun llType(name: String) = TypeRef(MapperPkg.Ll.Model, name)

private data class Rule(
    val namePredicate: (String) -> Boolean,
    val typePredicate: (TypeRef) -> Boolean,
    val behavior: MemberCodegenBehavior,
) {
    constructor(name: String, type: TypeRef, behavior: MemberCodegenBehavior) :
        this(name::equals, type::isEquivalentTo, behavior)

    constructor(name: Regex, type: TypeRef, behavior: MemberCodegenBehavior) :
        this(name::matches, type::isEquivalentTo, behavior)

    fun matchedBehaviorOrNull(member: Member) = if (matches(member)) behavior else null
    fun matches(member: Member) = namePredicate(member.name) && typePredicate(member.type as TypeRef)
}

private fun Type.isEquivalentTo(other: Type): Boolean = when (this) {
    is TypeVar -> other is TypeVar && shortName == other.shortName
    is TypeRef ->
        other is TypeRef &&
            fullName == other.fullName &&
            genericArgs.size == other.genericArgs.size &&
            genericArgs.zip(other.genericArgs).all { (thisArg, otherArg) -> thisArg.isEquivalentTo(otherArg) }
}

/**
 * Priority-ordered list of dispositions to apply to members found in structures. The first element from this list that
 * successfully matches with a member will be chosen.
 */
private val rules = listOf(
    // Deprecated expression members not to be carried forward into HLL
    Rule("conditionalOperator", llType("ConditionalOperator"), Drop),
    Rule("expected", Types.Kotlin.stringMap(llType("ExpectedAttributeValue")), Drop),
    Rule("queryFilter", Types.Kotlin.stringMap(llType("Condition")), Drop),
    Rule("scanFilter", Types.Kotlin.stringMap(llType("Condition")), Drop),
    Rule("keyConditions", Types.Kotlin.stringMap(llType("Condition")), Drop),
    Rule("attributesToGet", Types.Kotlin.list(Types.Kotlin.String), Drop),
    Rule("attributeUpdates", Types.Kotlin.stringMap(llType("AttributeValueUpdate")), Drop),

    // FIXME Add support for workloadProfileName as a pass-through field by applying a @Synthetic annotation to
    //  pass-through members and ignoring API validation for them
    Rule("workloadProfileName", Types.Kotlin.String, Drop),

    // Hoisted members
    Rule("tableName", Types.Kotlin.String, Hoist),
    Rule("indexName", Types.Kotlin.String, Hoist),

    // Expression literals
    Rule("keyConditionExpression", Types.Kotlin.String, ExpressionLiteral(KeyCondition)),
    Rule("filterExpression", Types.Kotlin.String, ExpressionLiteral(Filter)),

    // TODO add support for remaining expression types
    Rule("conditionExpression", Types.Kotlin.String, Drop),
    Rule("projectionExpression", Types.Kotlin.String, Drop),
    Rule("updateExpression", Types.Kotlin.String, Drop),

    // Expression arguments
    Rule("expressionAttributeNames", Types.Kotlin.stringMap(Types.Kotlin.String), ExpressionArguments(AttributeNames)),
    Rule("expressionAttributeValues", MapperTypes.AttributeMap, ExpressionArguments(AttributeValues)),

    // Mappable members
    Rule(".*".toRegex(), Types.Kotlin.list(MapperTypes.AttributeMap), ListMapToObject),
    Rule("key|lastEvaluatedKey|exclusiveStartKey".toRegex(), MapperTypes.AttributeMap, MapToKeys),
    Rule(".*".toRegex(), MapperTypes.AttributeMap, MapToObject),
)
