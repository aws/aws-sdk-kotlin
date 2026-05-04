/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema

/**
 * A DSL interface providing support for creating "low-level"
 * [DynamoDB update expressions](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html).
 * The methods and properties in this interface specify how a matching item should be modified in an `UpdateItem`
 * operation.
 *
 * For example:
 *
 * ```kotlin
 * update {
 *     set {
 *         attr["foo"] = 42
 *     }
 * }
 * ```
 *
 * This example creates an expression which will set the attribute `foo` to the value `42`.
 *
 * # (Non-)Relationship to schema
 *
 * The expressions formed by [UpdateDsl] are referred to as "low-level" update expressions. This is because they are
 * not restricted by or adherent to any defined [ItemSchema]. Instead, they are a DSL convenience layer over
 * [literal DynamoDB expression strings and expression attribute value maps](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.html).
 * As such they provide **minimal type correctness** and may allow you to form expressions which are invalid given the
 * shape of your data, such as attributes which don't exist, comparisons with mismatched data types, etc.
 *
 * # Attributes
 *
 * Every update expression contains at least one attribute. Attributes are referenced by attribute paths, analogous to
 * [document paths in DynamoDB](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.Attributes.html#Expressions.Attributes.NestedElements.DocumentPathExamples).
 * Attribute paths consist of one or more elements, which are either names (e.g., of a top-level attribute or a nested
 * key in a map attribute) or indices (i.e., into a list). The first (and often only) element of an attribute path is a
 * name.
 *
 * ## Getting a top-level attribute
 *
 * All attribute paths start with a top-level attribute expression, created by the `attr` function:
 *
 * ```kotlin
 * attr["foo"] // References the top-level attribute "foo"
 * ```
 *
 * Note, the attribute `foo` may not exist for a given item or for an entire table.
 *
 * ## Nesting
 *
 * Sometimes values are nested inside other attributes like lists and maps. Update expressions can operate on those
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
 * attr["bar"]["baz"][0]
 * ```
 *
 * That is, in the top-level attribute `bar`, in the value keyed by `baz`, the element at index `0`.
 *
 * # Clauses
 *
 * An update expression consists of _at least one_ of the following clauses:
 * * [Set]: modify or add item attributes
 * * [Remove]: delete item attributes
 * * [Add]: update numbers and sets
 * * [Delete]: remove elements from sets
 *
 * Each of these clauses may contain one or more updates (for instance, setting multiple attribute values at once,
 * deleting multiple elements from a set, etc.).
 *
 * These clauses may be specified in any order. If a given clause is repeated multiple times within an `update` block,
 * the last one overrides the previous ones.
 *
 * See the documentation for the [Set], [Remove], [Add], and [Delete] interfaces for more details about each clause.
 */
public interface UpdateDsl {
    /**
     * Builds a `SET` clause for this update expression, which may be used to modify or add item attributes. See [Set]
     * for more details and examples.
     */
    public fun set(block: Set.() -> Unit)

    /**
     * Builds a `REMOVE` clause for this update expression, which may be used to delete item attributes. See [Remove]
     * for more details and examples.
     */
    public fun remove(block: Remove.() -> Unit)

    /**
     * Builds an `ADD` clause for this update expression, which may be used to update numbers and sets. See [Add]
     * for more details and examples.
     */
    public fun add(block: Add.() -> Unit)

    /**
     * Builds a `DELETE` clause for this update expression, which may be used to remove elements from sets. See [Delete]
     * for more details and examples.
     */
    public fun delete(block: Delete.() -> Unit)

