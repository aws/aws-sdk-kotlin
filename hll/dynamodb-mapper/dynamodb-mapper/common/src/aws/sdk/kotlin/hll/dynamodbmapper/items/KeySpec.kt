/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Defines a specification for a single key attribute
 * @param K The type of the key property, either [kotlin.String], [kotlin.Number], or [kotlin.ByteArray]
 */
public sealed interface KeySpec<in K> {
    /**
     * A [KeySpec] for a [kotlin.ByteArray]-typed field
     */
    public interface ByteArray : KeySpec<kotlin.ByteArray>

    /**
     * A [KeySpec] for a [kotlin.Number]-typed field
     */
    public interface Number : KeySpec<kotlin.Number>

    /**
     * A [KeySpec] for a [kotlin.String]-typed field
     */
    public interface String : KeySpec<kotlin.String>

    public companion object {
        /**
         * Creates a new [ByteArray] key specification
         * @param name The name of the key attribute
         */
        public fun ByteArray(name: kotlin.String): KeySpec.ByteArray = ByteArrayImpl(name)

        /**
         * Creates a new [Number] key specification
         * @param name The name of the key attribute
         */
        public fun Number(name: kotlin.String): KeySpec.Number = NumberImpl(name)

        /**
         * Creates a new [String] key specification
         * @param name The name of the key attribute
         */
        public fun String(name: kotlin.String): KeySpec.String = StringImpl(name)
    }

    /**
     * The name of the key attribute
     */
    public val name: kotlin.String

    /**
     * Given a value for this key attribute, convert into a field
     * @param value The value to use for the key attribute
     */
    public fun toField(value: K): Pair<kotlin.String, AttributeValue>
}

private data class ByteArrayImpl(override val name: String) : KeySpec.ByteArray {
    override fun toField(value: ByteArray) = name to AttributeValue.B(value)
}

private data class NumberImpl(override val name: String) : KeySpec.Number {
    override fun toField(value: Number) = name to AttributeValue.N(value.toString())
}

private data class StringImpl(override val name: String) : KeySpec.String {
    override fun toField(value: String) = name to AttributeValue.S(value)
}
