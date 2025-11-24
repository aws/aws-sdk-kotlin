/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.util.dynamicAttr

/**
 * A DSL interface providing support for "low-level" filter expressions. Implementations of this interface provide
 * methods and properties which create boolean expressions to filter item results (e.g., in Scan or Query operations).
 * Expressions are typically formed by getting a reference to an attribute path and then exercising some operation or
 * function upon it.
 *
 * For example:
 *
 * ```kotlin
 * filter {
 *     attr("foo") eq 42
 * }
 * ```
 *
 * This example creates an expression which checks whether an attribute named `foo` is equal to the value `42`.
 *
 * ## (Non-)Relationship to schema
 *
 * The expressions formed by [Filter] are referred to as a "low-level" filter expression. This is because they are not
 * restricted by or adherent to any defined schema. Instead, they are a DSL convenience layer over [literal DynamoDB
 * expression strings and expression attribute value maps](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.html).
 * As such they provide **minimal type correctness** and may allow you to form expressions which are invalid given the
 * shape of your data, such as attributes which don't exist, comparisons with mismatched data types, etc.
 *
 * # Attributes
 *
 * Every filter condition contains at least one attribute. Attributes are referenced by attribute paths, analogous to
 * [document paths in DynamoDB](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.Attributes.html#Expressions.Attributes.NestedElements.DocumentPathExamples).
 * Attribute paths consist of one or more elements, which are either names (e.g., of a top-level attribute or a nested
 * key in a map attribute) or indices (i.e., into a list). The first (and often only) element of an attribute path is a
 * name.
 *
 * ## Getting a top-level attribute
 *
 * All attribute paths start with a top-level attribute expression, created by the [attr] function:
 *
 * ```kotlin
 * attr("foo") // References the top-level attribute "foo"
 * ```
 *
 * Note, the attribute `foo` may not exist for a given item or for an entire table.
 *
 * ## Nesting
 *
 * Sometimes values are nested inside other attributes like lists and maps. Filter expressions can operate on those
 * nested values by forming a more detailed attribute path using the `[]` operator or [get] functions on a path.
 *
 * For example, consider an item structure such as:
 *
 * ```json
 * {
 *     "foo": "Hello",
 *     "bar": {
 *         "baz": [
 *             "Yay",
 *             null,
 *             42,
 *             true
 *         ]
 *     }
 * }
 * ```
 *
 * The value `"Yay"` can be referenced with the following DSL syntax:
 *
 * ```kotlin
 * attr("bar")["baz"][0]
 * ```
 *
 * That is, in the top-level attribute `bar`, in the value keyed by `baz`, the element at index `0`.
 *
 * # Equalities/inequalities
 *
 * A very common filter condition is verifying whether some attribute is equal (or unequal) to another value—either a
 * literal value or the value of another attribute. These comparisons are available by using the following functions:
 * * [eq] — checks if two values are equal (equivalent to Kotlin's `==` operator)
 * * [neq] — checks if two values are _not_ equal (equivalent to Kotlin's `!=` operator)
 * * [lt] — checks if a value is less than another value (equivalent to Kotlin's `<` operator)
 * * [lte] — checks if a value is less than _or equal to_ another value (equivalent to Kotlin's `<=` operator)
 * * [gt] — checks if a value is greater than another value (equivalent to Kotlin's `>` operator)
 * * [gte] — checks if a value is greater than _or equal to_ another value (equivalent to Kotlin's `>=` operator)
 *
 * For example:
 *
 * ```kotlin
 * attr("foo") eq 5           // Checks whether the value of attribute `foo` is `5`
 * attr("bar") gt attr("baz") // Checks whether the value of attribute `bar` is greater than attribute `baz`
 * ```
 *
 * # Ranges and sets
 *
 * Expressions can check whether some attribute value is in a given range or set of possible values. These checks are
 * available via the [isBetween] or [isIn] functions:
 *
 * ```kotlin
 * // Checks whether the value of attribute `foo` is between 40 and 60 (inclusive)
 * attr("foo") isIn 40..60
 *
 * // Checks whether the value of attribute `foo` is either 1, 2, 4, 8, 16, or 32
 * attr("bar") isIn setOf(1, 2, 4, 8, 16, 32)
 *
 * // Checks whether the value of attribute `baz` is between the value of `foo` and the value of `baz`
 * attr("baz").isBetween(attr("foo"), attr("baz"))
 * ```
 *
 * # Boolean logic
 *
 * The previous sections dealt with singular conditions (e.g., `a == b`, `c in (d, e, f)`, etc.). But complex queries
 * may involve multiple conditions or negative conditions akin to the boolean operations AND (`&&`), OR (`||`, and NOT
 * (`!`). This logic is available via the [and], [or], and [not] functions:
 *
 * ```kotlin
 * and(
 *     attr("foo") eq 42,
 *     attr("bar") neq 42,
 * )
 * ```
 *
 * This block checks for value of the attribute `foo` equalling `42` and the value of attribute `bar` _not_ equalling
 * `42`. This is logically equivalent to the Kotlin syntax:
 *
 * ```kotlin
 * (foo == 42 && bar != 42)
 * ```
 *
 * These boolean functions can be composed in various ways:
 *
 * ```kotlin
 * or(
 *     attr("foo") eq "apple",
 *     and(
 *         attr("bar") lt attr("baz"),
 *         attr("baz") gte 42,
 *     ),
 *     not(
 *         attr("qux") in setOf("ready", "set", "go"),
 *     ),
 * )
 * ```
 *
 * This complex DSL code checks that at least one of three conditions is met (boolean OR):
 * * The value of attribute `foo` is `"apple"`
 * * The value of attribute `bar` is less than the value of `baz` **–and–** the value of `baz` is greater/equal to `42`
 * * The value of attribute `qux` is not one of `"ready"`, `"steady"`, or `"go"`
 *
 * This is logically equivalent to the Kotlin syntax:
 *
 * ```kotlin
 * (foo == "apple") || (bar < baz && baz >= 42) || qux !in setOf("ready", "steady", "go")
 * ```
 *
 * # Other functions/properties
 *
 * Several additional filter expressions are possible via the following methods/properties:
 *
 * * [contains] — Checks if a string/list attribute value contains the given value
 * * [exists] — Checks if _any value_ (including `null`) exists for an attribute. The low-level DynamoDB function for
 *   this is `attribute_exists`.
 * * [notExists] — Checks if no value is present for an attribute (i.e., the attribute is "undefined" for an item). The
 *   low-level DynamoDB function for this is `attribute_not_exists`.
 * * [isOfType] — Checks if an attribute value is of the given type. The low-level DynamoDB function for this is
 *   `attribute_type`.
 * * [size] — Gets the size of an attribute (e.g., number of elements in list/map/set, the length of a string, etc.)
 * * [startsWith] — Checks if a string attribute value starts with the given value. The low-level DynamoDB function for
 *   this is `begins_with`.
 *
 * For example:
 *
 * ```kotlin
 * attr("foo") contains 13 // Checks whether the value of attribute `foo` contains the value `13`
 * attr("bar").exists()    // Checks whether any value exists for `bar` (including `null`)
 * ```
 */