    /**
     * The `Set` clause may be used to add new attributes to an item, update existing attributes, add/update list
     * elements, add/update map attributes, and add or subtract numerical values. See
     * [the low-level DynamoDB documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET)
     * for more details on the underlying features and capabilities.
     *
     * # Adding or updating attributes and elements
     *
     * The simplest form of update involves setting a specific attribute/element to a specific value using the `[]`
     * operator or the [set] method. The value passed may be a literal value such as `42` or an attribute reference such
     * as `attr["bar"]`.
     *
     * For example:
     *
     * ```kotlin
     * set {
     *     attr["foo"] = "hello"                // Set the attribute `foo` to the value `"hello"`
     *     attr["bar"]["baz"] = listOf(1, 2, 3) // Set `bar`'s entry `baz` to the value `[1, 2, 3]`
     *     attr["qux"][0] = attr["nox"]         // Set `qux`'s first element to the value of the attribute `nox`
     * }
     * ```
     *
     * If the targeted attribute/element already exists, it will be replaced. If it does not exist, it will be added.
     *
     * # Adding and subtracting numerical values
     *
     * `Set` updates may derive their desired value by adding to or subtracting from an existing numerical attribute
     * using the `+` operator (or [plus] method) and the `-` operator (or the [minus] method):
     *
     * ```kotlin
     * set {
     *     attr["foo"] = attr["bar"] + 100 // Set the attribute `foo` to the value of attribute `bar` plus `100`
     *     attr["baz"] = attr["qux"] - 10  // Set the attribute `baz` to the value of attribute `qux` minus `10`
     *     attr["nox"] = attr["nox"] + 1   // Increment the numerical value of attribute `nox` by `1`
     * }
     * ```
     *
     * The last update in the preceding example increments the value of `nox` by setting `nox` to its current value plus
     * `1`. This expression can be simplified by using the `+=` operator (or [plusAssign] method) or `-=` operator (or
     * [minusAssign] method):
     *
     * ```kotlin
     * set {
     *     attr["foo"] += 42 // Increments the attribute `foo` by `42`
     *     attr["bar"] -= 84 // Decrements the attribute `bar` by `84`
     * }
     * ```
     *
     * # Conditional updates
     *
     * Update expressions support setting an attribute value _only_ if it does not already exist to prevent overwriting
     * data by using the [orElse] method:
     *
     * ```kotlin
     * set {
     *     attr["foo"] = attr["foo"] orElse "hello" // Sets the attribute `foo` to `"hello"` unless `foo` already exists
     * }
     * ```
     *
     * # Appending lists
     *
     * Update expressions may form a value by appending a list value to another list value using the [appending] method:
     *
     * ```kotlin
     * set {
     *     attr["foo"] = attr["foo"] appending listOf(1, 2, 3) // Appends the value [1, 2, 3] to the value of `foo`
     *     attr["bar"] = listOf(4, 5, 6) appending attr["bar"] // Prepends the value [4, 5, 6] to the value of `bar`
     * }
     * ```
     *
     * @see UpdateDsl
     */
    public interface Set {
        // ATTRIBUTES

        /**
         * Accesses an attribute path reference from a top-level attribute name in this update expression (e.g.,
         * `attr["foo"]` references the "foo" attribute of items being updated)
         */
        public val attr: Attr

