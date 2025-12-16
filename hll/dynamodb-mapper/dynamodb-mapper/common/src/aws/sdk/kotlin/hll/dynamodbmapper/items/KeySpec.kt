/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec.Companion.byteArray
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec.Companion.number
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec.Companion.string
import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.KeyAttrSpecImpl
import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.KeySpec1Impl
import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ScalarAttributeType

/**
 * Defines the specification for a partition or sort key, including the names and types of its attributes. Key
 * specifications are an important component of item schemas since they describe how to interact with each key attribute
 * and provide vital typing information for features which interact with DynamoDB operations like `Query` and `Scan`.
 *
 * A [KeySpec] consisting of a single attribute may be created manually by invoking one the companion object methods
 * [byteArray], [number], or [string]. A [KeySpec] consisting of more attributes may be created by invoking one of the
 * extension methods [thenByteArray], [thenNumber], or [thenString]. Key specifications may consist of up to four
 * attributes.
 *
 * **Important**: The order of attributes within a [KeySpec] is significant and must reflect the order of the attributes
 * defined in the table or index key within DynamoDB.
 *
 * ## Examples
 *
 * To create a key specification for a single attribute:
 *
 * ```kotlin
 * val spec = KeySpec.number<Int>("companyId") // returns KeySpec.Key1<Int>
 * ```
 *
 * To create a key specification for multiple attributes:
 *
 * ```kotlin
 * val spec = KeySpec
 *     .number<Int>("companyId") // returns KeySpec.Key1<Int>
 *     .thenString("department") // returns KeySpec.Key2<Int, String>
 *     .thenNumber<_, Long>("timestamp") // returns KeySpec.Key3<Int, String, Long>
 * ```
 *
 * @param K The type of the key, either [KeyType] or one of its specific derivations
 */
public sealed interface KeySpec<in K : KeyType> {
    /**
     * Given a value for this key, convert into an [Item] map of keys and values
     * @param value The value to use for the key attribute
     */
    public fun toFields(value: K): Item

    /**
     * Defines the specification for a key consisting of a single attribute
     * @param K1 The type of the single key attribute, either [String], [Number], or [ByteArray]
     */
    public interface Key1<K1> : KeySpec<KeyType.Key1<K1>> {
        /**
         * Gets the specification for the key attribute
         */
        public val attr1: KeyAttrSpec<K1>
    }

    /**
     * Defines the specification for a key consisting of two attributes
     * @param K1 The type of the first key attribute, either [String], [Number], or [ByteArray]
     * @param K2 The type of the second key attribute, either [String], [Number], or [ByteArray]
     */
    public interface Key2<K1, K2> : KeySpec<KeyType.Key2<K1, K2>> {
        /**
         * Gets the specification for the first key attribute
         */
        public val attr1: KeyAttrSpec<K1>

        /**
         * Gets the specification for the second key attribute
         */
        public val attr2: KeyAttrSpec<K2>
    }

    /**
     * Defines the specification for a key consisting of three attributes
     * @param K1 The type of the first key attribute, either [String], [Number], or [ByteArray]
     * @param K2 The type of the second key attribute, either [String], [Number], or [ByteArray]
     * @param K3 The type of the third key attribute, either [String], [Number], or [ByteArray]
     */
    public interface Key3<K1, K2, K3> : KeySpec<KeyType.Key3<K1, K2, K3>> {
        /**
         * Gets the specification for the first key attribute
         */
        public val attr1: KeyAttrSpec<K1>

        /**
         * Gets the specification for the second key attribute
         */
        public val attr2: KeyAttrSpec<K2>

        /**
         * Gets the specification for the third key attribute
         */
        public val attr3: KeyAttrSpec<K3>
    }

    /**
     * Defines the specification for a key consisting of four attributes
     * @param K1 The type of the first key attribute, either [String], [Number], or [ByteArray]
     * @param K2 The type of the second key attribute, either [String], [Number], or [ByteArray]
     * @param K3 The type of the third key attribute, either [String], [Number], or [ByteArray]
     * @param K3 The type of the fourth key attribute, either [String], [Number], or [ByteArray]
     */
    public interface Key4<K1, K2, K3, K4> : KeySpec<KeyType.Key4<K1, K2, K3, K4>> {
        /**
         * Gets the specification for the first key attribute
         */
        public val attr1: KeyAttrSpec<K1>

        /**
         * Gets the specification for the second key attribute
         */
        public val attr2: KeyAttrSpec<K2>

        /**
         * Gets the specification for the third key attribute
         */
        public val attr3: KeyAttrSpec<K3>

        /**
         * Gets the specification for the fourth key attribute
         */
        public val attr4: KeyAttrSpec<K4>
    }

    public companion object {
        /**
         * Instantiates a new [KeySpec] for a single attribute. Additional attributes may be added with the extension
         * methods [thenByteArray], [thenNumber], and [thenString].
         * @param name The name of the attribute
         */
        public fun byteArray(name: String): Key1<ByteArray> = KeySpec1Impl(KeyAttrSpec.byteArray(name))

        /**
         * Instantiates a new [KeySpec] for a single attribute. Additional attributes may be added with the extension
         * methods [thenByteArray], [thenNumber], and [thenString].
         * @param N The type of [Number] used for this attribute (e.g., [Int])
         * @param name The name of the attribute
         */
        public fun <N : Number> number(name: String): Key1<N> = KeySpec1Impl(KeyAttrSpec.number(name))

        /**
         * Instantiates a new [KeySpec] for a single attribute. Additional attributes may be added with the extension
         * methods [thenByteArray], [thenNumber], and [thenString].
         * @param name The name of the attribute
         */
        public fun string(name: String): Key1<String> = KeySpec1Impl(KeyAttrSpec.string(name))
    }
}

/**
 * Defines the specification for a single key attribute, including its name and type
 * @param K The Kotlin type of the attribute
 */
public interface KeyAttrSpec<K> {
    /**
     * The DynamoDB name of the key attribute
     */
    public val name: String

    /**
     * The DynamoDB type of the attribute
     */
    public val type: ScalarAttributeType

    /**
     * Creates a DynamoDB key-value pair for an attribute as a `Pair<String, AttributeValue>`, suitable for use in an
     * [Item] instance.
     */
    public fun toField(value: K): Pair<String, AttributeValue>

    public companion object {
        /**
         * Instantiates a new binary [KeyAttrSpec] with the given name
         * @param name The name of the attribute
         */
        public fun byteArray(name: String): KeyAttrSpec<ByteArray> =
            KeyAttrSpecImpl(name, ScalarAttributeType.B, AttributeValue::B)

        /**
         * Instantiates a new numeric [KeyAttrSpec] with the given name
         * @param N The type of [Number] of this attribute (e.g., [Int])
         * @param name The name of the attribute
         */
        public fun <N : Number> number(name: String): KeyAttrSpec<N> =
            KeyAttrSpecImpl(name, ScalarAttributeType.N) { AttributeValue.N(it.toString()) }

        /**
         * Instantiates a new string [KeyAttrSpec] with the given name
         * @param name The name of the attribute
         */
        public fun string(name: String): KeyAttrSpec<String> =
            KeyAttrSpecImpl(name, ScalarAttributeType.S, AttributeValue::S)
    }
}