public interface Filter {
    // ATTRIBUTES

    /**
     * Creates an attribute path reference from a top-level attribute name. To reference nested properties or indexed
     * elements, use the `[]` operator or [AttributePath.get].
     * @param name The top-level attribute name
     */
    public fun attr(name: String): AttributePath

    /**
     * Creates an attribute path reference for an index into a list or set
     * @param index The index to dereference
     */
    public operator fun AttributePath.get(index: Int): AttributePath

    /**
     * Creates an attribute path reference for a key in map
     * @param key The key to dereference
     */
    public operator fun AttributePath.get(key: String): AttributePath

    // BINARY OPERATORS

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param expr The other expression in the comparison
     */
    public infix fun Expression.eq(expr: Expression): BooleanExpr

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.eq(value: Boolean?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.eq(value: ByteArray?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.eq(value: List<Any?>?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.eq(value: Map<String, Any?>?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.eq(value: Nothing?): BooleanExpr = eq(LiteralExpr(null))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.eq(value: Number?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("eqSetByteArray")
    public infix fun Expression.eq(value: Set<ByteArray>?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("eqSetNumber")
    public infix fun <N : Number> Expression.eq(value: Set<N>?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("eqSetString")
    public infix fun Expression.eq(value: Set<String>?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("eqSetUByte")
    public infix fun Expression.eq(value: Set<UByte>?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("eqSetUInt")
    public infix fun Expression.eq(value: Set<UInt>?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("eqSetULong")
    public infix fun Expression.eq(value: Set<ULong>?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("eqSetUShort")
    public infix fun Expression.eq(value: Set<UShort>?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.eq(value: String?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.eq(value: UByte?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.eq(value: UInt?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.eq(value: ULong?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying two expressions are equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.eq(value: UShort?): BooleanExpr = eq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param expr The other expression in the comparison
     */
    public infix fun Expression.neq(expr: Expression): BooleanExpr

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.neq(value: Boolean?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.neq(value: ByteArray?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.neq(value: List<Any?>?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.neq(value: Map<String, Any?>?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.neq(value: Nothing?): BooleanExpr = neq(LiteralExpr(null))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.neq(value: Number?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("neqSetByteArray")
    public infix fun Expression.neq(value: Set<ByteArray>?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("neqSetNumber")
    public infix fun <N : Number> Expression.neq(value: Set<N>?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("neqSetString")
    public infix fun Expression.neq(value: Set<String>?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("neqSetUByte")
    public infix fun Expression.neq(value: Set<UByte>?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("neqSetUInt")
    public infix fun Expression.neq(value: Set<UInt>?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("neqSetULong")
    public infix fun Expression.neq(value: Set<ULong>?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("neqSetUShort")
    public infix fun Expression.neq(value: Set<UShort>?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.neq(value: String?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.neq(value: UByte?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.neq(value: UInt?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.neq(value: ULong?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying two expressions are not equal to each other
     * @param value The other value in the comparison
     */
    public infix fun Expression.neq(value: UShort?): BooleanExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is less than another expression
     * @param expr The other expression in the comparison
     */
    public infix fun Expression.lt(expr: Expression): BooleanExpr

    /**
     * Creates an inequality expression for verifying this expression is less than another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.lt(value: ByteArray): BooleanExpr = lt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is less than another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.lt(value: Number): BooleanExpr = lt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is less than another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.lt(value: String): BooleanExpr = lt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is less than another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.lt(value: UByte): BooleanExpr = lt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is less than another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.lt(value: UInt): BooleanExpr = lt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is less than another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.lt(value: ULong): BooleanExpr = lt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is less than another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.lt(value: UShort): BooleanExpr = lt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is less than or equal to another expression
     * @param expr The other expression in the comparison
     */
    public infix fun Expression.lte(expr: Expression): BooleanExpr

    /**
     * Creates an inequality expression for verifying this expression is less than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.lte(value: ByteArray): BooleanExpr = lte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is less than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.lte(value: Number): BooleanExpr = lte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is less than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.lte(value: String): BooleanExpr = lte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is less than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.lte(value: UByte): BooleanExpr = lte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is less than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.lte(value: UInt): BooleanExpr = lte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is less than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.lte(value: ULong): BooleanExpr = lte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is less than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.lte(value: UShort): BooleanExpr = lte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is greater than another expression
     * @param expr The other expression in the comparison
     */
    public infix fun Expression.gt(expr: Expression): BooleanExpr

    /**
     * Creates an inequality expression for verifying this expression is greater than another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.gt(value: ByteArray): BooleanExpr = gt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is greater than another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.gt(value: Number): BooleanExpr = gt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is greater than another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.gt(value: String): BooleanExpr = gt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is greater than another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.gt(value: UByte): BooleanExpr = gt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is greater than another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.gt(value: UInt): BooleanExpr = gt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is greater than another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.gt(value: ULong): BooleanExpr = gt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is greater than another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.gt(value: UShort): BooleanExpr = gt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is greater than or equal to another expression
     * @param expr The other expression in the comparison
     */
    public infix fun Expression.gte(expr: Expression): BooleanExpr

    /**
     * Creates an inequality expression for verifying this expression is greater than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.gte(value: ByteArray): BooleanExpr = gte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is greater than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.gte(value: Number): BooleanExpr = gte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is greater than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.gte(value: String): BooleanExpr = gte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is greater than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.gte(value: UByte): BooleanExpr = gte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is greater than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.gte(value: UInt): BooleanExpr = gte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is greater than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.gte(value: ULong): BooleanExpr = gte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying this expression is greater than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun Expression.gte(value: UShort): BooleanExpr = gte(LiteralExpr(value))

    // RANGES & SETS

    /**
     * Creates a range expression for verifying this expression is between two other expressions
     * @param min The lower bound expression
     * @param max The upper bound expression (inclusive)
     */
    public fun AttributePath.isBetween(min: Expression, max: Expression): BooleanExpr

    /**
     * Creates a range expression for verifying this expression is between two other expressions
     * @param min The lower bound value
     * @param max The upper bound value (inclusive)
     */
    public fun AttributePath.isBetween(min: ByteArray, max: ByteArray): BooleanExpr =
        isBetween(LiteralExpr(min), LiteralExpr(max))

    // TODO The following overloads support [ClosedRange] but [OpenEndRange] also exists. DynamoDB expressions don't
    //  support it directly but we may be able to cheese it with two inequalities ANDed together.

    /**
     * Creates a range expression for verifying this expression is in the given range
     * @param range The range to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInRangeNumber")
    public infix fun <N> AttributePath.isIn(range: ClosedRange<N>): BooleanExpr where N : Number, N : Comparable<N> =
        isBetween(LiteralExpr(range.start), LiteralExpr(range.endInclusive))

    /**
     * Creates a range expression for verifying this expression is in the given range
     * @param range The range to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInRangeString")
    public infix fun AttributePath.isIn(range: ClosedRange<String>): BooleanExpr =
        isBetween(LiteralExpr(range.start), LiteralExpr(range.endInclusive))

    /**
     * Creates a range expression for verifying this expression is in the given range
     * @param range The range to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInRangeUByte")
    public infix fun AttributePath.isIn(range: ClosedRange<UByte>): BooleanExpr =
        isBetween(LiteralExpr(range.start), LiteralExpr(range.endInclusive))

    /**
     * Creates a range expression for verifying this expression is in the given range
     * @param range The range to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInRangeUInt")
    public infix fun AttributePath.isIn(range: ClosedRange<UInt>): BooleanExpr =
        isBetween(LiteralExpr(range.start), LiteralExpr(range.endInclusive))

    /**
     * Creates a range expression for verifying this expression is in the given range
     * @param range The range to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInRangeULong")
    public infix fun AttributePath.isIn(range: ClosedRange<ULong>): BooleanExpr =
        isBetween(LiteralExpr(range.start), LiteralExpr(range.endInclusive))

    /**
     * Creates a range expression for verifying this expression is in the given range
     * @param range The range to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInRangeUShort")
    public infix fun AttributePath.isIn(range: ClosedRange<UShort>): BooleanExpr =
        isBetween(LiteralExpr(range.start), LiteralExpr(range.endInclusive))

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionExpression")
    public infix fun AttributePath.isIn(set: Collection<Expression>): BooleanExpr

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionByteArray")
    public infix fun AttributePath.isIn(set: Collection<ByteArray?>): BooleanExpr = isIn(set.map(::LiteralExpr))

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionList")
    public infix fun AttributePath.isIn(set: Collection<List<Any?>?>): BooleanExpr = isIn(set.map(::LiteralExpr))

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionMap")
    public infix fun AttributePath.isIn(set: Collection<Map<String, Any?>?>): BooleanExpr = isIn(set.map(::LiteralExpr))

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionNumber")
    public infix fun <N : Number> AttributePath.isIn(set: Collection<N?>): BooleanExpr = isIn(set.map(::LiteralExpr))

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionSetByteArray")
    public infix fun AttributePath.isIn(set: Collection<Set<ByteArray>?>): BooleanExpr = isIn(set.map(::LiteralExpr))

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionSetNumber")
    public infix fun <N : Number> AttributePath.isIn(set: Collection<Set<N>?>): BooleanExpr = isIn(set.map(::LiteralExpr))

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionSetString")
    public infix fun AttributePath.isIn(set: Collection<Set<String>?>): BooleanExpr = isIn(set.map(::LiteralExpr))

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionSetUByte")
    public infix fun AttributePath.isIn(set: Collection<Set<UByte>?>): BooleanExpr = isIn(set.map(::LiteralExpr))

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionSetUInt")
    public infix fun AttributePath.isIn(set: Collection<Set<UInt>?>): BooleanExpr = isIn(set.map(::LiteralExpr))

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionSetULong")
    public infix fun AttributePath.isIn(set: Collection<Set<ULong>?>): BooleanExpr = isIn(set.map(::LiteralExpr))

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionSetUShort")
    public infix fun AttributePath.isIn(set: Collection<Set<UShort>?>): BooleanExpr = isIn(set.map(::LiteralExpr))

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionString")
    public infix fun AttributePath.isIn(set: Collection<String?>): BooleanExpr = isIn(set.map(::LiteralExpr))

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionUByte")
    public infix fun AttributePath.isIn(set: Collection<UByte?>): BooleanExpr = isIn(set.map(::LiteralExpr))

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionUInt")
    public infix fun AttributePath.isIn(set: Collection<UInt?>): BooleanExpr = isIn(set.map(::LiteralExpr))

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionULong")
    public infix fun AttributePath.isIn(set: Collection<ULong?>): BooleanExpr = isIn(set.map(::LiteralExpr))

    /**
     * Creates a contains expression for verifying this expression is in the given set of elements
     * @param set The collection to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInCollectionUShort")
    public infix fun AttributePath.isIn(set: Collection<UShort?>): BooleanExpr = isIn(set.map(::LiteralExpr))

    // FUNCTIONS

    /**
     * Creates a contains expression for verifying this expression contains the given expression
     * @param expr The expression which may be contained
     */
    public infix fun AttributePath.contains(expr: Expression): BooleanExpr

    /**
     * Creates a contains expression for verifying this expression contains the given expression
     * @param value The value which may be contained
     */
    public infix fun AttributePath.contains(value: Any?): BooleanExpr = contains(LiteralExpr(dynamicAttr(value)))

    /**
     * Creates an expression for verifying an attribute exists
     */
    public fun AttributePath.exists(): BooleanExpr

    /**
     * Creates an expression for verifying an attribute does not exist
     */
    public fun AttributePath.notExists(): BooleanExpr

    /**
     * Creates an expression for verifying an attribute exists
     * @param type The [AttributeType] to test for
     */
    public infix fun AttributePath.isOfType(type: AttributeType): BooleanExpr

    /**
     * Creates an expression for getting the size (or length) of an attribute
     */
    public val AttributePath.size: Expression

    /**
     * Creates an expression for verifying this attribute starts with the given expression
     * @param expr The expression to test for at the beginning of this attribute
     */
    public infix fun AttributePath.startsWith(expr: Expression): BooleanExpr

    /**
     * Creates an expression for verifying this attribute starts with the given expression
     * @param value The value to test for at the beginning of this attribute
     */
    public infix fun AttributePath.startsWith(value: ByteArray): BooleanExpr = startsWith(LiteralExpr(value))

    /**
     * Creates an expression for verifying this attribute starts with the given expression
     * @param value The value to test for at the beginning of this attribute
     */
    public infix fun AttributePath.startsWith(value: String): BooleanExpr = startsWith(LiteralExpr(value))

    // BOOLEAN LOGIC

    /**
     * Creates a boolean expression for verifying that multiple conditions are all met
     * @param conditions A list of 2 or more conditions
     */
    public fun and(conditions: List<BooleanExpr>): BooleanExpr

    /**
     * Creates a boolean expression for verifying that multiple conditions are all met
     * @param conditions A list of 2 or more conditions
     */
    public fun and(vararg conditions: BooleanExpr): BooleanExpr = and(conditions.toList())

    /**
     * Creates a boolean expression for verifying the opposite of a condition is met
     * @param condition The condition to negate
     */
    public fun not(condition: BooleanExpr): BooleanExpr

    /**
     * Creates a boolean expression for verifying that at least one of several conditions is met
     * @param conditions A list of 2 or more conditions
     */
    public fun or(conditions: List<BooleanExpr>): BooleanExpr

    /**
     * Creates a boolean expression for verifying that at least one of several conditions is met
     * @param conditions A list of 2 or more conditions
     */
    public fun or(vararg conditions: BooleanExpr): BooleanExpr = or(conditions.toList())
}