        // SET VALUE

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        public operator fun Attr.set(key: String, value: Expression)

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        public operator fun Attr.set(key: String, value: Boolean?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        public operator fun Attr.set(key: String, value: ByteArray?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        public operator fun Attr.set(key: String, value: List<Any?>?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        public operator fun Attr.set(key: String, value: Map<String, Any?>?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        public operator fun Attr.set(key: String, value: Nothing?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        public operator fun Attr.set(key: String, value: Number?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attrSetSetByteArray")
        public operator fun Attr.set(
            key: String,
            value: kotlin.collections.Set<ByteArray>?,
        ): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attrSetSetNumber")
        public operator fun <N : Number> Attr.set(
            key: String,
            value: kotlin.collections.Set<N>?,
        ): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attrSetSetString")
        public operator fun Attr.set(
            key: String,
            value: kotlin.collections.Set<String>?,
        ): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attrSetSetUByte")
        public operator fun Attr.set(
            key: String,
            value: kotlin.collections.Set<UByte>?,
        ): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attrSetSetUInt")
        public operator fun Attr.set(
            key: String,
            value: kotlin.collections.Set<UInt>?,
        ): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attrSetSetULong")
        public operator fun Attr.set(
            key: String,
            value: kotlin.collections.Set<ULong>?,
        ): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attrSetSetUShort")
        public operator fun Attr.set(
            key: String,
            value: kotlin.collections.Set<UShort>?,
        ): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        public operator fun Attr.set(key: String, value: String?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        public operator fun Attr.set(key: String, value: UByte?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        public operator fun Attr.set(key: String, value: UInt?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        public operator fun Attr.set(key: String, value: ULong?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute [key] to the given [value]
         * @param key The name of the top-level attribute to set
         * @param value The desired value of the given attribute
         */
        public operator fun Attr.set(key: String, value: UShort?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        public operator fun AttributePath.set(index: Int, value: Expression)

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        public operator fun AttributePath.set(index: Int, value: Boolean?): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        public operator fun AttributePath.set(index: Int, value: ByteArray?): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        public operator fun AttributePath.set(index: Int, value: List<Any?>?): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        public operator fun AttributePath.set(
            index: Int,
            value: Map<String, Any?>?,
        ): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        public operator fun AttributePath.set(index: Int, value: Nothing?): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        public operator fun AttributePath.set(index: Int, value: Number?): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathSetSetByteArray")
        public operator fun AttributePath.set(
            index: Int,
            value: kotlin.collections.Set<ByteArray>?,
        ): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathSetSetNumber")
        public operator fun <N : Number> AttributePath.set(
            index: Int,
            value: kotlin.collections.Set<N>?,
        ): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathSetSetString")
        public operator fun AttributePath.set(
            index: Int,
            value: kotlin.collections.Set<String>?,
        ): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathSetSetUByte")
        public operator fun AttributePath.set(
            index: Int,
            value: kotlin.collections.Set<UByte>?,
        ): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathSetSetUInt")
        public operator fun AttributePath.set(
            index: Int,
            value: kotlin.collections.Set<UInt>?,
        ): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathSetSetULong")
        public operator fun AttributePath.set(
            index: Int,
            value: kotlin.collections.Set<ULong>?,
        ): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathSetSetUShort")
        public operator fun AttributePath.set(
            index: Int,
            value: kotlin.collections.Set<UShort>?,
        ): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        public operator fun AttributePath.set(index: Int, value: String?): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        public operator fun AttributePath.set(index: Int, value: UByte?): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        public operator fun AttributePath.set(index: Int, value: UInt?): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        public operator fun AttributePath.set(index: Int, value: ULong?): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the elements at the given [index] to the given [value]
         * @param index The index of the element to update
         * @param value The desired value of the given element
         */
        public operator fun AttributePath.set(index: Int, value: UShort?): Unit = set(index, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        public operator fun AttributePath.set(key: String, value: Expression)

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        public operator fun AttributePath.set(key: String, value: Boolean?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        public operator fun AttributePath.set(key: String, value: ByteArray?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        public operator fun AttributePath.set(key: String, value: List<Any?>?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        public operator fun AttributePath.set(
            key: String,
            value: Map<String, Any?>?,
        ): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        public operator fun AttributePath.set(key: String, value: Nothing?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        public operator fun AttributePath.set(key: String, value: Number?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathSetSetByteArray")
        public operator fun AttributePath.set(
            key: String,
            value: kotlin.collections.Set<ByteArray>?,
        ): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathSetSetNumber")
        public operator fun <N : Number> AttributePath.set(
            key: String,
            value: kotlin.collections.Set<N>?,
        ): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathSetSetString")
        public operator fun AttributePath.set(
            key: String,
            value: kotlin.collections.Set<String>?,
        ): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathSetSetUByte")
        public operator fun AttributePath.set(
            key: String,
            value: kotlin.collections.Set<UByte>?,
        ): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathSetSetUInt")
        public operator fun AttributePath.set(
            key: String,
            value: kotlin.collections.Set<UInt>?,
        ): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathSetSetULong")
        public operator fun AttributePath.set(
            key: String,
            value: kotlin.collections.Set<ULong>?,
        ): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathSetSetUShort")
        public operator fun AttributePath.set(
            key: String,
            value: kotlin.collections.Set<UShort>?,
        ): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        public operator fun AttributePath.set(key: String, value: String?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        public operator fun AttributePath.set(key: String, value: UByte?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        public operator fun AttributePath.set(key: String, value: UInt?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        public operator fun AttributePath.set(key: String, value: ULong?): Unit = set(key, LiteralExpr(value))

        /**
         * Adds an update instruction for setting the given attribute/entry [key] to the given [value]
         * @param key The name of the attribute or entry to set
         * @param value The desired value of the given attribute
         */
        public operator fun AttributePath.set(key: String, value: UShort?): Unit = set(key, LiteralExpr(value))

        // ADDITION/SUBTRACTION

        /**
         * Creates an arithmetic expression for adding the given [value] to this expression
         * @param value The value to add
         */
        public operator fun Expression.plus(value: Expression): Expression

        /**
         * Creates an arithmetic expression for adding the given [value] to this expression
         * @param value The value to add
         */
        public operator fun Expression.plus(value: Number): Expression = plus(LiteralExpr(value))

        /**
         * Creates an arithmetic expression for adding the given [value] to this expression
         * @param value The value to add
         */
        public operator fun Expression.plus(value: UByte): Expression = plus(LiteralExpr(value))

        /**
         * Creates an arithmetic expression for adding the given [value] to this expression
         * @param value The value to add
         */
        public operator fun Expression.plus(value: UInt): Expression = plus(LiteralExpr(value))

        /**
         * Creates an arithmetic expression for adding the given [value] to this expression
         * @param value The value to add
         */
        public operator fun Expression.plus(value: ULong): Expression = plus(LiteralExpr(value))

        /**
         * Creates an arithmetic expression for adding the given [value] to this expression
         * @param value The value to add
         */
        public operator fun Expression.plus(value: UShort): Expression = plus(LiteralExpr(value))

        /**
         * Creates an arithmetic expression for subtracting the given [value] from this expression
         * @param value The value to subtract
         */
        public operator fun Expression.minus(value: Expression): Expression

        /**
         * Creates an arithmetic expression for subtracting the given [value] from this expression
         * @param value The value to subtract
         */
        public operator fun Expression.minus(value: Number): Expression = minus(LiteralExpr(value))

        /**
         * Creates an arithmetic expression for subtracting the given [value] from this expression
         * @param value The value to subtract
         */
        public operator fun Expression.minus(value: UByte): Expression = minus(LiteralExpr(value))

        /**
         * Creates an arithmetic expression for subtracting the given [value] from this expression
         * @param value The value to subtract
         */
        public operator fun Expression.minus(value: UInt): Expression = minus(LiteralExpr(value))

        /**
         * Creates an arithmetic expression for subtracting the given [value] from this expression
         * @param value The value to subtract
         */
        public operator fun Expression.minus(value: ULong): Expression = minus(LiteralExpr(value))

        /**
         * Creates an arithmetic expression for subtracting the given [value] from this expression
         * @param value The value to subtract
         */
        public operator fun Expression.minus(value: UShort): Expression = minus(LiteralExpr(value))

        // FUNCTIONS

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        public infix fun AttributePath.orElse(value: Expression): Expression

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        public infix fun AttributePath.orElse(value: Boolean?): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        public infix fun AttributePath.orElse(value: ByteArray?): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        public infix fun AttributePath.orElse(value: List<Any?>?): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        public infix fun AttributePath.orElse(value: Map<String, Any?>?): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        public infix fun AttributePath.orElse(value: Nothing?): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        public infix fun AttributePath.orElse(value: Number?): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathOrElseSetByteArray")
        public infix fun AttributePath.orElse(
            value: kotlin.collections.Set<ByteArray>?,
        ): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathOrElseSetNumber")
        public infix fun <N : Number> AttributePath.orElse(
            value: kotlin.collections.Set<N>?,
        ): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathOrElseSetString")
        public infix fun AttributePath.orElse(
            value: kotlin.collections.Set<String>?,
        ): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathOrElseSetUByte")
        public infix fun AttributePath.orElse(
            value: kotlin.collections.Set<UByte>?,
        ): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathOrElseSetUInt")
        public infix fun AttributePath.orElse(
            value: kotlin.collections.Set<UInt>?,
        ): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathOrElseSetULong")
        public infix fun AttributePath.orElse(
            value: kotlin.collections.Set<ULong>?,
        ): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("attributePathOrElseSetUShort")
        public infix fun AttributePath.orElse(
            value: kotlin.collections.Set<UShort>?,
        ): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        public infix fun AttributePath.orElse(
            value: String?,
        ): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        public infix fun AttributePath.orElse(
            value: UByte?,
        ): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        public infix fun AttributePath.orElse(
            value: UInt?,
        ): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        public infix fun AttributePath.orElse(
            value: ULong?,
        ): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the value of this attribute path _unless_ a value does not
         * exist, in which case the given [value] is used. This corresponds to the
         * [low-level DynamoDB function `if_not_exists`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.PreventingAttributeOverwrites).
         * @param value The value to use if this attribute does not exist
         */
        public infix fun AttributePath.orElse(
            value: UShort?,
        ): Expression = orElse(LiteralExpr(value))

        /**
         * Creates a function expression which represents the list value of this attribute path concatenated with the
         * given list [value]. This corresponds to the
         * [low-level DynamoDB function `list_append`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.UpdatingListElements).
         * @param value The list value to append to this list value
         */
        public infix fun AttributePath.appending(value: Expression): Expression

        /**
         * Creates a function expression which represents the list value of this attribute path concatenated with the
         * given list [value]. This corresponds to the
         * [low-level DynamoDB function `list_append`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.UpdatingListElements).
         * @param value The list value to append to this list value
         */
        public infix fun AttributePath.appending(value: List<Any?>): Expression = appending(LiteralExpr(value))

        /**
         * Creates a function expression which represents the given list [value] with the value of this attribute path.
         * This corresponds to the
         * [low-level DynamoDB function `list_append`](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.SET.UpdatingListElements).
         * @param value The list-valued attribute to append to this list
         */
        public infix fun List<Any?>.appending(value: Expression): Expression
    }

    /**
     * The `Remove` clause may be used to remove attributes from an item or map and to remove elements from a list. See
     * [the low-level DynamoDB documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.REMOVE)
     * for more details on the underlying features and capabilities.
     *
     * Attributes and elements may be removed by using the unary `-` operator (or the [unaryMinus] method):
     *
     * ```kotlin
     * remove {
     *     -attr["foo"]        // Removes the attribute `foo`
     *     -attr["baz"]["bar"] // Removes `baz`'s `bar` entry
     *     -attr["qux"][3]     // Removes `qux`'s element at index `3`
     * }
     * ```
     */
    public interface Remove {
        /**
         * Accesses an attribute path reference from a top-level attribute name in this update expression (e.g.,
         * `attr["foo"]` references the "foo" attribute of items being updated)
         */
        public val attr: Attr

        /**
         * Adds an update instruction for removing this attribute, entry, or element
         */
        public operator fun AttributePath.unaryMinus()
    }

    /**
     * The `Add` clause may be used to update numbers and add elements to sets. See
     * [the low-level DynamoDB documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.ADD)
     * for more details on the underlying features and capabilities.
     *
     * Number values and set elements may be added by using the `+=` operator (or the [plusAssign] method):
     *
     * ```kotlin
     * add {
     *     attr["foo"] += 15                   // Increments the value of attribute `foo` by `15`
     *     attr["bar"] += -15                  // Decrements the value of attribute `foo` by `15`
     *     attr["baz"] += setOf("a", "b", "c") // Adds the elements `["a", "b", "c"]` to the attribute `baz`
     * }
     * ```
     */
    public interface Add {
        /**
         * Accesses an attribute path reference from a top-level attribute name in this update expression (e.g.,
         * `attr["foo"]` references the "foo" attribute of items being updated)
         */
        public val attr: Attr

        /**
         * Adds an update instruction to update this numerical attribute or to add an element to this set
         * @param value The value to add
         */
        public operator fun AttributePath.plusAssign(value: Expression)

        /**
         * Adds an update instruction to update this numerical attribute or to add an element to this set
         * @param value The value to add
         */
        public operator fun AttributePath.plusAssign(value: Number): Unit = plusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to update this numerical attribute or to add an element to this set
         * @param value The value to add
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("plusAssignSetByteArray")
        public operator fun AttributePath.plusAssign(
            value: kotlin.collections.Set<ByteArray>,
        ): Unit = plusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to update this numerical attribute or to add an element to this set
         * @param value The value to add
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("plusAssignSetNumber")
        public operator fun <N : Number> AttributePath.plusAssign(
            value: kotlin.collections.Set<N>,
        ): Unit = plusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to update this numerical attribute or to add an element to this set
         * @param value The value to add
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("plusAssignSetString")
        public operator fun AttributePath.plusAssign(
            value: kotlin.collections.Set<String>,
        ): Unit = plusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to update this numerical attribute or to add an element to this set
         * @param value The value to add
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("plusAssignSetUByte")
        public operator fun AttributePath.plusAssign(
            value: kotlin.collections.Set<UByte>,
        ): Unit = plusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to update this numerical attribute or to add an element to this set
         * @param value The value to add
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("plusAssignSetUInt")
        public operator fun AttributePath.plusAssign(
            value: kotlin.collections.Set<UInt>,
        ): Unit = plusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to update this numerical attribute or to add an element to this set
         * @param value The value to add
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("plusAssignSetULong")
        public operator fun AttributePath.plusAssign(
            value: kotlin.collections.Set<ULong>,
        ): Unit = plusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to update this numerical attribute or to add an element to this set
         * @param value The value to add
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("plusAssignSetUShort")
        public operator fun AttributePath.plusAssign(
            value: kotlin.collections.Set<UShort>,
        ): Unit = plusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to update this numerical attribute or to add an element to this set
         * @param value The value to add
         */
        public operator fun AttributePath.plusAssign(value: UByte): Unit = plusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to update this numerical attribute or to add an element to this set
         * @param value The value to add
         */
        public operator fun AttributePath.plusAssign(value: UInt): Unit = plusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to update this numerical attribute or to add an element to this set
         * @param value The value to add
         */
        public operator fun AttributePath.plusAssign(value: ULong): Unit = plusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to update this numerical attribute or to add an element to this set
         * @param value The value to add
         */
        public operator fun AttributePath.plusAssign(value: UShort): Unit = plusAssign(LiteralExpr(value))
    }

    /**
     * The `Delete` clause may be used to remove elements from sets. See
     * [the low-level DynamoDB documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.DELETE)
     * for more details on the underlying features and capabilities.
     *
     * Set values may be removed by using the `-=` operator (or the [minusAssign] method):
     *
     * ```kotlin
     * delete {
     *     attr["foo"] -= setOf(42) // Removes the element `42` from attribute `foo`
     * }
     * ```
     */
    public interface Delete {
        /**
         * Accesses an attribute path reference from a top-level attribute name in this update expression (e.g.,
         * `attr["foo"]` references the "foo" attribute of items being updated)
         */
        public val attr: Attr

        /**
         * Adds an update instruction to remove the given [value] from this set
         * @param value The value to remove
         */
        public operator fun AttributePath.minusAssign(value: Expression)

        /**
         * Adds an update instruction to remove the given [value] from this set
         * @param value The value to remove
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("minusAssignSetByteArray")
        public operator fun AttributePath.minusAssign(
            value: kotlin.collections.Set<ByteArray>,
        ): Unit = minusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to remove the given [value] from this set
         * @param value The value to remove
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("minusAssignSetNumber")
        public operator fun <N : Number> AttributePath.minusAssign(
            value: kotlin.collections.Set<N>,
        ): Unit = minusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to remove the given [value] from this set
         * @param value The value to remove
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("minusAssignSetString")
        public operator fun AttributePath.minusAssign(
            value: kotlin.collections.Set<String>,
        ): Unit = minusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to remove the given [value] from this set
         * @param value The value to remove
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("minusAssignSetUByte")
        public operator fun AttributePath.minusAssign(
            value: kotlin.collections.Set<UByte>,
        ): Unit = minusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to remove the given [value] from this set
         * @param value The value to remove
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("minusAssignSetUInt")
        public operator fun AttributePath.minusAssign(
            value: kotlin.collections.Set<UInt>,
        ): Unit = minusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to remove the given [value] from this set
         * @param value The value to remove
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("minusAssignSetULong")
        public operator fun AttributePath.minusAssign(
            value: kotlin.collections.Set<ULong>,
        ): Unit = minusAssign(LiteralExpr(value))

        /**
         * Adds an update instruction to remove the given [value] from this set
         * @param value The value to remove
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("minusAssignSetUShort")
        public operator fun AttributePath.minusAssign(
            value: kotlin.collections.Set<UShort>,
        ): Unit = minusAssign(LiteralExpr(value))
    }
}
