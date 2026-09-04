/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.AttributeDescriptorImpl
import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter

/**
 * Describes a single item attribute and how it is converted from an object of type [T] and to a build object of type
 * [B].
 * @param A The type of value extracted by [getter], accepted by [setter], and used by [converter]
 * @param T The type of object from which values are extracted
 * @param B The type of builder object in which values are mutated
 */
public interface AttributeDescriptor<A, T, B> {
    /**
     * The name of the attribute
     */
    public val name: String

    /**
     * A function which extracts a value of type [A] from an object of type [T]
     */
    public val getter: (T) -> A

    /**
     * A function which operates on a builder of type [B] and mutates a value of type [A]
     */
    public val setter: B.(A) -> Unit

    /**
     * A [ValueConverter] which defines how an object value is converted to an attribute value and vice versa
     */
    public val converter: ValueConverter<A>
}

/**
 * Instantiates a new [AttributeDescriptor]
 * @param A The type of value extracted by [getter], accepted by [setter], and used by [converter]
 * @param T The type of object from which values are extracted
 * @param B The type of builder object in which values are mutated
 * @param name The name of the attribute
 * @param getter A function which extracts a value of type [A] from an object of type [T]
 * @param setter A function which operates on a builder of type [B] and mutates a value of type [A]
 * @param converter A [ValueConverter] which defines how an object value is converted to an attribute value and vice
 * versa
 */
public fun <A, T, B> AttributeDescriptor(
    name: String,
    getter: (T) -> A,
    setter: B.(A) -> Unit,
    converter: ValueConverter<A>,
): AttributeDescriptor<A, T, B> = AttributeDescriptorImpl(name, getter, setter, converter)
