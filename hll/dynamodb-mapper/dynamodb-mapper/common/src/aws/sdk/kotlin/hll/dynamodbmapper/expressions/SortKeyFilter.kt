/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

/**
 * Represents a sort key independent of schema
 */
public interface SortKey

/**
 * A DSL interface providing support for "low-level" sort key filter expressions. Implementations of this interface
 * provide methods and properties which create sort key expressions to narrow results in Query operations. Expressions
 * are formed by referencing [sortKey] and then exercising some function upon it.
 *
 * For example:
 *
 * ```kotlin
 * { sortKey eq 42 }
 * ```
 *
 * This example creates an expression which checks whether the sort key is equal to the value `42`.
 *
 * ## (Non-)Relationship to schema
 *
 * The expressions formed by [SortKeyFilter] are referred to as a "low-level" filter expression. This is because they
 * are not restricted by or adherent to any defined schema. Instead, they are a DSL convenience layer over [literal
 * DynamoDB expression strings and expression attribute value maps](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Query.KeyConditionExpressions.html).
 * As such they provide **minimal type correctness** and may allow you to form expressions which are invalid given the
 * shape of your data, such with mismatched data types.
 *
 * # Equalities/inequalities
 *
 * A very common filter condition is verifying whether the value of the sort key is equal (or unequal) to another a
 * literal value. These comparisons are available by using the following functions:
 * * [eq] — checks if the sort key is equal to another value (equivalent to Kotlin's `==` operator)
 * * [neq] — checks if two the sort key is _not_ equal to another value (equivalent to Kotlin's `!=` operator)
 * * [lt] — checks if the sort key is less than another value (equivalent to Kotlin's `<` operator)
 * * [lte] — checks if the sort key is less than _or equal to_ another value (equivalent to Kotlin's `<=` operator)
 * * [gt] — checks if the sort key is greater than another value (equivalent to Kotlin's `>` operator)
 * * [gte] — checks if the sort key is greater than _or equal to_ another value (equivalent to Kotlin's `>=` operator)
 *
 * For example:
 *
 * ```kotlin
 * sortKey eq 5     // Checks whether the value of the sort key is `5`
 * sortKey gt "baz" // Checks whether the value of the sort key is greater than `"baz"`
 * ```
 *
 * # Ranges and sets
 *
 * Expressions can check whether the value of the sort key is in a given range possible values. These checks are
 * available via the [isBetween] or [isIn] functions:
 *
 * ```kotlin
 * // Checks whether the value of the sort key is between 40 and 60 (inclusive)
 * sortKey isIn 40..60
 *
 * // Checks whether the value of the sort key is between two binary values (inclusive)
 * val minBinary: ByteArray = ...
 * val maxBinary: ByteArray = ...
 * sortKey.isBetween(minBinary, maxBinary)
 * ```
 *
 * # Prefixes
 *
 * The [startsWith] function checks for a prefix in the value of the sort key. For example:
 *
 * ```kotlin
 * sortKey startsWith "abc" // Checks whether the value of the sort key starts with `"abc"`
 * ```
 */
public interface SortKeyFilter {
    /**
     * Gets an attribute reference to the sort key
     */
    public val sortKey: SortKey

    /**
     * Creates an equality expression for verifying the sort key is equal to another expression
     * @param expr The other expression in the comparison
     */
    public infix fun SortKey.eq(expr: LiteralExpr): SortKeyExpr

    /**
     * Creates an equality expression for verifying the sort key is equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.eq(value: ByteArray): SortKeyExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying the sort key is equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.eq(value: Number): SortKeyExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying the sort key is equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.eq(value: String): SortKeyExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying the sort key is equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.eq(value: UByte): SortKeyExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying the sort key is equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.eq(value: UInt): SortKeyExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying the sort key is equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.eq(value: ULong): SortKeyExpr = eq(LiteralExpr(value))

    /**
     * Creates an equality expression for verifying the sort key is equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.eq(value: UShort): SortKeyExpr = eq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is not equal to another expression
     * @param expr The other expression in the comparison
     */
    public infix fun SortKey.neq(expr: LiteralExpr): SortKeyExpr

    /**
     * Creates an inequality expression for verifying the sort key is not equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.neq(value: ByteArray): SortKeyExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is not equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.neq(value: Number): SortKeyExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is not equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.neq(value: String): SortKeyExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is not equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.neq(value: UByte): SortKeyExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is not equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.neq(value: UInt): SortKeyExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is not equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.neq(value: ULong): SortKeyExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is not equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.neq(value: UShort): SortKeyExpr = neq(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is less than another expression
     * @param expr The other expression in the comparison
     */
    public infix fun SortKey.lt(expr: LiteralExpr): SortKeyExpr

    /**
     * Creates an inequality expression for verifying the sort key is less than another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.lt(value: ByteArray): SortKeyExpr = lt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is less than another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.lt(value: Number): SortKeyExpr = lt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is less than another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.lt(value: String): SortKeyExpr = lt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is less than another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.lt(value: UByte): SortKeyExpr = lt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is less than another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.lt(value: UInt): SortKeyExpr = lt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is less than another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.lt(value: ULong): SortKeyExpr = lt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is less than another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.lt(value: UShort): SortKeyExpr = lt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is less than or equal to another expression
     * @param expr The other expression in the comparison
     */
    public infix fun SortKey.lte(expr: LiteralExpr): SortKeyExpr

