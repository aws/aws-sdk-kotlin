/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.KeyType1Impl

/**
 * A key value for a table or index.
 *
 * Keys in DynamoDB may consist of 1, 2, 3, or 4 attributes. This type exists to provide a uniform interface over keys
 * of any cardinality to use in `GetItem`, `Query`, and other operations which operate on keys. Where possible,
 * convenience methods are added to simplify the most common pattern of a single key attribute.
 *
 * A [KeyType] consisting of a single attribute value may be created with the [Key] factory function. A [KeyType]
 * consisting of additional attribute values may be created by invoking the one of the [invoke] extension function
 * overloads.
 *
 * **Important**: The order of attribute values within a [KeyType] is significant and must reflect the order of the
 * attributes defined in the table or index key within DynamoDB as well as related [KeySpec] instances for the same
 * table/index.
 *
 * ## Examples
 *
 * To create a key value for a single attribute:
 *
 * ```kotlin
 * val keyValue = Key(42) // returns KeyType.Key1<Int>
 * ```
 *
 * To create a key specification for multiple attributes:
 *
 * ```kotlin
 * val keyValue = Key(42)("ABCD")(1000L) // returns KeyType.Key3<Int, String, Long>
 * ```
 */
public sealed interface KeyType {
    /**
     * A key value consisting of a single attribute
     * @param K1 The type of the single attribute
     */
    public interface Key1<K1> : KeyType {
        /**
         * The single attribute value
         */
        public val value1: K1

        /**
         * Destructuring function that returns the single attribute value
         */
        public operator fun component1(): K1 = value1
    }

    /**
     * A key value consisting of two attributes
     * @param K1 The type of the first attribute
     * @param K2 The type of the second attribute
     */
    public interface Key2<K1, K2> : KeyType {
        /**
         * The first attribute value
         */
        public val value1: K1

        /**
         * The second attribute value
         */
        public val value2: K2

        /**
         * Destructuring function that returns the first attribute value
         */
        public operator fun component1(): K1 = value1

        /**
         * Destructuring function that returns the second attribute value
         */
        public operator fun component2(): K2 = value2
    }

    /**
     * A key value consisting of three attributes
     * @param K1 The type of the first attribute
     * @param K2 The type of the second attribute
     * @param K3 The type of the third attribute
     */
    public interface Key3<K1, K2, K3> : KeyType {
        /**
         * The first attribute value
         */
        public val value1: K1

        /**
         * The second attribute value
         */
        public val value2: K2

        /**
         * The third attribute value
         */
        public val value3: K3

        /**
         * Destructuring function that returns the first attribute value
         */
        public operator fun component1(): K1 = value1

        /**
         * Destructuring function that returns the second attribute value
         */
        public operator fun component2(): K2 = value2

        /**
         * Destructuring function that returns the third attribute value
         */
        public operator fun component3(): K3 = value3
    }

    /**
     * A key value consisting of four attributes
     * @param K1 The type of the first attribute
     * @param K2 The type of the second attribute
     * @param K3 The type of the third attribute
     * @param K4 The type of the four attribute
     */
    public interface Key4<K1, K2, K3, K4> : KeyType {
        /**
         * The first attribute value
         */
        public val value1: K1

        /**
         * The second attribute value
         */
        public val value2: K2

        /**
         * The third attribute value
         */
        public val value3: K3

        /**
         * The fourth attribute value
         */
        public val value4: K4

        /**
         * Destructuring function that returns the first attribute value
         */
        public operator fun component1(): K1 = value1

        /**
         * Destructuring function that returns the second attribute value
         */
        public operator fun component2(): K2 = value2

        /**
         * Destructuring function that returns the third attribute value
         */
        public operator fun component3(): K3 = value3

        /**
         * Destructuring function that returns the fourth attribute value
         */
        public operator fun component4(): K4 = value4
    }
}

/**
 * Instantiates a new [KeyType] consisting of a single value. Additional values may be added with one of the [invoke]
 * extension function overloads.
 * @param value1 The value of the single attribute
 */
@Suppress("FunctionName")
public fun Key(value1: Byte): KeyType.Key1<Byte> = KeyType1Impl(value1)

/**
 * Instantiates a new [KeyType] consisting of a single value. Additional values may be added with one of the [invoke]
 * extension function overloads.
 * @param value1 The value of the single attribute
 */
@Suppress("FunctionName")
public fun Key(value1: ByteArray): KeyType.Key1<ByteArray> = KeyType1Impl(value1)

/**
 * Instantiates a new [KeyType] consisting of a single value. Additional values may be added with one of the [invoke]
 * extension function overloads.
 * @param value1 The value of the single attribute
 */
@Suppress("FunctionName")
public fun Key(value1: Int): KeyType.Key1<Int> = KeyType1Impl(value1)

/**
 * Instantiates a new [KeyType] consisting of a single value. Additional values may be added with one of the [invoke]
 * extension function overloads.
 * @param value1 The value of the single attribute
 */
@Suppress("FunctionName")
public fun Key(value1: Long): KeyType.Key1<Long> = KeyType1Impl(value1)

/**
 * Instantiates a new [KeyType] consisting of a single value. Additional values may be added with one of the [invoke]
 * extension function overloads.
 * @param value1 The value of the single attribute
 */
@Suppress("FunctionName")
public fun Key(value1: Short): KeyType.Key1<Short> = KeyType1Impl(value1)

/**
 * Instantiates a new [KeyType] consisting of a single value. Additional values may be added with one of the [invoke]
 * extension function overloads.
 * @param value1 The value of the single attribute
 */
@Suppress("FunctionName")
public fun Key(value1: String): KeyType.Key1<String> = KeyType1Impl(value1)