    /**
     * Creates an inequality expression for verifying the sort key is less than or equal to another expression
     * @param expr The other expression in the comparison
     */
    public infix fun SortKey.lte(value: ByteArray): SortKeyExpr = lte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is less than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.lte(value: Number): SortKeyExpr = lte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is less than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.lte(value: String): SortKeyExpr = lte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is less than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.lte(value: UByte): SortKeyExpr = lte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is less than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.lte(value: UInt): SortKeyExpr = lte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is less than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.lte(value: ULong): SortKeyExpr = lte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is less than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.lte(value: UShort): SortKeyExpr = lte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is greater than another expression
     * @param expr The other expression in the comparison
     */
    public infix fun SortKey.gt(expr: LiteralExpr): SortKeyExpr

    /**
     * Creates an inequality expression for verifying the sort key is greater than another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.gt(value: ByteArray): SortKeyExpr = gt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is greater than another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.gt(value: Number): SortKeyExpr = gt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is greater than another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.gt(value: String): SortKeyExpr = gt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is greater than another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.gt(value: UByte): SortKeyExpr = gt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is greater than another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.gt(value: UInt): SortKeyExpr = gt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is greater than another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.gt(value: ULong): SortKeyExpr = gt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is greater than another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.gt(value: UShort): SortKeyExpr = gt(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is greater than or equal to another expression
     * @param expr The other expression in the comparison
     */
    public infix fun SortKey.gte(expr: LiteralExpr): SortKeyExpr

    /**
     * Creates an inequality expression for verifying the sort key is greater than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.gte(value: ByteArray): SortKeyExpr = gte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is greater than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.gte(value: Number): SortKeyExpr = gte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is greater than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.gte(value: String): SortKeyExpr = gte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is greater than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.gte(value: UByte): SortKeyExpr = gte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is greater than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.gte(value: UInt): SortKeyExpr = gte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is greater than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.gte(value: ULong): SortKeyExpr = gte(LiteralExpr(value))

    /**
     * Creates an inequality expression for verifying the sort key is greater than or equal to another expression
     * @param value The other value in the comparison
     */
    public infix fun SortKey.gte(value: UShort): SortKeyExpr = gte(LiteralExpr(value))

    /**
     * Creates a range expression for verifying the sort key is between two other expressions
     * @param min The lower bound expression
     * @param max The upper bound expression (inclusive)
     */
    public fun SortKey.isBetween(min: LiteralExpr, max: LiteralExpr): SortKeyExpr

    /**
     * Creates a range expression for verifying the sort key is between two other expressions
     * @param min The lower bound value
     * @param max The upper bound value (inclusive)
     */
    public fun SortKey.isBetween(min: ByteArray, max: ByteArray): SortKeyExpr =
        isBetween(LiteralExpr(min), LiteralExpr(max))

    /**
     * Creates a range expression for verifying the sort key is in the given range
     * @param range The range to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInRangeNumber")
    public infix fun <N> SortKey.isIn(range: ClosedRange<N>): SortKeyExpr where N : Number, N : Comparable<N> =
        isBetween(LiteralExpr(range.start), LiteralExpr(range.endInclusive))

    /**
     * Creates a range expression for verifying the sort key is in the given range
     * @param range The range to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInRangeString")
    public infix fun SortKey.isIn(range: ClosedRange<String>): SortKeyExpr =
        isBetween(LiteralExpr(range.start), LiteralExpr(range.endInclusive))

    /**
     * Creates a range expression for verifying the sort key is in the given range
     * @param range The range to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInRangeUByte")
    public infix fun SortKey.isIn(range: ClosedRange<UByte>): SortKeyExpr =
        isBetween(LiteralExpr(range.start), LiteralExpr(range.endInclusive))

    /**
     * Creates a range expression for verifying the sort key is in the given range
     * @param range The range to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInRangeUInt")
    public infix fun SortKey.isIn(range: ClosedRange<UInt>): SortKeyExpr =
        isBetween(LiteralExpr(range.start), LiteralExpr(range.endInclusive))

    /**
     * Creates a range expression for verifying the sort key is in the given range
     * @param range The range to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInRangeULong")
    public infix fun SortKey.isIn(range: ClosedRange<ULong>): SortKeyExpr =
        isBetween(LiteralExpr(range.start), LiteralExpr(range.endInclusive))

    /**
     * Creates a range expression for verifying the sort key is in the given range
     * @param range The range to check
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("isInRangeUShort")
    public infix fun SortKey.isIn(range: ClosedRange<UShort>): SortKeyExpr =
        isBetween(LiteralExpr(range.start), LiteralExpr(range.endInclusive))

    /**
     * Creates an expression for verifying the sort key starts with the given expression
     * @param expr The expression to test for at the beginning of this attribute
     */
    public infix fun SortKey.startsWith(expr: LiteralExpr): SortKeyExpr

    /**
     * Creates an expression for verifying the sort key starts with the given expression
     * @param value The value to test for at the beginning of this attribute
     */
    public infix fun SortKey.startsWith(value: ByteArray): SortKeyExpr = startsWith(LiteralExpr(value))

    /**
     * Creates an expression for verifying the sort key starts with the given expression
     * @param value The value to test for at the beginning of this attribute
     */
    public infix fun SortKey.startsWith(value: String): SortKeyExpr = startsWith(LiteralExpr(value))
}
